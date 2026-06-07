#!/bin/sh
set -e

DATA_DIR="/app/src/main/resources"

# On first run the named volume is empty — seed it from the image defaults
if [ ! -f "$DATA_DIR/civ6_leaders.json" ]; then
  echo "First run: initialising data directory from image defaults..."
  mkdir -p "$DATA_DIR"
  cp -r /app/resources-default/. "$DATA_DIR/"
  echo "Data directory initialised."
fi

exec java -jar /app/app.jar
