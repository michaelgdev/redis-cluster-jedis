package mg.demo.redistypes;

import mg.demo.component.RedisClusterManager;
import redis.clients.jedis.JedisCluster;

import java.util.*;

public class RedisMapStrIntImpl implements Map<String, Integer> {
    private final RedisClusterManager clusterManager;
    private final String redisHashKey;

    public RedisMapStrIntImpl(RedisClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        this.redisHashKey = "redis_map:" + UUID.randomUUID();
    }

    private JedisCluster getJedisCluster() {
        return clusterManager.getJedisCluster();
    }

    @Override
    public int size() {
        return (int) getJedisCluster().hlen(redisHashKey);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return getJedisCluster().hexists(redisHashKey, (String) key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (String val : getJedisCluster().hvals(redisHashKey)) {
            if (Integer.valueOf(val).equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Integer get(Object key) {
        String value = getJedisCluster().hget(redisHashKey, (String) key);
        return value != null ? Integer.valueOf(value) : null;
    }

    @Override
    public Integer put(String key, Integer value) {
        getJedisCluster().hset(redisHashKey, key, value.toString());
        return value;
    }

    @Override
    public Integer remove(Object key) {
        String value = getJedisCluster().hget(redisHashKey, (String) key);
        getJedisCluster().hdel(redisHashKey, (String) key);
        return value != null ? Integer.valueOf(value) : null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Integer> m) {
        Map<String, String> redisMap = new HashMap<>();
        for (Entry<? extends String, ? extends Integer> entry : m.entrySet()) {
            redisMap.put(entry.getKey(), entry.getValue().toString());
        }
        getJedisCluster().hmset(redisHashKey, redisMap);
    }

    @Override
    public void clear() {
        Set<String> keys = getJedisCluster().hkeys(redisHashKey);
        for (String key : keys) {
            getJedisCluster().hdel(redisHashKey, key);
        }
    }

    @Override
    public Set<String> keySet() {
        return getJedisCluster().hkeys(redisHashKey);
    }

    @Override
    public Collection<Integer> values() {
        Collection<String> redisValues = getJedisCluster().hvals(redisHashKey);
        Collection<Integer> values = new ArrayList<>();
        for (String val : redisValues) {
            values.add(Integer.valueOf(val));
        }
        return values;
    }

    @Override
    public Set<Entry<String, Integer>> entrySet() {
        Map<String, String> redisMap = getJedisCluster().hgetAll(redisHashKey);
        Set<Entry<String, Integer>> entrySet = new HashSet<>();
        for (Entry<String, String> entry : redisMap.entrySet()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(entry.getKey(), Integer.valueOf(entry.getValue())));
        }
        return entrySet;
    }

    @Override
    public String toString() {
        Map<String, String> redisMap = getJedisCluster().hgetAll(redisHashKey);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Entry<String, String> entry : redisMap.entrySet()) {
            if (sb.length() > 1) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }

}
