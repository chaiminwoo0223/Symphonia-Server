# syntax=docker/dockerfile:1

# jar는 아키텍처와 무관하므로 빌드는 이미지를 만드는 머신에서 실행한다. arm64 머신에서 amd64 이미지를 만들어도 컴파일은 에뮬레이션을 거치지 않는다.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar \
    && cp build/libs/*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 symphonia
# 자주 바뀌지 않는 계층부터 복사해, 코드만 바뀐 배포는 마지막 계층만 새로 push하고 pull한다.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER symphonia
EXPOSE 8080 8081
# exec 형식이어야 JVM이 SIGTERM을 직접 받아 graceful shutdown을 실행한다.
ENTRYPOINT ["java", "-jar", "application.jar"]
