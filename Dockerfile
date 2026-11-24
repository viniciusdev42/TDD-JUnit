# syntax=docker/dockerfile:1

# ==============================================================================
# Estágio 1: Build e execução dos testes com Maven + JDK 17
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Baixa primeiro as dependências para aproveitar o cache de camadas do Docker
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copia o código-fonte e os testes
COPY src ./src

# Compila e executa os testes unitários (JUnit 5 via Surefire)
RUN mvn -B test

# ==============================================================================
# Estágio 2: Imagem final leve para rodar os testes em CI / container
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17-alpine AS runtime

WORKDIR /app

# Reaproveita o repositório local Maven (.m2) e o projeto já preparado
COPY --from=build /root/.m2 /root/.m2
COPY --from=build /app /app

# Por padrão, executa os testes ao iniciar o container
CMD ["mvn", "-B", "test"]
