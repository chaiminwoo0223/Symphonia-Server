package com.symphonia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// 스프링 컨텍스트는 테스트 클래스 조합마다 따로 생기므로, 컨테이너를 static 필드로 두어 테스트 JVM 하나에서 한 번만 띄운다.
// 컨테이너를 빈으로 등록하면 컨텍스트 캐시가 오래된 컨텍스트를 닫을 때 공유 컨테이너까지 멈춘다.
// 그래서 빈으로 두지 않고 접속 정보만 넘기며, 컨테이너는 테스트 JVM이 끝날 때 Testcontainers가 정리한다.
// 테스트는 트랜잭션 롤백이나 @AfterEach 정리로 데이터를 남기지 않아 컨텍스트끼리 DB와 Redis를 같이 써도 된다.
// RabbitMQ는 캐시된 다른 컨텍스트의 리스너가 메시지를 가져갈 수 있어 RabbitTestcontainersConfiguration에서 컨텍스트마다 띄운다.
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    private static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                            DockerImageName.parse("pgvector/pgvector:pg17")
                                    .asCompatibleSubstituteFor("postgres"))
                    // 컨텍스트 캐시 상한 32개가 모두 차도 컨텍스트마다 최대 4개인 커넥션 풀이 상한을 넘지 않게 늘린다.
                    // 기본 명령을 덮어쓰므로 Testcontainers 기본값인 fsync=off도 함께 넘긴다.
                    .withCommand("postgres", "-c", "fsync=off", "-c", "max_connections=200");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:8.8")).withExposedPorts(REDIS_PORT);

    static {
        Startables.deepStart(POSTGRES, REDIS).join();
    }

    @Bean
    DynamicPropertyRegistrar containerProperties() {
        return registry -> {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.data.redis.host", REDIS::getHost);
            registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
        };
    }
}
