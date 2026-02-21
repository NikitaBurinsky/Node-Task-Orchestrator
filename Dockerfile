FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-alpine
WORKDIR /app

# JAR файл будет скопирован сюда на этапе деплоя (из GitHub Actions)
COPY app.jar /app/app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]