# Fase de construcción
FROM maven:3.8.6-openjdk-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Fase de producción
FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=builder /app/target/pedidos-app-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]