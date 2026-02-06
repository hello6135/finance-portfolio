# 1단계: 빌드 스테이지 (JDK 21 사용)
FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build -x test --no-daemon

# 2단계: 실행 스테이지 (JDK 21 사용)
FROM eclipse-temurin:21-jre-jammy
EXPOSE 8080
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
