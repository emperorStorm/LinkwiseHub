package com.linkwisehub;

import com.linkwisehub.config.AiConfig;
import com.linkwisehub.config.OnlyOfficeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AiConfig.class, OnlyOfficeProperties.class})
public class LinkwiseHubApplication {

    private static final Logger logger = LoggerFactory.getLogger(LinkwiseHubApplication.class);

    @Autowired
    private Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(LinkwiseHubApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        
        logger.info("===============================================");
        logger.info("智链中枢启动成功！");
        logger.info("===============================================");
        logger.info("访问地址: http://localhost:{}{}", port, contextPath);
        logger.info("API 文档: http://localhost:{}{}/swagger-ui.html", port, contextPath);
        logger.info("===============================================");
    }
}
