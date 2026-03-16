# ── Stage 1: Build ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-25-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:resolve -q

COPY src src
RUN mvn package -DskipTests -q && \
    mv target/vertx-*-fat.jar target/app.jar

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

USER app

EXPOSE 9292

ENV JAVA_XMS=256m
ENV JAVA_XMX=512m
ENV TZ=UTC

ENTRYPOINT ["sh", "-c", "java \
  -XX:+UseZGC \
  -Xms${JAVA_XMS} \
  -Xmx${JAVA_XMX} \
  -Duser.timezone=${TZ} \
  -Dfile.encoding=UTF-8 \
  -Djava.security.egd=file:/dev/./urandom \
  -XX:+UseStringDeduplication \
  -XX:+ExitOnOutOfMemoryError \
  -jar app.jar"]
