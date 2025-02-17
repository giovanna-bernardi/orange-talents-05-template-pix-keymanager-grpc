FROM openjdk:11
ARG JAR_FILE=build/libs/*-all.jar
COPY ${JAR_FILE} app.jar
EXPOSE 50051
ENV APP_NAME keymanager-grpc
ENTRYPOINT ["java","-jar","/app.jar"]