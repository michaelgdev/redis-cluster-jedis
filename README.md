

Start Docker in Cluster configuration with 3 Master+3 Slave 
```
cd ./docker
docker-compose up -d
```

```
docker-compose down
```
Checking The Cluster 
```
redis-cli -p 6378 cluster nodes
docker logs docker-redis-node1-1 
```