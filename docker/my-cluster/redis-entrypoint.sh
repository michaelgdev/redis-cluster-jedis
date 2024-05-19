#!/bin/sh

# Start Redis server with the provided configuration
redis-server /usr/local/etc/redis/redis.conf &

# Wait for the Redis server's to start
sleep 10

# Function to check if Redis is up
check_redis_up() {
  while ! redis-cli -h 127.0.0.1 -p "$1" ping; do
    echo "Waiting for Redis server on port $1 to start..."
    sleep 1
  done
}

# Check if the Redis server is up
check_redis_up "$(grep 'port' /usr/local/etc/redis/redis.conf | awk '{print $2}')"

# Print the hostname for debugging
echo "Current hostname: $(hostname)"

# Only the first node will create the cluster
if [ "$(hostname)" = "redis-node1ttttttt" ]; then
  echo "Creating Redis cluster..."

  yes yes | redis-cli --cluster create \
    redis-node1:6379 \
    redis-node2:6380 \
    redis-node3:6381 \
    redis-node4:6382 \
    redis-node5:6383 \
    redis-node6:6384 \
    --cluster-replicas 1

  if [ $? -eq 0 ]; then
    echo "Cluster creation command executed successfully"
  else
    echo "Cluster creation command failed"
  fi
else
  echo "Cluster creation skipped for $(hostname)"
fi

# Keep the container running
tail -f /dev/null
