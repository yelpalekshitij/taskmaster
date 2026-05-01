FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY backend/service-registry/build/libs/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
