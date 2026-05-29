#!/bin/bash

# Запуск контейнера Qdrant
docker run -d \
  --name ragsystem-qdrant \
  -p 6333:6333 \
  -p 6334:6334 \
  -v qdrant_storage:/qdrant/storage \
  -e QDRANT__SERVICE__GRPC_PORT=6334 \
  -e QDRANT__SERVICE__HTTP_PORT=6333 \
  --restart unless-stopped \
  qdrant/qdrant:latest

echo "✅ Qdrant запущен на портах: 6333 (HTTP), 6334 (gRPC)"
echo "🌐 Web UI: http://localhost:6333/dashboard"