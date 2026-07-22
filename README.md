# code-evaluation

# Docker Compose Commands

Minimal commands for running the project locally without deleting PostgreSQL data.

## First-time setup

Build the C++ sandbox images first:

```bash
docker compose --profile build-sandbox build cpp-compile-image cpp-run-image
```

This creates the local images used by the backend:

```txt
cpp-compile:latest
cpp-run:latest
```

This is usually needed only the first time, or after changing the Dockerfiles inside `cpp-compile` or `cpp-run`.

---

## Start the application

```bash
docker compose up --build
```

Builds the main application services if needed and starts them.

PostgreSQL data is kept because the database uses a named Docker volume:

```yaml
volumes:
  - pgdata:/var/lib/postgresql/data
```

---

## Start the application in the background

```bash
docker compose up --build -d
```

Same as above, but runs containers in the background.

---

## Stop services without deleting database data

```bash
docker compose down
```

Stops and removes containers and networks.

This does **not** delete the `pgdata` volume, so PostgreSQL data stays saved.

---

## Rebuild sandbox images

```bash
docker compose --profile build-sandbox build cpp-compile-image cpp-run-image
```

Run this again after changing files inside:

```txt
cpp-compile/
cpp-run/
```

---

## Build sandbox images and start everything

```bash
docker compose --profile build-sandbox up --build
```

Builds the sandbox-profile services and starts the normal application services.

Note: the sandbox services use:

```yaml
entrypoint: [ "true" ]
```

so they only build the images and then immediately exit.

---

## Check existing Docker volumes

```bash
docker volume ls
```

Use this to see existing Docker volumes.

The volume may have a project prefix, for example:

```txt
myproject_pgdata
```

---

## Commands that delete database data

Avoid these unless you intentionally want to reset the database.

```bash
docker compose down -v
```

```bash
docker compose down --volumes
```

```bash
docker system prune --volumes
```

These commands can delete the PostgreSQL volume and remove the saved database data.

---

# Dokploy deployment

Use `docker-compose.dokploy.yml` for the Dokploy Docker Compose app that runs:

- `backend`
- `sandbox-image-puller`
- `db`

Keep the frontend as a separate Dokploy Web Application.

The backend service mounts `/var/run/docker.sock` and runs as `root` in this compose file so the Docker CLI inside the backend can create sandbox containers on the Dokploy host.

## Dokploy Compose environment

Set these variables in the Dokploy Docker Compose environment UI:

```env
BACKEND_CORE_IMAGE=ghcr.io/your-org/backend-core:1.0.0
CPP_COMPILE_IMAGE=ghcr.io/your-org/cpp-compile:1.0.0
CPP_RUN_IMAGE=ghcr.io/your-org/cpp-run:1.0.0

POSTGRES_DB=appdb
POSTGRES_USER=appuser
POSTGRES_PASSWORD=change-me

MP_JWT_VERIFY_ISSUER=code-evaluation

QUARKUS_HTTP_CORS_ORIGINS=https://your-frontend-domain.com
QUARKUS_MAILER_FROM=noreply@your-domain.com
QUARKUS_MAILER_HOST=smtp.example.com
QUARKUS_MAILER_PORT=587
QUARKUS_MAILER_USERNAME=smtp-user
QUARKUS_MAILER_PASSWORD=smtp-password
QUARKUS_MAILER_START_TLS=REQUIRED

SANDBOX_REGISTRY_URL=ghcr.io
SANDBOX_REGISTRY_USERNAME=your-github-user
SANDBOX_REGISTRY_PASSWORD=github-token-with-package-read
```

Add these as Dokploy file mounts for the Compose app (inset PKCS#8 values from [this website](https://emn178.github.io/online-tools/rsa/key-generator/)):

```txt
publicKey.pem
privateKey.pem
```

The compose file mounts them into the backend container at:

```txt
/run/secrets/publicKey.pem
/run/secrets/privateKey.pem
```

## GitHub Actions secrets

For backend deploys through the Compose app, define:

```txt
DOKPLOY_COMPOSE_ID_BACKEND_CORE
```

For frontend deploys through the Web Application, define:

```txt
DOKPLOY_APP_ID_FRONTEND
```

For frontend runtime config, define these Web Application environment variables:

```env
API_URL=https://your-backend-domain.com
PLAGSCAN_REPORT_URL=https://your-backend-domain.com/api/plagscan/report
PLAGSCAN_REPORT_VIEWER_URL=https://your-plagscan-viewer-domain.com
```

The shared Dokploy secrets are still required:

```txt
DOKPLOY_URL
DOKPLOY_API_KEY
DOKPLOY_REGISTRY_USERNAME
DOKPLOY_REGISTRY_PASSWORD
CF_ACCESS_CLIENT_ID
CF_ACCESS_CLIENT_SECRET
```

Build and push the sandbox images with the same workflow by selecting `cpp-compile` and `cpp-run`.
Use `deploy=false` for those two services; they are pulled by the backend when it runs sandbox containers.

If the sandbox images are public, `SANDBOX_REGISTRY_USERNAME` and `SANDBOX_REGISTRY_PASSWORD` can be omitted.
