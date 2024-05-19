

Start Docker in Cluster configuration with 3 Master+3 Slave 
```
cd ./docker
docker-compose up -d
```
docker network create redis-cluster
docker network inspect redis-cluster

```
docker-compose down
```
```
docker exec -it redis-1 redis-cli --cluster create \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-1):6379 \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-2):6380 \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-3):6381 \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-4):6382 \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-5):6383 \
$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' redis-6):6384 \
--cluster-replicas 1
```
Checking The Cluster
```
redis-cli -p 6378 cluster nodes
```
```
docker logs docker-redis-node1-1
```
```
nc -zv localhost 6378 
```