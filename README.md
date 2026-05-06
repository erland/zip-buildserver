# zip-buildserver

`zip-buildserver` is a planned self-hosted verification service for uploaded source-code zip packages.

The service will validate uploaded archives, detect supported project structures, run predefined build and test checks in isolated worker environments, and return concise structured verification reports for humans and assistant integrations.

## Current status

Repository skeleton initialized. Implementation will proceed step-by-step using `AGENTS.md` and `docs/agent-progress.md`.

## What this service will do

- Accept source-code zip packages.
- Validate archives safely before extraction.
- Detect Maven and Node/npm projects.
- Run administrator-controlled verification plans.
- Capture command results and concise failure summaries.
- Store full logs as controlled artifacts.

## What this service will not do

- Modify source code.
- Execute arbitrary commands from uploaded files.
- Deploy applications.
- Replace human security or production-readiness review.

## Delivery workflow

To continue implementation, ask an assistant to:

```text
Follow AGENTS.md and implement next incomplete step
```
