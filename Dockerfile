FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --update-snapshots verify

FROM jetty:10.0-jdk21

COPY --from=build /build/target/HnJobs-1.0-SNAPSHOT.war webapps/ROOT.war

HEALTHCHECK CMD curl --fail http://localhost:8080/healthz/live || exit 1
