FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY backend/user-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
