FROM amazoncorretto:17-alpine-jdk

COPY target/pedidos-app-0.0.1-SNAPSHOT.jar /api-pedidosapp-v1.jar

ENTRYPOINT ["java", "-jar", "/api-pedidosapp-v1.jar"]