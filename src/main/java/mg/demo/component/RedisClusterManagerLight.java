package mg.demo.component;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RedisClusterManagerLight {
    private JedisCluster jedisCluster;

    public RedisClusterManagerLight(Set<HostAndPort> initialClusterNodes) {
        // Configure the connection pool
        GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(128);

        // Create JedisClientConfig with necessary configurations
        DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
                .connectionTimeoutMillis(2000) // 2 seconds
                .socketTimeoutMillis(2000) // 2 seconds
                .build();

        int maxAttempts = 50; // Number of attempts
        this.jedisCluster = new JedisCluster(initialClusterNodes, clientConfig, maxAttempts, poolConfig);
    }



    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

}
