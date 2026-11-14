# Symphonia-Server
심포니아

## 실행 환경

- Java 25가 필요하다. CI도 Java 25로 빌드한다.
- `./gradlew`는 `JAVA_HOME` 또는 `PATH`의 Java로 Gradle 자체를 먼저 실행하므로, Gradle을 시작할 Java가 하나는 있어야 한다. 그 뒤 컴파일과 테스트에 쓰는 Java 25는 Gradle toolchain이 찾아 쓴다.
- `java -jar`는 `PATH`의 `java`를 실행하므로 `JAVA_HOME`만 바꿔서는 실행되는 Java가 바뀌지 않는다. 다른 버전이면 `UnsupportedClassVersionError`로 기동에 실패한다.
- `java -version`으로 25인지 확인하고, 다르면 `PATH`의 Java를 25로 바꾸거나 `"$JAVA_HOME/bin/java" -jar`로 직접 실행한다. macOS 예시는 `export JAVA_HOME=$(/usr/libexec/java_home -v 25)`다.
