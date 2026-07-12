package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里智能外呼配置属性。
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.outbound-bot")
public class AliOutboundBotProperties {
    /** 是否启用外呼能力。 */
    private Boolean enabled = Boolean.FALSE;
    /** RAM AccessKeyId。 */
    private String accessKeyId;
    /** RAM AccessKeySecret。 */
    private String accessKeySecret;
    /** 阿里 OutboundBot endpoint。 */
    private String endpoint = "outboundbot.cn-shanghai.aliyuncs.com";
    /** 阿里地域 ID。 */
    private String regionId = "cn-shanghai";
    /** 智能外呼实例 ID。 */
    private String instanceId;
    /** 默认话术 ID。 */
    private String scriptId;
    /** 默认场景 ID。 */
    private String scenarioId;
    /** 默认主叫号码。 */
    private List<String> callingNumbers = new ArrayList<>();
    /** 默认重呼主叫号码。 */
    private List<String> recallCallingNumbers = new ArrayList<>();
    /** 回调签名密钥；为空时跳过签名校验。 */
    private String callbackSecret;
    /** 回调来源 IP 白名单；为空时跳过来源校验。 */
    private List<String> sourceIpWhitelist = new ArrayList<>();
    /** 连接超时时间，单位秒。 */
    private Integer connectTimeoutSeconds = 3;
    /** 响应超时时间，单位秒。 */
    private Integer readTimeoutSeconds = 10;
    /** 异步 SDK 等待时间，单位秒。 */
    private Integer asyncWaitTimeoutSeconds = 10;
    /** 回调时间戳允许偏差，单位秒。 */
    private Long callbackTimestampToleranceSeconds = 300L;
}
