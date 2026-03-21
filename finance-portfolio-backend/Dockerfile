# 1단계: 빌드 스테이지
# deploy 워크플로우의 (Sonar) Code Quality Analysis 단계에서 빌드하게 변경

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 소나 워크플로우에서 생성된 jar를 복사 (이름을 app.jar로 고정)
COPY build/libs/*.jar app.jar

EXPOSE 8080
# Graceful Shutdown 설정 추가
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

ENTRYPOINT ["java", "-jar", "app.jar"]