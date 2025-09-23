FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/bankcards-*.jar /app/
CMD ["sh", "-c", "exec java -jar $(ls /app/bankcards-*.jar)"]
