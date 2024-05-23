package mg.demo;

import mg.demo.component.RedisClusterManager;
import mg.demo.redistypes.RedisListStrImpl;
import mg.demo.redistypes.RedisMapStrIntImpl;
import redis.clients.jedis.HostAndPort;

import java.util.*;

public class JedisClusterDemo {

    private static final int ELEMENTS = 10;

    public static void start() {
        hello();
        RedisClusterManager redisClusterManager = new RedisClusterManager();
        Set<HostAndPort> initialNodes = new HashSet<>();


        // !!!!! ADD CORRECT IP AND PORT
        initialNodes.add(new HostAndPort("192.168.10.110", 6379));


        redisClusterManager.ConnectCluster(initialNodes);

        // Create the RedisMa, RedisList instance
        Map<String, Integer> redisMap = new RedisMapStrIntImpl(redisClusterManager);
        RedisListStrImpl redisList = new RedisListStrImpl(redisClusterManager);

        // Adding elements
        for (int i = 1; i <= ELEMENTS; i++) {
            redisMap.put("key" + i, i);
            redisList.add("element" + i);
        }

        // Test RedisMap, RedisList
        System.out.println("RedisMap Not Sorted: " + redisMap);
        System.out.println("RedisList Ordered: " + redisList);
        System.out.println("RedisMap contains 'key5': " + redisMap.containsKey("key5"));
        System.out.println("RedisMap contains value '5': " + redisMap.containsValue(5));
        System.out.println("Contains 'element5': " + redisList.contains("element5"));
        System.out.println("Get element at index 4: " + redisList.get(4));

        // Removing elements
        redisMap.remove("key5");
        redisList.remove("element5");
        System.out.println("=============AFTER REMOVING=============");

        // Test RedisMap, RedisList
        System.out.println("RedisMap Not Sorted: " + redisMap);
        System.out.println("RedisList Ordered: " + redisList);
        System.out.println("RedisMap contains 'key5': " + redisMap.containsKey("key5"));
        System.out.println("RedisMap contains value '5': " + redisMap.containsValue(5));
        System.out.println("Contains 'element5': " + redisList.contains("element5"));
        System.out.println("Get element at index 4: " + redisList.get(4));

        // Test SortedMap
        SortedMap<String, Integer> sortedRedisMap = new TreeMap<>(redisMap);
        sortedRedisMap.put("key7", 7);
        sortedRedisMap.put("key0", 0);
        System.out.println("SortedMap: " + sortedRedisMap);
        System.out.println("First key in SortedMap: " + sortedRedisMap.firstKey());
        System.out.println("Last key in SortedMap: " + sortedRedisMap.lastKey());

        // Test Deque
        Deque<String> redisDeque = new ArrayDeque<>(redisList);
        redisDeque.addFirst("elementAlpha");
        redisDeque.addLast("elementOmega");
        System.out.println("Deque: " + redisDeque);
        System.out.println("First element in Deque: " + redisDeque.getFirst());
        System.out.println("Last element in Deque: " + redisDeque.getLast());


        // Check Adding/Removing On The Fly
        checkAddingRemovingOnTheFly(redisClusterManager, redisMap, redisList);
    }

    private static void checkAddingRemovingOnTheFly(RedisClusterManager redisClusterManager, Map<String, Integer> redisMap, RedisListStrImpl redisList) {
        System.out.println("=============START CHECKING ADDING/REMOVING ON THE FLY=============");
        while (true) {
            redisClusterManager.refreshClusterState();

            System.out.println("RedisMap Not Sorted: " + redisMap);
            System.out.println("RedisList Ordered: " + redisList);
            System.out.println("NODES: " + redisClusterManager.getJedisCluster().getClusterNodes().size());

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static void hello(){
        System.out.println();
        System.out.println("      :::    :::     :::     :::     ::: ::::::::::           :::        ::::    ::: ::::::::::: ::::::::  ::::::::::       :::::::::      :::   :::   ::: ");
        System.out.println("     :+:    :+:   :+: :+:   :+:     :+: :+:                :+: :+:      :+:+:   :+:     :+:    :+:    :+: :+:              :+:    :+:   :+: :+: :+:   :+:  ");
        System.out.println("    +:+    +:+  +:+   +:+  +:+     +:+ +:+               +:+   +:+     :+:+:+  +:+     +:+    +:+        +:+              +:+    +:+  +:+   +:+ +:+ +:+    ");
        System.out.println("   +#++:++#++ +#++:++#++: +#+     +:+ +#++:++#         +#++:++#++:    +#+ +:+ +#+     +#+    +#+        +#++:++#         +#+    +:+ +#++:++#++: +#++:      ");
        System.out.println("  +#+    +#+ +#+     +#+  +#+   +#+  +#+              +#+     +#+    +#+  +#+#+#     +#+    +#+        +#+              +#+    +#+ +#+     +#+  +#+        ");
        System.out.println(" #+#    #+# #+#     #+#   #+#+#+#   #+#              #+#     #+#    #+#   #+#+#     #+#    #+#    #+# #+#              #+#    #+# #+#     #+#  #+#         ");
        System.out.println("###    ### ###     ###     ###     ##########       ###     ###    ###    #### ########### ########  ##########       #########  ###     ###  ###          ");
        System.out.println();
    }

    public static void start3()  {
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


}
