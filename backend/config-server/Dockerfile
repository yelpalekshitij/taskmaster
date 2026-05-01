FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY backend/config-server/build/libs/*.jar app.jar
COPY backend/config-server/config-files/ ./config-files/
EXPOSE 8888
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
