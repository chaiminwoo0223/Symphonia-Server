package com.symphonia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

// 큐 리스너가 뜨는 @SpringBootTest 컨텍스트만 가져온다.
// 빈으로 두어 컨텍스트마다 브로커를 따로 띄워야 다른 컨텍스트의 리스너가 메시지를 가져가지 않는다.
@TestConfiguration(proxyBeanMethods = false)
class RabbitTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.3-management"));
    }
}
