# Build stage
FROM eclipse-temurin:17.0.21_9-jdk-noble AS builder
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon clean bootJar

# Runtime stage
# JDK 패치 버전까지 태그로 고정한다. digest는 고정하지 않아 같은 태그의 OS 보안 패치를 재빌드 시 받는다.
FROM eclipse-temurin:17.0.21_9-jre-noble
ENV TZ=Asia/Seoul \
    JAVA_OPTS="-Duser.timezone=Asia/Seoul"
RUN groupadd --system app && useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
# exec로 java를 PID 1로 실행해 docker stop의 SIGTERM을 직접 받는다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
