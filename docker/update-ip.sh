#!/bin/bash

# Path to the Docker Compose file
COMPOSE_FILE="docker-compose.yml"

# Extract the current IP address, excluding localhost
CURRENT_IP=$(ifconfig | grep -o 'inet \([0-9]\{1,3\}\.\)\{3\}[0-9]\{1,3\}' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1)

if [[ -z "$CURRENT_IP" ]]; then
    echo "No IP address found, exiting..."
    exit 1
fi

echo "Found IP: $CURRENT_IP"

# Use sed to replace the IP address in the Docker Compose file
# For macOS, use '' with -i to avoid creating a backup file
sed -i '' "s/--cluster-announce-ip '[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}\.[0-9]\{1,3\}'/--cluster-announce-ip '$CURRENT_IP'/g" $COMPOSE_FILE

echo "Updated Docker Compose file with IP: $CURRENT_IP"
