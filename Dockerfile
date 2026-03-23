FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache wget && \
    wget -q https://archive.apache.org/dist/skywalking/java-agent/9.4.0/apache-skywalking-java-agent-9.4.0.tgz -O /tmp/skywalking.tgz && \
    mkdir -p /app/skywalking && \
    tar -xzf /tmp/skywalking.tgz -C /tmp && \
    cp -r /tmp/skywalking-agent/* /app/skywalking/ && \
    cp /app/skywalking/optional-plugins/apm-spring-webmvc-6.x-plugin-*.jar /app/skywalking/plugins/ && \
    cp /app/skywalking/optional-plugins/apm-spring-resttemplate-6.x-plugin-*.jar /app/skywalking/plugins/ && \
    cp /app/skywalking/optional-plugins/apm-trace-ignore-plugin-*.jar /app/skywalking/plugins/ && \
    rm -rf /tmp/skywalking.tgz /tmp/skywalking-agent && \
    mkdir -p /app/skywalking/logs

RUN addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 38081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:38081/actuator/health || exit 1

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "exec java -javaagent:/app/skywalking/skywalking-agent.jar $JAVA_OPTS -jar app.jar"]
