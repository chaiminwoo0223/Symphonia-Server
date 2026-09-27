package com.symphonia.pairing.infrastructure.reader.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "import.bjcp")
public record BjcpProperties(String path) {}
