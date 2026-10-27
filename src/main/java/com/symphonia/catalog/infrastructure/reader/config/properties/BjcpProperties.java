package com.symphonia.catalog.infrastructure.reader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "import.bjcp")
public record BjcpProperties(String path) {}
