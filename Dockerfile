FROM gradle:8.7.0-jdk17-alpine AS build
WORKDIR /build

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew quarkusBuild --no-daemon

FROM eclipse-temurin:17-alpine AS runtime
WORKDIR /app

COPY --from=build /build/build/quarkus-app/lib/ /app/lib/
COPY --from=build /build/build/quarkus-app/quarkus/ /app/quarkus/
COPY --from=build /build/build/quarkus-app/app/ /app/app/
COPY --from=build /build/build/quarkus-app/quarkus-run.jar /app/quarkus-run.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/quarkus-run.jar"]
