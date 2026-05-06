# Scripts

Local development helpers:

```bash
./scripts/dev-up.sh
./scripts/dev-down.sh
./scripts/build-worker-image.sh
```

`dev-up.sh` starts the Docker Compose development stack.

`dev-down.sh` stops it and removes named volumes for a clean local reset.

`build-worker-image.sh` builds the local worker image used by future Docker-based verification execution:

```bash
ZIP_BUILDSERVER_WORKER_IMAGE=zip-buildserver-worker-node-maven:local ./scripts/build-worker-image.sh
```
