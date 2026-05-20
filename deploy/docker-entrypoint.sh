#!/usr/bin/env sh
set -eu

if [ "$#" -eq 0 ]; then
  set -- java -jar /app/mmpay-app.jar
fi

if [ "${1:-}" = "java" ] && [ "${2:-}" = "-jar" ] && [ "${3:-}" = "/app/mmpay-app.jar" ]; then
  missing=""
  for name in SPRING_DATASOURCE_URL SPRING_DATASOURCE_USERNAME SPRING_DATASOURCE_PASSWORD MMPAY_AUDIT_HMAC_KEY; do
    eval "value=\${$name:-}"
    if [ -z "$value" ]; then
      if [ -n "$missing" ]; then
        missing="$missing, $name"
      else
        missing="$name"
      fi
    fi
  done

  if [ -n "$missing" ]; then
    echo "MMPay startup configuration error: missing $missing" >&2
    echo "Run with deploy/docker-compose.yml or provide the Spring datasource and audit key environment variables explicitly." >&2
    exit 78
  fi
fi

exec "$@"
