package com.fmi.config;

import java.util.List;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cors")
@Getter
public class CorsProperties {
    private final List<String> allowedOriginPatterns;

    public CorsProperties(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns = allowedOriginPatterns != null ? allowedOriginPatterns : List.of();
    }
}
