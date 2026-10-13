package com.symphonia.pairing.infrastructure.reader.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "import.x-wines")
public record XWinesProperties(String path) {}
