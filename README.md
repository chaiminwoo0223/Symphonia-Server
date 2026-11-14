# Symphonia-Server
심포니아

## 실행 환경

- Java 25가 필요하다. CI도 Java 25로 빌드한다.
- `./gradlew`는 Gradle toolchain이 Java 25를 찾아 쓰지만, `java -jar`는 `JAVA_HOME`의 Java를 쓴다. 다른 버전이면 `UnsupportedClassVersionError`로 기동에 실패한다.
- `java -version`으로 25인지 확인하고, 다르면 `JAVA_HOME`을 25로 지정한다. macOS 예시는 `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`다.
