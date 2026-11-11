FROM mcr.microsoft.com/openjdk/jdk:21-azurelinux AS build

RUN tdnf install -y findutils && tdnf clean all

WORKDIR /build

COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src/main ./src/main

RUN chmod +x ./gradlew && ./gradlew installDist --no-daemon

FROM mcr.microsoft.com/openjdk/jdk:21-distroless AS runtime

WORKDIR /app
COPY --from=build /build/build/install/ktor-sample/lib/ ./lib/
USER app
EXPOSE 8080

CMD ["-cp", "/app/lib/*", "io.ktor.server.netty.EngineMain"]
