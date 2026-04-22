#!/bin/bash

# Colori per output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Funzione per controllare se il comando precedente ha avuto successo
check_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Success${NC}"
    else
        echo -e "${RED}✗ Failed${NC}"
        exit 1
    fi
}

echo "Installing eulero dependency..."
mvn clean install:install-file \
  -Dfile=libs/eulero-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  -DgroupId=org.oris-tool \
  -DartifactId=eulero \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackaging=jar
check_status

echo "Installing eulero-queueing dependency..."
mvn clean install:install-file \
  -Dfile=libs/eulero-queueing-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -DgroupId=org.oris-tool \
  -DartifactId=eulero-queueing \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar
check_status


echo "Installing toad dependency..."
mvn clean install:install-file \
  -Dfile=libs/rospo-1.0-SNAPSHOT.jar \
  -DgroupId=org.example \
  -DartifactId=rospo \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar
check_status

echo "Building and installing main project..."
mvn clean install
check_status

echo -e "${GREEN}All installations completed successfully!${NC}"