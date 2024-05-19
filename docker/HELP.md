## Start Redis Cluster configuration in Docker (3 Master+3 Slave)

# Step 1. Determine IP address of Docker Host machine
Put found address in docker-compose.yml in the param 'cluster-announce-ip' and for the Step 3
```Bash
ifconfig | grep -o 'inet \([0-9]\{1,3\}\.\)\{3\}[0-9]\{1,3\}' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1
```
# Step 2. Start Docker Containers
```Bash
docker-compose up -d
```

```Bash
docker-compose down
```
# Step 3. Creating a Redis Cluster
```Bash
redis-cli --cluster create \
192.168.10.110:6379 \
192.168.10.110:6380 \
192.168.10.110:6381 \
192.168.10.110:6382 \
192.168.10.110:6383 \
192.168.10.110:6384 \
--cluster-replicas 1 --cluster-yes
```

Checking The Cluster
```Bash
redis-cli -h localhost -p 6379 cluster nodes
```
Logs
```Bash
docker logs docker-redis-1-1
```