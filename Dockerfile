FROM gradle:8.14.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle --no-daemon clean test installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/install/vehicle-signal-gateway/ ./
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["/app/bin/vehicle-signal-gateway"]
