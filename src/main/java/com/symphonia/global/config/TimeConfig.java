package com.symphonia.global.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 서비스의 모든 시각은 한국 시간이다. 정산 메시지의 10:00 발송 규칙과 48시간 기록 기간이 같은 시간대로 계산돼야 하기 때문이다.
// LocalDateTime.now()와 DB의 TIMESTAMP 컬럼이 서버 시간대를 따르므로, 서버가 UTC로 떠도 시각이 어긋나지 않게 JVM 기본 시간대를 고정한다.
@Configuration
public class TimeConfig {
    public static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    // 시각에 따라 동작이 달라지는 서비스가 주입받아, 테스트에서 시각을 고정할 수 있게 한다.
    @Bean
    public Clock clock() {
        return Clock.system(SERVICE_ZONE);
    }

    // 스프링 컨텍스트가 뜨기 전에 만들어지는 시각까지 맞추도록 main에서 가장 먼저 호출한다.
    public static void useServiceZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(SERVICE_ZONE));
    }
}
