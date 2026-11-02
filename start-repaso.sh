#!/bin/sh
# Starts Repaso from the project folder (so ./data is found), then opens the lists page.
cd "$(dirname "$0")" || exit 1
(sleep 8 && xdg-open http://localhost:8080/lists >/dev/null 2>&1) &
exec java -jar target/repasogr-0.0.1-SNAPSHOT.jar
