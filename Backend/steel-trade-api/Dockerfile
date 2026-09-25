# Etapa 1: build com o JDK completo e o wrapper do Maven
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# camada de dependências separada: só invalida quando o pom muda
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# checkout no Windows perde o bit de execução do mvnw
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -q package -DskipTests

# Etapa 2: runtime enxuto, só o JRE, sem root
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1000 --no-create-home app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
