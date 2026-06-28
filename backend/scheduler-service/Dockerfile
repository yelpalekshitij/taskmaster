FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY backend/scheduler-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
