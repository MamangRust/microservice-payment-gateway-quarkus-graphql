# ---- Build Stage ----
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY common/pom.xml common/
COPY gateway/pom.xml gateway/
COPY auth/pom.xml auth/
COPY card/pom.xml card/
COPY merchant/pom.xml merchant/
COPY role/pom.xml role/
COPY saldo/pom.xml saldo/
COPY topup/pom.xml topup/
COPY transaction/pom.xml transaction/
COPY transfer/pom.xml transfer/
COPY user/pom.xml user/
COPY withdraw/pom.xml withdraw/
COPY email-service/pom.xml email-service/
COPY chaos.yaml chaos.yaml

COPY common/src common/src
COPY user/src user/src

RUN mvn clean package -pl user -am -DskipTests -q

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/user/target/quarkus-app/ ./
COPY --from=build /app/chaos.yaml ./
EXPOSE 9011 8091
ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
