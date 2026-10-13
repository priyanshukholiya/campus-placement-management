FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY common common
COPY student-service student-service
COPY recruitment-service recruitment-service
COPY application-service application-service
COPY notification-service notification-service
COPY api-gateway api-gateway
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/${SERVICE}-0.1.0.jar /app/service.jar
USER 10001:10001
ENV BIND_ADDRESS=0.0.0.0
ENTRYPOINT ["java","-Xms64m","-Xmx256m","-jar","/app/service.jar"]
