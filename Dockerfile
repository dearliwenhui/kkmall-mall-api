FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget

RUN wget -q https://archive.apache.org/dist/skywalking/java-agent/9.4.0/apache-skywalking-java-agent-9.4.0.tgz -O /tmp/skywalking.tgz && \
    mkdir -p /app/skywalking && \
    tar -xzf /tmp/skywalking.tgz -C /tmp && \
    cp -r /tmp/skywalking-agent/* /app/skywalking/ && \
    rm -rf /tmp/skywalking.tgz /tmp/skywalking-agent && \
    mkdir -p /app/skywalking/logs && \
    chmod -R 755 /app/skywalking && \
    ls -la /app/skywalking/ && \
    test -f /app/skywalking/skywalking-agent.jar && echo "Agent JAR found" || echo "Agent JAR NOT found"

RUN addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 38081

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:38081/actuator/health || exit 1

ENTRYPOINT ["java", "-javaagent:/app/skywalking/skywalking-agent.jar", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
