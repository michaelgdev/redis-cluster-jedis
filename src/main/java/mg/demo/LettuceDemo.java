package mg.demo;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

import java.util.Arrays;

public  class LettuceDemo {


    public static void start() {
        RedisURI node1 = RedisURI.create("redis://192.168.10.53:6379");
        RedisURI node2 = RedisURI.create("redis://192.168.10.53:6380");
        RedisURI node3 = RedisURI.create("redis://192.168.10.53:6381");
        RedisURI node4 = RedisURI.create("redis://192.168.10.53:6382");
        RedisURI node5 = RedisURI.create("redis://192.168.10.53:6383");
        RedisURI node6 = RedisURI.create("redis://192.168.10.53:6384");

        RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2, node3, node4, node5, node6));

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {
            RedisAdvancedClusterCommands<String, String> syncCommands = connection.sync();

            for (int i = 0; i < 10; i++) {
                String key = "key" + i;
                String value = "value" + i;
                syncCommands.set(key, value);
                System.out.println("Set " + key + ": " + syncCommands.get(key));
            }

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
