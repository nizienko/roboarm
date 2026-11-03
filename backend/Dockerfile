FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src
RUN chmod +x gradlew && ./gradlew --no-daemon test installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/roboarm/ ./
EXPOSE 6593
ENTRYPOINT ["/app/bin/roboarm"]
