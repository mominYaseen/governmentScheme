# OpenAPI / Swagger for frontend development

## URLs (local)

| Resource | URL |
|----------|-----|
| **Swagger UI** | `http://localhost:8080/swagger-ui/index.html` |
| **OpenAPI JSON (all groups)** | `http://localhost:8080/v3/api-docs` |
| **Group: all** | `http://localhost:8080/v3/api-docs/all` |
| **Group: catalog** (list + recommend, no AI) | `http://localhost:8080/v3/api-docs/catalog` |
| **Group: ai** (`/match` only) | `http://localhost:8080/v3/api-docs/ai` |

Use the **group** URLs with OpenAPI Generator (`openapi-generator-cli`) to generate TypeScript clients scoped to catalog vs AI flows.

## Client generation (example)

```bash
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs/catalog \
  -g typescript-axios \
  -o ./src/api/catalog
```

## Environment

Set `openapi.public-url` in `application.properties` (or env) to add a second **Server** entry in the spec (e.g. your deployed API base URL).

## Response shape

Almost every endpoint returns:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-03-28T12:00:00Z"
}
```

Typed payloads are always under `data`. See operation `operationId` values in Swagger UI for stable method names in generated clients.
