# RAG System

Spring Boot сервис для **RAG по коду**: индексирует Git-репозитории и коллекции документов, ищет релевантные фрагменты и подмешивает их в запросы к LLM.

Подходит для кодогенерации, автодополнения и анализа кодовой базы с опорой на ваш реальный контекст.

## Преимущества

- **AST-chunking** — код режется по структуре (классы, методы), а не только по размеру окна
- **Git-синхронизация** — клонирование и обновление репозиториев через JGit, инкрементальная индексация по хешам файлов
- **Гибкий scope** — проекты и коллекции можно активировать/деактивировать независимо
- **Контекстный retrieve** — рядом с найденными чанками подтягиваются соседние и структурные фрагменты
- **Два режима embeddings** — OpenAI-совместимый API или локальный Ollama
- **LLM proxy** — прозрачная подстановка RAG-контекста в chat-completions запросы
- **MCP-сервер** — tool `retrieve` для Cursor и других MCP-клиентов
- **Admin UI + OpenAPI** — панель управления и Swagger из коробки

## Стек

Java 17+ · Spring Boot 3 · Spring AI · MCP · PostgreSQL · Qdrant · Ollama / OpenAI

## Запуск

### 1. Инфраструктура

```bash
./scripts/start-postgres.sh
./scripts/start-qdrant.sh
# опционально, если EMBED_PROVIDER=ollama:
./scripts/start-ollama.sh
```

### 2. Конфигурация

Задайте переменные окружения (или `.env`):

| Переменная | Описание | По умолчанию |
|---|---|---|
| `EMBED_PROVIDER` | `openai` или `ollama` | `openai` |
| `OPENAI_API_KEY` | ключ API | — |
| `OPENAI_BASE_URL` | base URL провайдера | — |
| `OPENAI_EMBED_MODEL` | модель эмбеддингов | — |
| `OLLAMA_URL` | URL Ollama | `http://localhost:11434` |
| `OLLAMA_EMBED_MODEL` | модель Ollama | `nomic-embed-text` |
| `QDRANT_HOST` / `QDRANT_PORT` | Qdrant gRPC | `localhost` / `6334` |
| `POSTGRESQL_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/ragsystem` |
| `DB_USERNAME` / `DB_PASSWORD` | БД | `postgres` / `postgres` |

### 3. Приложение

```bash
./mvnw spring-boot:run
```

- Admin UI: http://localhost:8080/
- Swagger: http://localhost:8080/swagger-ui.html
- API: http://localhost:8080/api/v1/
- MCP: http://localhost:8080/mcp

## Как пользоваться

### Типичный сценарий с Git-проектом

1. **Создать проект**
   ```http
   POST /api/v1/projects
   { "name": "my-repo", "url": "https://github.com/org/repo.git", "defaultBranch": "main", "sourceType": "GIT" }
   ```
2. **Синхронизировать** файлы: `POST /api/v1/projects/{id}/sync`
3. **Проиндексировать**: `POST /api/v1/projects/{id}/index`
4. **Активировать** для поиска: `GET /api/v1/projects/{id}/activate`
5. **Искать** релевантные чанки:
   ```http
   POST /api/v1/retrieve
   { "query": "How does authentication work?", "fileExtensions": ["java"] }
   ```

Статус долгих задач (sync / index): `GET /api/v1/tasks`.

### Коллекции

Произвольный набор документов из уже синхронизированных проектов:

```http
POST /api/v1/collections          → создать
POST /api/v1/collections/{id}/documents   → добавить documentIds
POST /api/v1/collections/{id}/index
GET  /api/v1/collections/{id}/activate
```

### MCP (Model Context Protocol)

Сервер отдаёт Streamable HTTP MCP на `/mcp` с одним tool — `retrieve`.

| Параметр | Обязательный | Описание |
|---|---|---|
| `query` | да | поисковый запрос (текст или код) |
| `fileExtensions` | нет | фильтр расширений, например `["java", "kt"]` |

Ответ — те же чанки, что и у `POST /api/v1/retrieve` (карта `localPath → chunks`).

Подключение в Cursor (`mcp.json` / настройки MCP):

```json
{
  "mcpServers": {
    "rag-system": {
      "url": "http://localhost:8080/mcp"
    }
  }
}
```

Перед использованием через MCP проект или коллекция должны быть проиндексированы и **активированы**.

### Proxy к LLM с RAG

Любой chat-completions запрос можно прогнать через прокси — в последнее user-сообщение автоматически добавится найденный контекст:

```http
POST /api/v1/proxy/{host}/{path}
Header: X-Proxy-Protocol: https   # http | https, по умолчанию https
```

Примеры:
- `POST /api/v1/proxy/api.openai.com/v1/chat/completions`
- `POST /api/v1/proxy/localhost:11434/v1/chat/completions` + `X-Proxy-Protocol: http`

Заголовки (`Authorization` и др.) пробрасываются на upstream; ответ стримится обратно клиенту.

### Admin UI

На главной странице (`/`) можно создавать проекты и коллекции, запускать sync/index, включать/выключать источники и смотреть документы без ручных curl-запросов.
