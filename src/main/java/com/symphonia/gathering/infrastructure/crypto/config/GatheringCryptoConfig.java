package com.symphonia.gathering.infrastructure.crypto.config;

import com.symphonia.gathering.infrastructure.crypto.config.properties.GatheringCryptoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatheringCryptoProperties.class)
public class GatheringCryptoConfig {}
