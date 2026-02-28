FROM debian:bookworm-slim

RUN apt-get update \
 && apt-get install -y --no-install-recommends g++ ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# non-root user
RUN useradd -m -u 10001 runner
USER 10001:10001

WORKDIR /work
ENTRYPOINT ["/bin/bash", "-lc"]