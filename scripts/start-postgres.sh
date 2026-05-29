#!/bin/bash

# Запуск контейнера PostgreSQL
docker run -d \
  --name ragsystem-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=ragsystem \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -v postgres_data:/var/lib/postgresql/data \
  --restart unless-stopped \
  postgres:15