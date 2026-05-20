FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY backend/pom.xml backend/pom.xml
COPY backend/mmpay-bom/pom.xml backend/mmpay-bom/pom.xml
COPY backend/mmpay-common/pom.xml backend/mmpay-common/pom.xml
COPY backend/mmpay-gateway-core/pom.xml backend/mmpay-gateway-core/pom.xml
COPY backend/mmpay-adapter-spi/pom.xml backend/mmpay-adapter-spi/pom.xml
COPY backend/mmpay-adapter-huifu/pom.xml backend/mmpay-adapter-huifu/pom.xml
COPY backend/mmpay-webhook-out/pom.xml backend/mmpay-webhook-out/pom.xml
COPY backend/mmpay-license-relay/pom.xml backend/mmpay-license-relay/pom.xml
COPY backend/mmpay-admin-api/pom.xml backend/mmpay-admin-api/pom.xml
COPY backend/mmpay-app/pom.xml backend/mmpay-app/pom.xml
COPY backend backend

RUN mvn -f backend/pom.xml -pl mmpay-app -am -DskipTests source:jar package

FROM eclipse-temurin:21-jdk

WORKDIR /symbols

RUN groupadd --system mmpay && useradd --system --gid mmpay --home-dir /symbols mmpay \
    && mkdir -p /symbols/sources

COPY --from=build /workspace/backend/mmpay-app/target/mmpay-app-0.1.0-SNAPSHOT.jar /symbols/mmpay-app.jar
COPY --from=build /workspace/backend /symbols/source-tree

RUN find /symbols/source-tree -type d -name target -prune -exec rm -rf {} + 2>/dev/null || true

USER mmpay

CMD ["sh", "-c", "echo 'mmpay debug symbols + sources image'"]
