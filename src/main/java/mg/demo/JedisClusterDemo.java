package mg.demo;

import mg.demo.component.RedisClusterManager;
import mg.demo.redistypes.RedisListStrImpl;
import mg.demo.redistypes.RedisMapStrIntImpl;
import redis.clients.jedis.HostAndPort;

import java.util.*;

import static java.lang.System.out;

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
        out.println("RedisMap Not Sorted: " + redisMap);
        out.println("RedisList Ordered: " + redisList);
        out.println("RedisMap contains 'key5': " + redisMap.containsKey("key5"));
        out.println("RedisMap contains value '5': " + redisMap.containsValue(5));
        out.println("Contains 'element5': " + redisList.contains("element5"));
        out.println("Get element at index 4: " + redisList.get(4));

        // Removing elements
        redisMap.remove("key5");
        redisList.remove("element5");
        out.println("=============AFTER REMOVING=============");

        // Test RedisMap, RedisList
        out.println("RedisMap Not Sorted: " + redisMap);
        out.println("RedisList Ordered: " + redisList);
        out.println("RedisMap contains 'key5': " + redisMap.containsKey("key5"));
        out.println("RedisMap contains value '5': " + redisMap.containsValue(5));
        out.println("Contains 'element5': " + redisList.contains("element5"));
        out.println("Get element at index 4: " + redisList.get(4));

        // Test SortedMap
        SortedMap<String, Integer> sortedRedisMap = new TreeMap<>(redisMap);
        sortedRedisMap.put("key7", 7);
        sortedRedisMap.put("key0", 0);
        out.println("SortedMap: " + sortedRedisMap);
        out.println("First key in SortedMap: " + sortedRedisMap.firstKey());
        out.println("Last key in SortedMap: " + sortedRedisMap.lastKey());

        // Test Deque
        Deque<String> redisDeque = new ArrayDeque<>(redisList);
        redisDeque.addFirst("elementAlpha");
        redisDeque.addLast("elementOmega");
        out.println("Deque: " + redisDeque);
        out.println("First element in Deque: " + redisDeque.getFirst());
        out.println("Last element in Deque: " + redisDeque.getLast());


        // Loop for Check Adding/Removing On The Fly
        checkAddingRemovingOnTheFly(redisClusterManager, redisMap, redisList);
    }

    private static void checkAddingRemovingOnTheFly(RedisClusterManager redisClusterManager, Map<String, Integer> redisMap, RedisListStrImpl redisList) {
        out.println("=============START CHECKING ADDING/REMOVING ON THE FLY=============");

        for (int i = 1; i <= 1_000_000; i++) {
            redisClusterManager.refreshClusterState();

            out.println("RedisMap Not Sorted: " + redisMap);
            out.println("RedisList Ordered: " + redisList);
            out.println("NODES: " + redisClusterManager.getJedisCluster().getClusterNodes().size());

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private static void hello() {
        String reset = "\033[0m";
        String color = "\033[0;32m";
        out.println(color);
        out.println("      :::    :::     :::     :::     ::: ::::::::::           :::        ::::    ::: ::::::::::: ::::::::  ::::::::::       :::::::::      :::   :::   ::: ");
        out.println("     :+:    :+:   :+: :+:   :+:     :+: :+:                :+: :+:      :+:+:   :+:     :+:    :+:    :+: :+:              :+:    :+:   :+: :+: :+:   :+:  ");
        out.println("    +:+    +:+  +:+   +:+  +:+     +:+ +:+               +:+   +:+     :+:+:+  +:+     +:+    +:+        +:+              +:+    +:+  +:+   +:+ +:+ +:+    ");
        out.println("   +#++:++#++ +#++:++#++: +#+     +:+ +#++:++#         +#++:++#++:    +#+ +:+ +#+     +#+    +#+        +#++:++#         +#+    +:+ +#++:++#++: +#++:      ");
        out.println("  +#+    +#+ +#+     +#+  +#+   +#+  +#+              +#+     +#+    +#+  +#+#+#     +#+    +#+        +#+              +#+    +#+ +#+     +#+  +#+        ");
        out.println(" #+#    #+# #+#     #+#   #+#+#+#   #+#              #+#     #+#    #+#   #+#+#     #+#    #+#    #+# #+#              #+#    #+# #+#     #+#  #+#         ");
        out.println("###    ### ###     ###     ###     ##########       ###     ###    ###    #### ########### ########  ##########       #########  ###     ###  ###          ");
        out.println(reset);
    }

}
