FROM node:22-bookworm-slim AS build

WORKDIR /workspace

RUN corepack enable && corepack prepare pnpm@10.32.1 --activate

COPY frontend-admin frontend-admin
RUN pnpm --dir frontend-admin install --frozen-lockfile
RUN pnpm --dir frontend-admin build

FROM nginx:1.27-alpine

RUN addgroup -S mmpay && adduser -S -G mmpay mmpay

COPY --from=build /workspace/frontend-admin/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
USER mmpay
