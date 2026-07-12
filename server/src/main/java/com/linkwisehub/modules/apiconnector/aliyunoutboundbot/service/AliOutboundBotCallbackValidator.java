package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.service;

import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.modules.apiconnector.aliyunoutboundbot.config.AliOutboundBotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 阿里智能外呼回调安全校验器。
 */
@Component
@RequiredArgsConstructor
public class AliOutboundBotCallbackValidator {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Pattern SIGNATURE_MIDDLE_PATTERN =
            Pattern.compile(",\\s*\"signature\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\"");
    private static final Pattern SIGNATURE_LEADING_PATTERN =
            Pattern.compile("\"signature\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\"\\s*,\\s*");
    private static final Pattern SIGNATURE_ONLY_PATTERN =
            Pattern.compile("\"signature\"\\s*:\\s*\"(?:\\\\.|[^\"\\\\])*\"");

    private final AliOutboundBotProperties properties;

    /**
     * 按来源 IP、时间戳、签名三个维度校验回调合法性。
     */
    public void validate(JSONObject body, String rawBody, Map<String, String> headers, String sourceIp) {
        if (body == null) {
            throw new IllegalArgumentException("回调报文不能为空");
        }
        validateSourceIp(sourceIp);
        validateTimestamp(headers, body);
        validateSignature(headers, body, rawBody);
    }

    /**
     * 校验来源 IP 白名单；未配置白名单时跳过。
     */
    private void validateSourceIp(String sourceIp) {
        List<String> whitelist = normalizeList(properties.getSourceIpWhitelist());
        if (CollectionUtils.isEmpty(whitelist)) {
            return;
        }
        if (isBlank(sourceIp)) {
            throw new IllegalArgumentException("回调来源IP不能为空");
        }
        if (!whitelist.contains(sourceIp)) {
            throw new IllegalArgumentException("回调来源IP不在白名单内");
        }
    }

    /**
     * 校验回调时间戳，避免重放时间过久的请求。
     */
    private void validateTimestamp(Map<String, String> headers, JSONObject body) {
        String timestamp = getIgnoreCase(headers, "X-Ali-Timestamp");
        if (isBlank(timestamp)) {
            timestamp = body.getString("timestamp");
        }
        if (isBlank(timestamp)) {
            return;
        }
        try {
            long requestMillis = Long.parseLong(timestamp);
            if (String.valueOf(requestMillis).length() == 10) {
                requestMillis = requestMillis * 1000L;
            }
            long diffSeconds = Math.abs(System.currentTimeMillis() - requestMillis) / 1000L;
            Long tolerance = properties.getCallbackTimestampToleranceSeconds();
            if (tolerance != null && tolerance > 0 && diffSeconds > tolerance) {
                throw new IllegalArgumentException("回调时间戳已超出允许窗口");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("回调时间戳格式不正确", e);
        }
    }

    /**
     * 校验 HMAC-SHA256 签名；未配置回调密钥时跳过。
     */
    private void validateSignature(Map<String, String> headers, JSONObject body, String rawBody) {
        if (isBlank(properties.getCallbackSecret())) {
            return;
        }
        String signature = getIgnoreCase(headers, "X-Ali-Signature");
        if (isBlank(signature)) {
            signature = body.getString("signature");
        }
        if (isBlank(signature)) {
            throw new IllegalArgumentException("回调签名不能为空");
        }
        String expected = hmacSha256(buildSignatureContent(body, rawBody), properties.getCallbackSecret());
        if (!signature.equals(expected)) {
            throw new IllegalArgumentException("回调签名校验失败");
        }
    }

    /**
     * 构造签名明文，尽量保留原始 body，避免 JSON 重排导致签名不一致。
     */
    private String buildSignatureContent(JSONObject body, String rawBody) {
        if (!isBlank(rawBody)) {
            return body != null && body.containsKey("signature") ? stripSignatureField(rawBody) : rawBody;
        }
        JSONObject signBody = new JSONObject();
        signBody.putAll(body);
        signBody.remove("signature");
        return signBody.toJSONString();
    }

    /**
     * 从原始 body 中剔除 signature 字段，用于兼容签名放 body 的场景。
     */
    private String stripSignatureField(String rawBody) {
        String stripped = SIGNATURE_MIDDLE_PATTERN.matcher(rawBody).replaceFirst("");
        if (!stripped.equals(rawBody)) {
            return stripped;
        }
        stripped = SIGNATURE_LEADING_PATTERN.matcher(rawBody).replaceFirst("");
        if (!stripped.equals(rawBody)) {
            return stripped;
        }
        return SIGNATURE_ONLY_PATTERN.matcher(rawBody).replaceFirst("");
    }

    /**
     * 计算 HMAC-SHA256 小写十六进制签名。
     */
    private String hmacSha256(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    builder.append('0');
                }
                builder.append(hex);
            }
            return builder.toString();
        } catch (Exception e) {
            throw new IllegalStateException("回调签名计算失败", e);
        }
    }

    /**
     * 忽略大小写读取请求头。
     */
    private String getIgnoreCase(Map<String, String> map, String key) {
        if (map == null || key == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (key.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 过滤环境变量默认空值，避免空白字符串被当成有效白名单。
     */
    private List<String> normalizeList(List<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return values;
        }
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            if (!isBlank(value)) {
                result.add(value.trim());
            }
        }
        return result;
    }

    /** 判断字符串是否为空白。 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
