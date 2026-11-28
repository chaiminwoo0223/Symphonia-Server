package com.symphonia.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 웹 서버 없이 실행하는 카탈로그 적재에서는 스케줄러를 켜지 않는다. 스케줄러 스레드가 JVM을 붙잡으면 적재를 마쳐도 프로세스가 끝나지 않는다.
@Configuration
@ConditionalOnWebApplication
@EnableScheduling
public class SchedulingConfig {}
