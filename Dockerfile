FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el JAR construido
COPY target/pedidos-app-0.0.1-SNAPSHOT.jar app.jar

# Crear usuario no-root para mayor seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]