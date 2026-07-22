#!/bin/sh
set -eu

: "${API_URL:=}"
: "${PLAGSCAN_REPORT_URL:=}"
: "${PLAGSCAN_REPORT_VIEWER_URL:=}"

envsubst < /usr/share/nginx/html/config.template.json > /usr/share/nginx/html/config.json

exec nginx -g 'daemon off;'
