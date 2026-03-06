# 1단계: 빌드 스테이지
# Sonar 작동에 빌드가 선행되어야 하기에
# 빌드 스테이지 삭제로 중복 제거

# 2단계: 실행 스테이지
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# 빌드 스테이지에서 생성된 jar를 복사 (이름을 app.jar로 고정)
COPY build/libs/*.jar app.jar

EXPOSE 8080
# Graceful Shutdown 설정 추가
ENV SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE=30s

ENTRYPOINT ["java", "-jar", "app.jar"]