package com.symphonia.gathering.infrastructure.sender.config;

import com.symphonia.gathering.infrastructure.sender.config.properties.NotificationLinkProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NotificationLinkProperties.class)
public class NotificationSenderConfig {}
