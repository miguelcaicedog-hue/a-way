FROM maven:3.9.11-eclipse-temurin-11 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM tomcat:9.0-jdk11-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /src/target/a-way-core.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
