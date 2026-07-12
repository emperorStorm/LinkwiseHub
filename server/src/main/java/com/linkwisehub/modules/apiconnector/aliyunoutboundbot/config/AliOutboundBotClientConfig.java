package com.linkwisehub.modules.apiconnector.aliyunoutboundbot.config;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.outboundbot20191226.AsyncClient;
import darabonba.core.client.ClientOverrideConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 阿里智能外呼 SDK Client 配置。
 */
@Configuration
@ConditionalOnProperty(prefix = "aliyun.outbound-bot", name = "enabled", havingValue = "true")
public class AliOutboundBotClientConfig {

    /**
     * 创建阿里 OutboundBot 异步客户端，统一绑定 endpoint、region 和超时配置。
     */
    @Bean
    public AsyncClient aliOutboundBotAsyncClient(AliOutboundBotProperties properties) {
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(properties.getAccessKeyId())
                .accessKeySecret(properties.getAccessKeySecret())
                .build());

        ClientOverrideConfiguration overrideConfiguration = ClientOverrideConfiguration.create()
                .setEndpointOverride(properties.getEndpoint())
                .setConnectTimeout(Duration.ofSeconds(defaultIfNull(properties.getConnectTimeoutSeconds(), 3)))
                .setResponseTimeout(Duration.ofSeconds(defaultIfNull(properties.getReadTimeoutSeconds(), 10)));

        return AsyncClient.builder()
                .credentialsProvider(provider)
                .region(properties.getRegionId())
                .overrideConfiguration(overrideConfiguration)
                .build();
    }

    /**
     * 处理空配置，避免 Duration 接收到 null。
     */
    private long defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value.longValue();
    }
}
