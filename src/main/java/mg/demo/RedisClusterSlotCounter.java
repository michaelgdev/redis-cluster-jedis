package mg.demo;

import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedisClusterSlotCounter {

    private JedisCluster jedisCluster;

    public RedisClusterSlotCounter(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    public Map<HostAndPort, Integer> getSlotsPerNode() {
        Map<HostAndPort, Integer> slotsPerNode = new HashMap<>();

        try {
            Map<String, ConnectionPool> clusterNodes = jedisCluster.getClusterNodes();
            for (Map.Entry<String, ConnectionPool> entry : clusterNodes.entrySet()) {
                String nodeInfo = entry.getKey();
                HostAndPort slotNode = parseHostAndPort(nodeInfo);

                try (Jedis jedis = new Jedis(slotNode)) {
                    List<Object> slots = jedis.clusterSlots();
                    for (Object slotInfoObj : slots) {
                        List<Object> slotInfo = (List<Object>) slotInfoObj;
                        long startSlot = (long) slotInfo.get(0);
                        long endSlot = (long) slotInfo.get(1);
                        int slotCount = (int) (endSlot - startSlot + 1);

                        List<Object> nodeDetails = (List<Object>) slotInfo.get(2);
                        String host = new String((byte[]) nodeDetails.get(0));
                        int port = ((Long) nodeDetails.get(1)).intValue();

                        slotsPerNode.put(new HostAndPort(host, port), slotCount);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return slotsPerNode;
    }

    private HostAndPort parseHostAndPort(String nodeInfo) {
        String[] parts = nodeInfo.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);
        return new HostAndPort(host, port);
    }
}
