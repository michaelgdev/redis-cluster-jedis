package mg.demo;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;

import java.util.Map;
import java.util.Set;

public class JedisDemo {
    public static void start()  {
        RedisClusterManager clusterManager = new RedisClusterManager();

        // Add initial nodes
        clusterManager.addNode("192.168.10.110", 6379);
        clusterManager.addNode("192.168.10.110", 6380);
        clusterManager.addNode("192.168.10.110", 6381);

        clusterManager.createCluster();

        clusterManager.addNewNodeToCluster("192.168.10.110", 6382);


//        clusterManager.getJedisCluster().close();

        // Create the cluster
//        if (clusterManager.createCluster()) {
//            // Initialize cluster manager
//
//            Map<String, Integer> redisMap = new RedisMap(clusterManager);
//
//            redisMap.put("key1", 1);
//            redisMap.put("key2", 2);
//            System.out.println("key1: " + redisMap.get("key1"));
//            System.out.println("key2: " + redisMap.get("key2"));
//
//            // Simulate adding a new node
//            if (clusterManager.addNewNodeToCluster("192.168.10.110", 6382)) {
//                // Continue operations after adding the node
//                redisMap.put("key3", 3);
//                System.out.println("key3: " + redisMap.get("key3"));
//
//                // Simulate removing a node
////                if (clusterManager.removeNodeFromCluster("192.168.10.110", 6382)) {
////                    // Continue operations after removing the node
////                    redisMap.put("key4", 4);
////                    System.out.println("key4: " + redisMap.get("key4"));
////                }
//            }
//
//            // Clean up
//            clusterManager.getJedisCluster().close();
//        }
    }

    public static void start2 () {
        // Replace with your actual cluster nodes
        Set<HostAndPort> jedisClusterNodes = Set.of(
                new HostAndPort("192.168.10.110", 6379),
                new HostAndPort("192.168.10.110", 6380),
                new HostAndPort("192.168.10.110", 6381)
//                new HostAndPort("192.168.10.110", 6382)
        );

        JedisCluster jedisCluster = new JedisCluster(jedisClusterNodes);

        RedisClusterSlotCounter slotCounter = new RedisClusterSlotCounter(jedisCluster);
        Map<HostAndPort, Integer> slotsPerNode = slotCounter.getSlotsPerNode();

        for (Map.Entry<HostAndPort, Integer> entry : slotsPerNode.entrySet()) {
            System.out.println("Node: " + entry.getKey() + " - Slots: " + entry.getValue());
        }

    }
}
