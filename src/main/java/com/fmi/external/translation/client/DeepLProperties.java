package com.fmi.external.translation.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "translation.deepl")
public record DeepLProperties(String apiKey, String baseUrl) {}
