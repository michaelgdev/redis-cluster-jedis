package mg.demo.redistypes;


import mg.demo.component.RedisClusterManager;
import redis.clients.jedis.JedisCluster;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class RedisMap implements Map<String, Integer> {
    private RedisClusterManager clusterManager;

    public RedisMap(RedisClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    private JedisCluster getJedisCluster() {
        return clusterManager.getJedisCluster();
    }

    @Override
    public int size() {
        return (int) getJedisCluster().dbSize();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return getJedisCluster().exists((String) key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("containsValue is not supported");
    }

    @Override
    public Integer get(Object key) {
        String value = getJedisCluster().get((String) key);
        return value != null ? Integer.valueOf(value) : null;
    }

    @Override
    public Integer put(String key, Integer value) {
        getJedisCluster().set(key, value.toString());
        return value;
    }

    @Override
    public Integer remove(Object key) {
        return (int) getJedisCluster().del((String) key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Integer> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        getJedisCluster().flushDB();
    }

    @Override
    public Set<String> keySet() {
        return getJedisCluster().keys("*");
    }

    @Override
    public Collection<Integer> values() {
        throw new UnsupportedOperationException("values is not supported");
    }

    @Override
    public Set<Entry<String, Integer>> entrySet() {
        throw new UnsupportedOperationException("entrySet is not supported");
    }
}
