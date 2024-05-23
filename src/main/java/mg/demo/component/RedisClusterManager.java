package mg.demo.component;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RedisClusterManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterManager.class);
    private JedisCluster jedisCluster;

    public RedisClusterManager() {
    }

    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    public void ConnectCluster(Set<HostAndPort> initialClusterNodes) {
        try {
            // Configure the connection pool
            GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(128);

            // Create JedisClientConfig with necessary configurations
            DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                    .connectionTimeoutMillis(2000) // 2 seconds
                    .socketTimeoutMillis(2000) // 2 seconds
                    .build();

            int maxAttempts = 1000; // Number of attempts
            this.jedisCluster = new JedisCluster(initialClusterNodes, clientConfig, maxAttempts, poolConfig);
            logger.info("Connected to Redis cluster with nodes: {}", initialClusterNodes);
        } catch (Exception e) {
            logger.error("Error connecting to Redis cluster", e);
        }
    }


    public void refreshClusterState() {
        if (!isClusterStateOk()) {
            logger.warn("Cluster state is not OK. Aborting refresh.");
            return;
        }

        // Extract current cluster nodes
        Set<HostAndPort> currentClusterNodes = new HashSet<>();
        if (jedisCluster != null) {
            Map<String, ConnectionPool> clusterNodes = jedisCluster.getClusterNodes();
            for (String node : clusterNodes.keySet()) {
                String[] parts = node.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                currentClusterNodes.add(new HostAndPort(host, port));
            }

            // Close the existing JedisCluster
            try {
                jedisCluster.close();
            } catch (Exception e) {
                logger.error("Error closing JedisCluster", e);
            }
        }

        // Reinitialize the JedisCluster with the extracted nodes
        ConnectCluster(currentClusterNodes);
        logger.info("Cluster state refreshed with nodes: {}", currentClusterNodes);
    }

    private boolean isClusterStateOk() {
        try {
            // Check cluster state by ensuring all nodes are reachable and agree on the configuration
            for (Map.Entry<String, ConnectionPool> entry : jedisCluster.getClusterNodes().entrySet()) {
                try (Jedis jedis = new Jedis(entry.getKey().split(":")[0], Integer.parseInt(entry.getKey().split(":")[1]))) {
                    String clusterInfo = jedis.clusterInfo();
                    if (!clusterInfo.contains("cluster_state:ok")) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error checking cluster state", e);
            return false;
        }
    }


}
