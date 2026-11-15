package com.symphonia.place.infrastructure.naver.config;

import com.symphonia.place.infrastructure.naver.config.properties.NaverLocalSearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(NaverLocalSearchProperties.class)
public class NaverPlaceConfig {}
