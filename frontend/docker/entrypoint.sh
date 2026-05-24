#!/bin/sh
set -eu

: "${API_URL:=http://localhost:8080}"

envsubst < /usr/share/nginx/html/config.template.json > /usr/share/nginx/html/config.json

exec nginx -g 'daemon off;'
