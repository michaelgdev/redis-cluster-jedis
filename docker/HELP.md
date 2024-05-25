## Start Redis Cluster configuration in Docker (3 Master+3 Slave)

# Step 1. Determine the IP Address of the Docker Host Machine
Put YOUR found address in `docker-compose.yml` and `HELP.md`.
## Execute the Script
```Bash
./update-ip.sh
```
Your IP:
```Bash
ifconfig | grep -o 'inet \([0-9]\{1,3\}\.\)\{3\}[0-9]\{1,3\}' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1
```

# Step 2. Start Docker Containers
Start:
```Bash
docker-compose up -d
```
Stop:
```Bash
docker-compose down -v
```
# Step 3. Creating a Redis Cluster
Create a cluster (3 masters + 3 slaves):
```Bash
redis-cli --cluster create 192.168.10.110:6379 192.168.10.110:6380 192.168.10.110:6381 192.168.10.110:6382 192.168.10.110:6383 192.168.10.110:6384 --cluster-replicas 1 --cluster-yes
```

# Step 4. Add New Nodes to the Cluster (Run the Application Before This Step)
Adding new nodes to the Cluster (Future Master + Slave):
```Bash
redis-cli cluster meet 192.168.10.110 6385
redis-cli cluster meet 192.168.10.110 6386
```

# Step 5. Make the New Node a Slave
Make a node as a Slave:
```Bash
redis-cli -p 6386 cluster replicate <master-node-id>
```

# Step 6. Rebalance Slots Across All Nodes
```Bash
redis-cli --cluster rebalance 192.168.10.110:6379 --cluster-use-empty-masters
```

# Step 7. Remove a node from Cluster
Remove a Node from the Cluster:
```Bash
redis-cli cluster forget <remove-node-id>
```

Get the state of the Cluster:
```Bash
redis-cli CLUSTER NODES
```
```Bash
redis-cli CLUSTER INFO
```

Logs:
```Bash
docker logs docker-redis-1-1
```

### DATA MANIPULATION
Read Keys:
```Bash
redis-cli -p 6379 KEYS "*"
redis-cli -p 6380 KEYS "*"
redis-cli -p 6381 KEYS "*"
redis-cli -p 6380 KEYS "redis_map:*"
redis-cli -p 6380 KEYS "redis_list:*"
```
