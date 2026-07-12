package com.linkwisehub.modules.ai.knowledge.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.linkwisehub.common.ErrorCode;
import com.linkwisehub.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OnlyOfficeJwtSupport {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    public String createToken(Map<String, Object> payload, String secret) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        return encodedHeader + "." + encodedPayload + "." + sign(encodedHeader + "." + encodedPayload, secret);
    }

    public JSONObject verifyToken(String token, String secret) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "预览文件令牌不能为空");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "预览文件令牌格式无效");
        }
        String expectedSign = sign(parts[0] + "." + parts[1], secret);
        if (!constantTimeEquals(expectedSign, parts[2])) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "预览文件令牌签名无效");
        }
        JSONObject payload = JSON.parseObject(new String(BASE64_URL_DECODER.decode(parts[1]), StandardCharsets.UTF_8));
        Long exp = payload.getLong("exp");
        if (exp == null || exp < Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "预览文件令牌已过期");
        }
        return payload;
    }

    private String encodeJson(Map<String, Object> value) {
        return BASE64_URL_ENCODER.encodeToString(JSON.toJSONString(value).getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "生成 OnlyOffice 令牌失败: " + e.getMessage());
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < left.length(); i++) {
            result |= left.charAt(i) ^ right.charAt(i);
        }
        return result == 0;
    }
}
