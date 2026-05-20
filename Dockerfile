FROM node:22-bookworm-slim AS frontend-build

WORKDIR /workspace

RUN corepack enable && corepack prepare pnpm@10.32.1 --activate

COPY frontend-admin frontend-admin
RUN pnpm --dir frontend-admin install --frozen-lockfile
RUN pnpm --dir frontend-admin build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build

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
COPY --from=frontend-build /workspace/frontend-admin/dist backend/mmpay-app/src/main/resources/static

RUN mvn -f backend/pom.xml -pl mmpay-app -am -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --system mmpay && useradd --system --gid mmpay --home-dir /app mmpay

COPY --from=backend-build /workspace/backend/mmpay-app/target/mmpay-app-1.0.0.jar /app/mmpay-app.jar
COPY deploy/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod 0555 /app/docker-entrypoint.sh

EXPOSE 8080
USER mmpay

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "/app/mmpay-app.jar"]
