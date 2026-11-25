# Imagen del MVP El Apuntado: compila con Maven Wrapper y ejecuta el .jar sobre un JRE 17.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode --quiet dependency:go-offline
COPY config config
COPY src src
RUN ./mvnw --batch-mode --quiet -Dmaven.test.skip=true package \
 && cp target/arquitectura-entregable2-*.jar /build/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
ENV APUNTADO_AUDIT_DIR=/app/logs \
    APUNTADO_DECK=random
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
