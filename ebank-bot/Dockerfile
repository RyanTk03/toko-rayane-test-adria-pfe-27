ARG JAVA_VERSION=17

# --- Build ---
FROM maven:3.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Exécution ---
FROM eclipse-temurin:${JAVA_VERSION}-jre
WORKDIR /app
RUN useradd --system --no-create-home spring
COPY --from=build /app/target/*.jar app.jar
USER spring
ENTRYPOINT ["java", "-jar", "app.jar"]