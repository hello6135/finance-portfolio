# 1단계: 빌드 스테이지
FROM gradle:8.5-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
# 빌드 시점에 필요한 환경변수가 있다면 여기서 처리하거나, 테스트 제외 빌드
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 빌드 스테이지에서 생성된 jar를 복사 (이름을 app.jar로 고정)
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

EXPOSE 8080
# Graceful Shutdown 설정 추가
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

ENTRYPOINT ["java", "-jar", "app.jar"]