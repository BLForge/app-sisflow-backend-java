FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system sisflow && useradd --system --gid sisflow --create-home sisflow

WORKDIR /app

COPY --from=build /app/target/sisflow-0.0.1-SNAPSHOT.jar app.jar
RUN chown sisflow:sisflow /app/app.jar
USER sisflow

EXPOSE 9090

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
