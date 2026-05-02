FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY backend/spring-boot-admin-server/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
