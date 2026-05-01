FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY backend/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
