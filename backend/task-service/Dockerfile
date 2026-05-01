FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY backend/task-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
