FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN ./mvnw --batch-mode package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --user-group jobtrackr
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER jobtrackr
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
