## Start Redis Cluster configuration in Docker (3 Master+3 Slave)

# Step 1. Set your IP Address in all scripts
Put YOUR found address in `docker-compose.yml` and `HELP.md`.
## Execute the Script that do it automatically
```bash
./update-ip.sh
```
Your IP:
```bash
ifconfig | grep -o 'inet \([0-9]\{1,3\}\.\)\{3\}[0-9]\{1,3\}' | grep -v '127.0.0.1' | awk '{print $2}' | head -n 1
```

# Step 2. Start Docker Containers
Start:
```bash
docker-compose up -d
```
Stop:
```bash
docker-compose down -v
```
# Step 3. Creating a Redis Cluster
Create a cluster (3 masters + 3 slaves):
```bash
redis-cli --cluster create 192.168.10.110:6379 192.168.10.110:6380 192.168.10.110:6381 192.168.10.110:6382 192.168.10.110:6383 192.168.10.110:6384 --cluster-replicas 1 --cluster-yes
```

# Step 4. Add New Nodes to the Cluster (Run the Application Before This Step)
Adding new nodes to the Cluster (Future Master + Slave):
```bash
redis-cli cluster meet 192.168.10.110 6385
redis-cli cluster meet 192.168.10.110 6386
```

# Step 5. Make the New Node a Slave
Make a node as a Slave:
```bash
redis-cli -p 6386 cluster replicate <master-node-id>
```

# Step 6. Rebalance Slots Across All Nodes
```bash
redis-cli --cluster rebalance 192.168.10.110:6379 --cluster-use-empty-masters
```

# Step 7. Remove a node from Cluster
Remove a Node from the Cluster:
```bash
redis-cli cluster forget <remove-node-id>
```

**Get the state of the Cluster:**
```bash
redis-cli CLUSTER NODES
```
```bash
redis-cli CLUSTER INFO
```

**Logs:**
```bash
docker logs docker-redis-1-1
```


# Redis Data Manipulation Examples
**Read Keys**
```bash
redis-cli -p 6379 KEYS "*"
redis-cli -p 6380 KEYS "*"
redis-cli -p 6381 KEYS "*"
```
**Remove all data from all nodes**
```bash
redis-cli -p 6379 FLUSHALL
redis-cli -p 6380 FLUSHALL
redis-cli -p 6381 FLUSHALL
```
**Read specific Keys**
```bash
redis-cli -p 6380 KEYS "redis_map:*"
redis-cli -p 6380 KEYS "redis_list:*"
```

## String Operations
#### Redis Strings are the simplest type of value you can associate with a key. They are binary-safe, meaning they can contain any data, such as text or binary data (like images or serialized objects).
**Write String:**
```bash
redis-cli -c -p 6379 SET key_str1 "value key_str1" # Set the value of key_str1 to "value key_str1"
redis-cli -c -p 6379 SET key_str2 "value key_str2" # Set the value of key_str2 to "value key_str2"
redis-cli -c -p 6379 SETEX key_str3 60 "value key_str2" # Set the value of key_str3 to "value key_str2" and expire it after 60 seconds
redis-cli -c -p 6379 SETNX key_str4 "value key_str4" # Set the value of key_str4 to "value key_str4" only if key_str4 does not exist
redis-cli -c -p 6379 APPEND key_str1 " appended" # Append " appended" to the value of key_str1
redis-cli -c -p 6379 SETRANGE key_str1 6 "here set range" # Overwrite part of the value of key_str1 starting at offset 6 with "here set range"
```
**Read String:**
```bash
redis-cli -c -p 6379 GET key_str1 # Get the value of key_str1
redis-cli -c -p 6379 GET key_str2 # Get the value of key_str2
redis-cli -c -p 6379 STRLEN key_str1 # Get the length of the value stored in key_str1
redis-cli -c -p 6379 EXISTS key_str1 # Check if key_str1 exists
redis-cli -c -p 6379 GETRANGE key_str1 5 9 # Get a substring of the value of key_str1 from index 5 to 9
```
## Hash Operations
#### Redis Hashes are maps between string fields and string values, making them ideal for representing objects (e.g., a user with various attributes). They store multiple field-value pairs under a single key and are memory-efficient for small fields and values. Atomic operations ensure that concurrent changes are handled safely.
**Write Hash:**
```bash
redis-cli -c -p 6379 HSET hash1 field1 "hash field1" # Set field1 in hash1 to "hash field1"
redis-cli -c -p 6379 HSET hash1 field2 "hash field2" # Set field2 in hash1 to "hash field2"
redis-cli -c -p 6379 HSET hash1 field3 "hash field3" # Set field3 in hash1 to "hash field3"
redis-cli -c -p 6379 HDEL hash1 field2 # Delete field2 from hash1
redis-cli -c -p 6379 HMSET hash1 field4 "hash field4" field5 "hash field5" field6 "hash field6" # Set multiple fields in hash1
```
**Read Hash:**
```bash
redis-cli -c -p 6379 HGET hash1 field1 # Get the value of field1 in hash1
redis-cli -c -p 6379 HGET hash1 field2 # Get the value of field2 in hash1
redis-cli -c -p 6379 HGET hash1 field3 # Get the value of field3 in hash1
redis-cli -c -p 6379 HGETALL hash1 # Get all fields and values in hash1
redis-cli -c -p 6379 HLEN hash1 # Get the number of fields in hash1
redis-cli -c -p 6379 HEXISTS hash1 field1 # Check if field1 exists in hash1
redis-cli -c -p 6379 HKEYS hash1 # Get all the fields in hash1
redis-cli -c -p 6379 HVALS hash1 # Get all the values in hash1
```
### List Operations
#### Redis Lists are ordered collections of strings. They are one of the basic data types supported by Redis, and they are particularly useful for managing sequences of elements.
**Write List:**
```bash
redis-cli -c -p 6379 LPUSH list1 "item1" # Insert "item1" at the head of the list
redis-cli -c -p 6379 LPUSH list1 "item2" # Insert "item2" at the head of the list
redis-cli -c -p 6379 LPUSH list1 "item3" # Insert "item3" at the head of the list
redis-cli -c -p 6379 RPUSH list1 "item4" # Insert "item4" at the tail of the list
redis-cli -c -p 6379 RPUSH list1 "item5" # Insert "item5" at the tail of the list
redis-cli -c -p 6379 LINSERT list1 BEFORE "item2" "item1.5" # Insert "item1.5" before "item2" in the list
redis-cli -c -p 6379 LINSERT list1 AFTER "item3" "item3.5" # Insert "item3.5" after "item3" in the list
redis-cli -c -p 6379 LSET list1 0 "new_item1" # Set the value at index 0 of the list to "new_item1"
redis-cli -c -p 6379 LREM list1 2 "item2" # Removes the first 2 occurrences of "item2"
redis-cli -c -p 6379 LTRIM list1 1 -1 # Trims the list to only keep elements from index 1 to the end
```
**Read List:**
```bash
redis-cli -c -p 6379 LRANGE list1 0 -1 # Get all elements in the list from start to end
redis-cli -c -p 6379 LINDEX list1 0 # Get the element at index 0 in the list
redis-cli -c -p 6379 LLEN list1 # Get the length of the list
redis-cli -c -p 6379 LPOP list1 # Remove and get the first element in the list
redis-cli -c -p 6379 RPOP list1 # Remove and get the last element in the list
redis-cli -c -p 6379 BLPOP list1 5 # Remove and get the first element in the list, blocking for 5 seconds if the list is empty
redis-cli -c -p 6379 BRPOP list1 5 # Remove and get the last element in the list, blocking for 5 seconds if the list is empty
```

## Set Operations
#### Redis Sets are unordered collections of unique strings. They provide efficient operations to add, remove, and check for the existence of members.
**Write Set:**
```bash
redis-cli -c -p 6379 SADD set1 "set member1" # Add 'set member1' to set1
redis-cli -c -p 6379 SADD set1 "set member2" # Add 'set member2' to set1
redis-cli -c -p 6379 SADD set1 "set member3" # Add 'set member3' to set1
redis-cli -c -p 6379 SREM set1 "set member2" # Remove 'set member2' from set1
```

**Read Set:**
```bash
redis-cli -c -p 6379 SMEMBERS set1 # Get all members in set1
redis-cli -c -p 6379 SISMEMBER set1 "set member1" # Check if 'set member1' is in set1
redis-cli -c -p 6379 SCARD set1 # Get the number of members in set1
```

## Sorted Set Operations
#### Redis Sorted Sets are similar to Sets, but every member is associated with a score. Members are ordered by their scores
**Write Sorted Set:**
```bash
redis-cli -c -p 6379 ZADD zset1 1 "zset member1" # Add member "zset member1" with score 1 to zset1
redis-cli -c -p 6379 ZADD zset1 2 "zset member2" # Add member "zset member2" with score 2 to zset1
redis-cli -c -p 6379 ZADD zset1 3 "zset member3" # Add member "zset member3" with score 3 to zset1
redis-cli -c -p 6379 ZREM zset1 "zset member2" # Remove member "zset member2" from zset1
redis-cli -c -p 6379 ZADD zset1 80 "zset member1" # Add member "zset member1" with score 80 to zset1
```

**Read Sorted Set:**
```bash
redis-cli -c -p 6379 ZRANGE zset1 0 -1 WITHSCORES # Get all members in zset1 with their scores
redis-cli -c -p 6379 ZSCORE zset1 "zset member1" # Get the score of member "zset member1" in zset1
redis-cli -c -p 6379 ZCARD zset1 # Get the number of members in zset1
```
