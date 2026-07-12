package com.linkwisehub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI服务配置类
 * 支持 DeepSeek、阿里云百炼、MiniMax、Ollama 等多个 AI 服务提供商。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /**
     * AI服务提供商
     * 可选值: deepseek, aliyun, minimax, ollama
     */
    private String provider = "deepseek";

    /**
     * 默认模型
     * 可选值: deepseek-v4-flash, qwen3.5-plus, MiniMax-M2.7-highspeed, qwen3.6:latest
     */
    private String defaultModel = "deepseek-v4-flash";

    /**
     * DeepSeek配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();

    /**
     * 阿里云百炼配置
     */
    private AliyunConfig aliyun = new AliyunConfig();

    /**
     * MiniMax配置
     */
    private MiniMaxConfig minimax = new MiniMaxConfig();

    /**
     * Ollama本地模型配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 聊天配置
     */
    private ChatConfig chat = new ChatConfig();

    /**
     * DeepSeek具体配置
     */
    @Data
    public static class DeepSeekConfig {

        /**
         * DeepSeek API Key，建议通过环境变量注入。
         */
        private String apiKey;

        /**
         * 使用的模型名称，默认使用 V4 Flash。
         */
        private String model = "deepseek-v4-flash";

        /**
         * API基础地址，DeepSeek兼容OpenAI格式。
         */
        private String baseUrl = "https://api.deepseek.com";

        /**
         * 请求超时时间(毫秒)
         */
        private int timeout = 30000;

        /**
         * 最大生成token数
         */
        private int maxTokens = 2000;

        /**
         * 创造性参数 (0-1)，越高越有创造性
         */
        private double temperature = 0.7;
    }

    /**
     * 阿里云百炼具体配置
     */
    @Data
    public static class AliyunConfig {

        /**
         * 阿里云百炼API Key
         */
        private String apiKey;

        /**
         * 使用的模型名称
         * 可选值: qwen-turbo (快速), qwen-plus (增强), qwen3.5-plus (综合能力强)
         */
        private String model = "qwen3.5-plus";

        /**
         * API基础地址
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

        /**
         * 请求超时时间(毫秒)
         */
        private int timeout = 30000;

        /**
         * 最大生成token数
         */
        private int maxTokens = 2000;

        /**
         * 创造性参数 (0-1)，越高越有创造性
         */
        private double temperature = 0.7;
    }

    /**
     * MiniMax具体配置
     */
    @Data
    public static class MiniMaxConfig {

        /**
         * MiniMax API Key
         */
        private String apiKey;

        /**
         * 使用的模型名称
         * 可选值: MiniMax-M2.7, MiniMax-M2.7-highspeed, MiniMax-M2.5, MiniMax-M2.5-highspeed
         */
        private String model = "MiniMax-M2.7-highspeed";

        /**
         * API基础地址
         */
        private String baseUrl = "https://api.minimax.chat/v1";

        /**
         * 请求超时时间(毫秒)
         */
        private int timeout = 30000;

        /**
         * 最大生成token数
         */
        private int maxTokens = 2000;

        /**
         * 创造性参数 (0.0, 1.0]，越高越有创造性，MiniMax不支持0
         */
        private double temperature = 1.0;
    }

    /**
     * Ollama本地模型具体配置
     */
    @Data
    public static class OllamaConfig {

        /**
         * 使用的本地模型名称。
         */
        private String model = "qwen3.6:latest";

        /**
         * Ollama本地服务基础地址。
         */
        private String baseUrl = "http://127.0.0.1:11434";

        /**
         * 请求超时时间(毫秒)
         */
        private int timeout = 600000;

        /**
         * 最大生成token数
         */
        private int maxTokens = 2000;

        /**
         * 创造性参数 (0-1)，越高越有创造性
         */
        private double temperature = 0.7;
    }

    /**
     * 重试策略配置
     */
    @Data
    public static class RetryConfig {

        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始重试间隔（毫秒）
         */
        private long delayMs = 1000;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxDelayMs = 5000;

        /**
         * 乘法因子（用于指数退避）
         */
        private double multiplier = 2.0;
    }

    /**
     * 聊天上下文配置
     */
    @Data
    public static class ChatConfig {
        private HistoryConfig history = new HistoryConfig();
    }

    /**
     * 对话历史管理配置
     */
    @Data
    public static class HistoryConfig {
        private int recentRounds = 6;
        private int localRecentRounds = 3;
        private int summaryTriggerRounds = 6;
        private int summaryMaxChars = 1200;
        private int historyMaxChars = 8000;
        private int localHistoryMaxChars = 3000;
    }
}
