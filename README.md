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
