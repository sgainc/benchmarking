FROM amazoncorretto:21-alpine-jdk
LABEL authors="sw"
ARG JAR_FILE=build/libs/async-bench-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8080
