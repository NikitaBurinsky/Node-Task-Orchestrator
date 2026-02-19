FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# JAR файл будет скопирован сюда на этапе деплоя (из GitHub Actions)
COPY app.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]