package com.symphonia.catalog.infrastructure.reader.config;

import com.symphonia.catalog.infrastructure.reader.config.properties.BjcpProperties;
import com.symphonia.catalog.infrastructure.reader.config.properties.XWinesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 적재 원본을 읽는 리더가 쓰는 설정을 등록한다.
@Configuration
@EnableConfigurationProperties({BjcpProperties.class, XWinesProperties.class})
public class ReaderConfig {}
