package mg.demo.component;

import redis.clients.jedis.JedisCluster;

public interface RedisClusterManager2 {
    JedisCluster getJedisCluster();

    void updateClusterNodes();

    void addNode(String nodeIp, int nodePort);

    boolean createCluster();

    boolean addNewNodeToCluster(String newNodeIp, int newNodePort);

    void rebalanceCluster();

    boolean removeNodeFromCluster(String removeNodeIp, int removeNodePort);
}
