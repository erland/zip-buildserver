# API Overview

The backend exposes REST endpoints under `/api` and generates an OpenAPI document through SmallRye OpenAPI at:

```text
GET /q/openapi
```

The OpenAPI output is intended to be usable as the basis for a Custom GPT Action or another assistant integration. Authentication is added in a later delivery step, so the endpoints below are not yet protected.

## Human and UI endpoints

### Sessions

```text
POST /api/sessions
GET  /api/sessions
GET  /api/sessions/{sessionId}
POST /api/sessions/{sessionId}/close
```

### Package upload

```text
POST /api/sessions/{sessionId}/packages
GET  /api/packages/{packageId}
```

Package upload currently uses `multipart/form-data` with a `file` part containing the zip archive.

### Verification runs

```text
POST /api/sessions/{sessionId}/runs
GET  /api/sessions/{sessionId}/runs
GET  /api/runs/{runId}
GET  /api/runs/{runId}/summary
```

### Artifacts

```text
GET /api/runs/{runId}/artifacts
GET /api/artifacts/{artifactId}
```

Default run summaries contain concise log excerpts. Full logs are exposed through artifact references.

### Verification plans

```text
GET /api/verification-plans
```

Verification plans are loaded from server-side configuration under `backend/src/main/resources/verification-plans/`.

## Assistant-friendly endpoints

Assistant endpoints return compact JSON structures so an assistant can consume run state and failure details without reading full logs.

### Create assistant verification session

```text
POST /api/assistant/verification-sessions
Content-Type: application/json
```

Request:

```json
{
  "label": "optional task label",
  "retentionPolicy": "default"
}
```

Response:

```json
{
  "sessionId": "uuid",
  "status": "OPEN",
  "label": "optional task label",
  "retentionPolicy": "default",
  "createdAt": "2026-05-06T12:00:00Z"
}
```

### Create assistant verification run

```text
POST /api/assistant/verification-sessions/{sessionId}/runs
Content-Type: application/json
```

Request:

```json
{
  "packageId": "uuid",
  "requestedPlanId": "node-default"
}
```

`requestedPlanId` is optional. If provided, it must refer to a configured server-side verification plan compatible with the detected package.

Response:

```json
{
  "runId": "uuid",
  "sessionId": "uuid",
  "packageId": "uuid",
  "status": "PASSED",
  "summary": "Verification passed.",
  "planId": "node-default",
  "structuredSummary": {
    "runId": "uuid",
    "status": "PASSED",
    "summary": "Verification passed.",
    "primaryFailure": null,
    "failedFiles": [],
    "failedTests": [],
    "commandsRun": ["Install dependencies", "Run tests", "Build"],
    "failedChecks": [],
    "suggestedFocus": ["All fake verification commands completed successfully."],
    "fullLogReference": "/api/runs/{runId}/artifacts",
    "partial": false
  }
}
```

### Read assistant summary

```text
GET /api/assistant/verification-runs/{runId}/summary
```

The response omits full logs and returns command labels, failure details, suggested focus areas, and an artifact-list reference.

### Read failed log excerpts

```text
GET /api/assistant/verification-runs/{runId}/failed-log-excerpts
```

The response includes only failed, timed-out, or internal-error command excerpts.

## OpenAPI usage

Start the backend and fetch the OpenAPI schema:

```bash
cd backend
mvn quarkus:dev
curl http://localhost:8080/q/openapi
```

For Custom GPT Action use, prefer the `/api/assistant/*` endpoints because they are intentionally compact and avoid returning full logs by default.
