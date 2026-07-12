package com.linkwisehub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    private String documentServerUrl;
    private String publicBackendUrl;
    private String jwtSecret;

    public String getDocumentServerUrl() {
        return documentServerUrl;
    }

    public void setDocumentServerUrl(String documentServerUrl) {
        this.documentServerUrl = documentServerUrl;
    }

    public String getPublicBackendUrl() {
        return publicBackendUrl;
    }

    public void setPublicBackendUrl(String publicBackendUrl) {
        this.publicBackendUrl = publicBackendUrl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
