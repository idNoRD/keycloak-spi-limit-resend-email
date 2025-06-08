#!/bin/zsh

KC_DIR="/opt/keycloak"

docker exec keycloak sh -c "mkdir -p ${KC_DIR}/providers"

# Copy your provider jar(s) into providers folder inside container
docker cp target/idnord*.jar "keycloak:${KC_DIR}/providers/"

# Run the build command inside container to include the new provider(s)
docker exec keycloak sh -c "${KC_DIR}/bin/kc.sh build"

# Restart the keycloak container to apply changes
docker restart keycloak

