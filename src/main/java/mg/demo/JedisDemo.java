package mg.demo;

import java.util.Map;

public class JedisDemo {
    public static void start()  {
        RedisClusterManager clusterManager = new RedisClusterManager();

        // Add initial nodes
        clusterManager.addNode("192.168.10.110", 6379);
        clusterManager.addNode("192.168.10.110", 6380);
        clusterManager.addNode("192.168.10.110", 6381);

        // Create the cluster
        if (clusterManager.createCluster()) {
            // Initialize cluster manager

            Map<String, Integer> redisMap = new RedisMap(clusterManager);

            redisMap.put("key1", 1);
            redisMap.put("key2", 2);
            System.out.println("key1: " + redisMap.get("key1"));
            System.out.println("key2: " + redisMap.get("key2"));

            // Simulate adding a new node
            if (clusterManager.addNewNodeToCluster("192.168.10.110", 6382)) {
                // Continue operations after adding the node
                redisMap.put("key3", 3);
                System.out.println("key3: " + redisMap.get("key3"));

                // Simulate removing a node
//                if (clusterManager.removeNodeFromCluster("192.168.10.110", 6382)) {
//                    // Continue operations after removing the node
//                    redisMap.put("key4", 4);
//                    System.out.println("key4: " + redisMap.get("key4"));
//                }
            }

            // Clean up
            clusterManager.getJedisCluster().close();
        }
    }
}
