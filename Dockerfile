# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: imagen de ejecución ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/app.jar app.jar

# /app/efs  -> punto de montaje del EFS (docker run -v /mnt/efs:/app/efs)
# /app/data -> base de datos H2 con los metadatos de los tickets
RUN mkdir -p /app/efs /app/data
ENV EFS_PATH=/app/efs \
    DB_PATH=/app/data/ticketsdb

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
