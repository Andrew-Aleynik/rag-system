#!/bin/bash

# Загрузка переменных из .env файла
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
fi

# Использование переменной из .env или значение по умолчанию
OLLAMA_MODEL=${OLLAMA_EMBED_MODEL:-nomic-embed-text}

# Запуск контейнера Ollama
docker run -d \
  --name ragsystem-ollama \
  -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  -e OLLAMA_HOST=0.0.0.0 \
  -e OLLAMA_ORIGINS="*" \
  --restart unless-stopped \
  ollama/ollama:latest

echo "✅ Ollama запущен на порту: 11434"
echo "🔗 API: http://localhost:11434"

# Ждём запуска Ollama
sleep 5

# Скачивание модели
echo "📥 Скачивание модели ${OLLAMA_MODEL}..."
docker exec ragsystem-ollama ollama pull ${OLLAMA_MODEL}

echo "✅ Модель ${OLLAMA_MODEL} готова к использованию"