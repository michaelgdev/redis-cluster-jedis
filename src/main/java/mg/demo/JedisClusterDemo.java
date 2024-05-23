package mg.demo;

import mg.demo.component.RedisClusterManager2;
import mg.demo.component.RedisClusterManager;
import mg.demo.component.impl.RedisClusterManager2Impl;
import mg.demo.redistypes.RedisMap;
import mg.demo.redistypes.RedisMap2;
import redis.clients.jedis.HostAndPort;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JedisClusterDemo {

    public static void start()  {
//        RedisClusterManager2 clusterManager = new RedisClusterManager2Impl();

//        // Add initial nodes
//        clusterManager.addNode("192.168.10.110", 6379);
//        clusterManager.addNode("192.168.10.110", 6380);
//        clusterManager.addNode("192.168.10.110", 6381);
//        clusterManager.addNode("192.168.10.110", 6382);
//        clusterManager.addNode("192.168.10.110", 6383);
//        clusterManager.addNode("192.168.10.110", 6384);
//        clusterManager.updateClusterNodes();

//        clusterManager.createCluster();

        // Create the RedisMap instance
//        Map<String, Integer> redisMap = new RedisMap2(clusterManager);

        // Add 100 keys to the RedisMap
//        for (int i = 1; i <= 10; i++) {
//            redisMap.put("key" + i, i);
//        }
        // Retrieve and print the values of the keys along with the node information
//        for (int i = 1; i <= 10; i++) {
//            Integer value = redisMap.get("key" + i);
//            System.out.println("Getting key" + i + ": " + value);
//        }

//        clusterManager.addNewNodeToCluster("192.168.10.110", 6385);

//        for (int i = 1; i <= 10; i++) {
//            Integer value = redisMap.get("key" + i);
//            System.out.println("Getting key" + i + ": " + value);
//        }

//        clusterManager.addNewNodeToCluster("192.168.10.110", 6383);
//
//        for (int i = 1; i <= 10; i++) {
//            Integer value = redisMap.get("key" + i);
//            System.out.println("Getting key" + i + ": " + value);
//        }


//        clusterManager.removeNodeFromCluster("192.168.10.110", 6380);

//        while (true) {
//            for (int i = 1; i <= 10; i++) {
//                Integer value = redisMap.get("key" + i);
//                System.out.println("Getting key" + i + ": " + value);
//            }
//            try {
//                Thread.sleep(2000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }


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
        RedisClusterManager redisClusterManager = new RedisClusterManager();

        Set<HostAndPort> initialNodes = new HashSet<>();
        initialNodes.add(new HostAndPort("192.168.10.53", 6379));
        redisClusterManager.ConnectCluster(initialNodes);


        // Create the RedisMap instance
        Map<String, Integer> redisMap = new RedisMap(redisClusterManager);

        // Add 100 keys to the RedisMap
        for (int i = 1; i <= 10; i++) {
            redisMap.put("key" + i, i);
        }

        while (true) {
//            redisClusterManagerLight.getJedisCluster().getClusterNodes();
            redisClusterManager.refreshClusterState();
            for (int i = 1; i <= 10; i++) {
                Integer value = redisMap.get("key" + i);
                System.out.println("Getting key" + i + ": " + value);
                System.out.println("Contains key: " + redisMap.containsKey("key" + i));
                System.out.println("Contains value: " + redisMap.containsValue(value));
            }
            System.out.println("NODES: " + redisClusterManager.getJedisCluster().getClusterNodes().size());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
