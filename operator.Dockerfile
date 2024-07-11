FROM sbtscala/scala-sbt:eclipse-temurin-alpine-21.0.2_13_1.10.0_3.4.2 AS builder
WORKDIR /src
COPY . /src

RUN sbt update
RUN sbt operator/assembly

FROM openjdk:21-bookworm
COPY --from=builder /src/app/operator/target/scala-3.4.2/app-operator-assembly-0.1.0-SNAPSHOT.jar /src/operator.jar
ENTRYPOINT ["java", "-jar", "/src/operator.jar"]
