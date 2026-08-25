FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S lose10 && adduser -S lose10 -G lose10
COPY --from=build /app/target/*.jar app.jar
USER lose10
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
