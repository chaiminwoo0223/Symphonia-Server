package com.symphonia.pairing.infrastructure.reader.config;

import com.symphonia.pairing.infrastructure.reader.config.properties.BjcpProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 음료 적재 원본을 읽는 리더가 쓰는 설정을 등록한다.
@Configuration
@EnableConfigurationProperties(BjcpProperties.class)
public class ReaderConfig {}
