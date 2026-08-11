package com.fmi.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;


@ConfigurationProperties(prefix = "cors")
@Getter
public class CorsProperties {
    private final List<String> allowedOriginPatterns;

    public CorsProperties(List<String> allowedOriginPatterns) {
        this.allowedOriginPatterns =
                allowedOriginPatterns != null ? allowedOriginPatterns : List.of();
    }
}
