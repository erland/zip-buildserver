# Operations

This document describes the current local operation model for `zip-buildserver`.

## Docker Compose

Start the local stack with:

```bash
docker compose up --build
```

Stop and remove local containers with:

```bash
docker compose down -v
```

The Compose stack starts:

- `zip-buildserver-api`
- `zip-buildserver-web`
- `postgres`

## Static API token authentication

API authentication is enabled by default.

Set a static bearer token before exposing the service:

```bash
ZIP_BUILDSERVER_AUTH_ENABLED=true
ZIP_BUILDSERVER_API_TOKEN=replace-with-a-long-random-token
```

Requests to protected API endpoints must include:

```text
Authorization: Bearer <ZIP_BUILDSERVER_API_TOKEN>
```

Example:

```bash
curl -i http://localhost:8080/api/sessions

curl -i \
  -H "Authorization: Bearer $ZIP_BUILDSERVER_API_TOKEN" \
  http://localhost:8080/api/sessions
```

Expected behavior:

- Requests without a valid bearer token receive `401 unauthorized`.
- Requests with the configured token are allowed.
- `/api/health` remains public so container health checks can run.
- `/q/openapi` remains public for local OpenAPI inspection.

For private development only, authentication can be disabled:

```bash
ZIP_BUILDSERVER_AUTH_ENABLED=false
```

Do not disable authentication on a network-exposed deployment.

## Storage paths

The backend uses these storage settings:

```bash
ZIP_BUILDSERVER_DATA_DIR=/data/zip-buildserver
ZIP_BUILDSERVER_PACKAGES_DIR=/data/zip-buildserver/packages
ZIP_BUILDSERVER_WORKSPACES_DIR=/data/zip-buildserver/workspaces
ZIP_BUILDSERVER_ARTIFACTS_DIR=/data/zip-buildserver/artifacts
```

Uploaded packages, extracted workspaces, and verification artifacts should be treated as confidential user content.

## Worker execution

Use fake execution for deterministic local API/UI development:

```bash
ZIP_BUILDSERVER_WORKER_EXECUTOR=fake
```

Use Docker execution for real verification runs:

```bash
./scripts/build-worker-image.sh
ZIP_BUILDSERVER_WORKER_EXECUTOR=docker docker compose up --build
```

Docker-based execution is a trusted self-hosted MVP mode. Mounting or otherwise granting Docker control to the API service is powerful and should be isolated to a dedicated host.

## Local backend verification

Run backend tests locally with:

```bash
cd backend
mvn test
```

Run frontend tests and build locally with:

```bash
cd frontend
npm install
npm test
npm run build
```

## Retention and cleanup

Artifact and package retention configuration exists, but scheduled retention cleanup is implemented in a later delivery step.

## Retention cleanup

The backend includes scheduled retention cleanup for retained packages, artifacts, workspaces, and old closed-session metadata.

Default retention settings:

```bash
ZIP_BUILDSERVER_RETENTION_CLEANUP_ENABLED=true
ZIP_BUILDSERVER_RETENTION_CLEANUP_INTERVAL=24h
ZIP_BUILDSERVER_PACKAGE_RETENTION_DAYS=7
ZIP_BUILDSERVER_ARTIFACT_RETENTION_DAYS=14
ZIP_BUILDSERVER_SESSION_RETENTION_DAYS=90
ZIP_BUILDSERVER_WORKSPACE_CLEANUP_GRACE_MINUTES=60
```

Cleanup behavior:

- Expired uploaded package files are deleted from storage while package metadata is retained until session metadata expires.
- Expired artifact files and their artifact references are removed.
- Old workspace directories are removed after the workspace cleanup grace period.
- Closed sessions older than the session retention period are deleted with their associated metadata according to database cascade rules.
- Cleanup events are recorded as system audit events when any retained data is removed.

For local troubleshooting, disable the schedule with:

```bash
ZIP_BUILDSERVER_RETENTION_CLEANUP_ENABLED=false
```
