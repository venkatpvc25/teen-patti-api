FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S foodie && adduser -S foodie -G foodie
COPY --from=build /workspace/target/*.jar app.jar

USER foodie
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
