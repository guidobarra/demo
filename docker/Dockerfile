# ---------- STAGE 1: Compilar con Maven ----------
FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- STAGE 2: Imagen final para ejecución ----------
FROM eclipse-temurin:21-jre-alpine

# Crear carpeta de trabajo
WORKDIR /app

COPY --from=builder /build/target/demo*.jar app.jar
COPY src/main/resources/application.yml .

EXPOSE 9292
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]