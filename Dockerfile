FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/boe-proxy-service-*.jar app.jar

# JVM flags for module access (matches existing project conventions)
ENV JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED \
               --add-opens java.base/java.util=ALL-UNNAMED \
               -Xms256m -Xmx512m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
