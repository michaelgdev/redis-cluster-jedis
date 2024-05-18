package mg.demo;



import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RedisClusterJedisApp {

    public static void main(String[] args) {
        // Define the Redis cluster nodes using the container IP addresses and ports
        RedisURI node1 = RedisURI.create("redis://localhost:6378");
        RedisURI node2 = RedisURI.create("redis://localhost:6379");
        RedisURI node3 = RedisURI.create("redis://localhost:6380");

        RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2, node3));

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            RedisAdvancedClusterCommands<String, String> syncCommands = connection.sync();

            // Example of setting and getting a key to demonstrate sharding
            for (int i = 0; i < 10; i++) {
                String key = "key" + i;
                String value = "value" + i;
                syncCommands.set(key, value);
                System.out.println("Set " + key + ": " + syncCommands.get(key));
            }

            // Example of incrementing counters to demonstrate sharding
            for (int i = 0; i < 10; i++) {
                String counterKey = "counter" + i;
                syncCommands.incr(counterKey);
                System.out.println(counterKey + ": " + syncCommands.get(counterKey));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        clusterClient.shutdown();
    }

}