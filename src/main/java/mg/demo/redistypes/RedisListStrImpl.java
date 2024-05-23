package mg.demo.redistypes;

import mg.demo.component.RedisClusterManager;
import redis.clients.jedis.JedisCluster;

import java.util.*;

public class RedisListStrImpl implements List<String> {
    private final RedisClusterManager clusterManager;
    private final String redisListKey;

    public RedisListStrImpl(RedisClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        this.redisListKey = "redis_list:" + UUID.randomUUID();
    }

    private JedisCluster getJedisCluster() {
        return clusterManager.getJedisCluster();
    }

    @Override
    public int size() {
        return (int) getJedisCluster().llen(redisListKey);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return getJedisCluster().lrange(redisListKey, 0, -1).iterator();
    }

    @Override
    public Object[] toArray() {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.toArray(a);
    }

    @Override
    public boolean add(String s) {
        getJedisCluster().rpush(redisListKey, s);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        long count = getJedisCluster().lrem(redisListKey, 1, (String) o);
        return count > 0;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        for (String s : c) {
            add(s);
        }
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        // Adding elements at a specific index is not directly supported by Redis.
        // We need to implement a custom logic for this.
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        list.addAll(index, c);
        getJedisCluster().del(redisListKey);
        getJedisCluster().rpush(redisListKey, list.toArray(new String[0]));
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        for (Object o : c) {
            modified |= remove(o);
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        boolean modified = list.retainAll(c);
        if (modified) {
            getJedisCluster().del(redisListKey);
            getJedisCluster().rpush(redisListKey, list.toArray(new String[0]));
        }
        return modified;
    }

    @Override
    public void clear() {
        getJedisCluster().del(redisListKey);
    }

    @Override
    public String get(int index) {
        return getJedisCluster().lindex(redisListKey, index);
    }

    @Override
    public String set(int index, String element) {
        String oldValue = get(index);
        getJedisCluster().lset(redisListKey, index, element);
        return oldValue;
    }

    @Override
    public void add(int index, String element) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        list.add(index, element);
        getJedisCluster().del(redisListKey);
        getJedisCluster().rpush(redisListKey, list.toArray(new String[0]));
    }

    @Override
    public String remove(int index) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        String removed = list.remove(index);
        getJedisCluster().del(redisListKey);
        getJedisCluster().rpush(redisListKey, list.toArray(new String[0]));
        return removed;
    }

    @Override
    public int indexOf(Object o) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.lastIndexOf(o);
    }

    @Override
    public ListIterator<String> listIterator() {
        return getJedisCluster().lrange(redisListKey, 0, -1).listIterator();
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        return getJedisCluster().lrange(redisListKey, 0, -1).listIterator(index);
    }

    @Override
    public List<String> subList(int fromIndex, int toIndex) {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.subList(fromIndex, toIndex);
    }

    @Override
    public String toString() {
        List<String> list = getJedisCluster().lrange(redisListKey, 0, -1);
        return list.toString();
    }

}
