FROM docker.io/amazoncorretto:21-alpine
# Альтернативные зеркала (раскомментируйте при необходимости):
# FROM registry.cn-hangzhou.aliyuncs.com/amazoncorretto/amazoncorretto:21-alpine
# FROM mirror.gcr.io/amazoncorretto:21-alpine
# FROM public.ecr.aws/amazoncorretto/amazoncorretto:21-alpine
WORKDIR /app

# JAR файл будет скопирован сюда на этапе деплоя (из GitHub Actions)
COPY app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]