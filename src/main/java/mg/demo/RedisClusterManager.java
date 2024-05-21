package mg.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

public class RedisClusterManager {
    private JedisCluster jedisCluster;
    private Set<HostAndPort> clusterNodes;

    public RedisClusterManager() {
        this.clusterNodes = new HashSet<>();
    }

    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    public synchronized void updateClusterNodes(Set<HostAndPort> newNodes) {
        try {
            if (this.jedisCluster != null) {
                this.jedisCluster.close();
            }
            this.jedisCluster = new JedisCluster(newNodes);
            this.clusterNodes = newNodes;
            System.out.println("Updated Redis Cluster nodes.");
        } catch (Exception e) {
            System.err.println("Error updating Redis Cluster nodes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addNode(String nodeIp, int nodePort) {
        clusterNodes.add(new HostAndPort(nodeIp, nodePort));
    }

    public boolean createCluster() {
        try {
            // Meet each node with every other node to form the cluster
            for (HostAndPort node : clusterNodes) {
                try (Jedis jedis = new Jedis(node)) {
                    for (HostAndPort otherNode : clusterNodes) {
                        if (!node.equals(otherNode)) {
                            jedis.clusterMeet(otherNode.getHost(), otherNode.getPort());
                        }
                    }
                }
            }

            // Wait for the cluster to recognize all nodes
            waitForClusterState("cluster_known_nodes:3", 10000);

            // Allocate slots to the nodes
            int totalSlots = 16384;
            int slotsPerNode = totalSlots / clusterNodes.size();
            int remainingSlots = totalSlots % clusterNodes.size();

            int startSlot = 0;
            for (HostAndPort node : clusterNodes) {
                int endSlot = startSlot + slotsPerNode - 1;
                if (remainingSlots > 0) {
                    endSlot++;
                    remainingSlots--;
                }

                int[] slots = new int[endSlot - startSlot + 1];
                for (int i = startSlot; i <= endSlot; i++) {
                    slots[i - startSlot] = i;
                }

                try (Jedis jedis = new Jedis(node)) {
                    jedis.clusterAddSlots(slots);
                }

                startSlot = endSlot + 1;
            }

            // Wait for all nodes to be properly initialized with slots
            waitForClusterState("cluster_state:ok", 10000);

            // Initialize JedisCluster
            this.jedisCluster = new JedisCluster(clusterNodes);
            System.out.println("Successfully created the Redis cluster.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean waitForClusterState(String desiredState, long timeoutMillis) {
        System.out.print("Waiting.");
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                HostAndPort node = clusterNodes.iterator().next();
                try (Jedis jedis = new Jedis(node)) {
                    String output = jedis.clusterInfo();
                    if (output.contains(desiredState)) {
                        System.out.println();
                        System.out.println("Cluster state is now: " + desiredState);
                        return true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
                System.out.print(".");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println();
        return false;
    }


    public boolean addNewNodeToCluster(String newNodeIp, int newNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Meet the new node with an existing node in the cluster using Jedis
            try (Jedis jedis = new Jedis(newNodeIp, newNodePort)) {
                jedis.clusterMeet(existingNode.getHost(), existingNode.getPort());
            }
            System.out.println("Node " + newNodeIp + ":" + newNodePort + " has been met with the cluster.");

            // Check cluster status to confirm the node has joined
            if (!waitForClusterNodes(newNodeIp, newNodePort, 10000)) {
                System.err.println("Node did not join the cluster in time.");
                return false;
            }

            // Update the cluster nodes
            clusterNodes.add(new HostAndPort(newNodeIp, newNodePort));
            updateClusterNodes(clusterNodes);

            // Reshard the cluster to distribute slots to the new node
            reshardCluster(existingNode, newNodeIp, newNodePort);

            System.out.println("Successfully added new node to the cluster.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private boolean waitForClusterNodes(String newNodeIp, int newNodePort, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                ProcessBuilder nodesProcessBuilder = new ProcessBuilder(
                        "redis-cli", "-h", newNodeIp, "-p", String.valueOf(newNodePort), "cluster", "nodes");
                Process nodesProcess = nodesProcessBuilder.start();
                nodesProcess.waitFor();
                String output = getProcessOutput(nodesProcess);
                if (output.contains("connected")) {
                    System.out.println("Node " + newNodeIp + ":" + newNodePort + " has joined the cluster.");
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    private void reshardCluster(HostAndPort existingNode, String newNodeIp, int newNodePort) {
        try {
            // Get the node IDs
            String newNodeId = getNodeId(newNodeIp, newNodePort);
            if (newNodeId == null) {
                System.err.println("Error retrieving new node ID.");
                return;
            }

            // Retry mechanism to ensure the new node is fully recognized
            int retries = 5;
            while (retries > 0 && !isNodeRecognized(existingNode, newNodeId)) {
                System.out.println("Waiting for the new node to be recognized by the cluster...");
                Thread.sleep(2000);
                retries--;
            }

            if (retries == 0) {
                System.err.println("New node is not recognized by the cluster after multiple attempts.");
                return;
            }

            int totalSlots = 16384;
            int slotsPerNode = totalSlots / clusterNodes.size();
            int remainingSlots = totalSlots % clusterNodes.size();

            for (HostAndPort node : clusterNodes) {
                String nodeId = getNodeId(node.getHost(), node.getPort());
                if (nodeId == null || nodeId.equals(newNodeId)) {
                    continue;
                }

                try (Jedis jedis = new Jedis(node.getHost(), node.getPort())) {
                    List<Object> slots = jedis.clusterSlots();
                    for (int slot = 0; slot < slotsPerNode; slot++) {
                        int slotNumber = (slot + node.getPort() * slotsPerNode) % 16384;

                        // Check current owner of the slot before attempting to move
                        List<Object> slotInfo = slots.stream()
                                .map(s -> (List<Object>) s)
                                .filter(s -> (long) s.get(0) <= slotNumber && (long) s.get(1) >= slotNumber)
                                .findFirst().orElse(null);
                        if (slotInfo == null) {
                            continue;
                        }

                        List<Object> nodeDetails = (List<Object>) slotInfo.get(2);
                        String ownerId = new String((byte[]) nodeDetails.get(2));

                        if (ownerId.equals(nodeId)) {
                            jedis.clusterSetSlotMigrating(slotNumber, newNodeId);
                            try (Jedis newNodeJedis = new Jedis(newNodeIp, newNodePort)) {
                                newNodeJedis.clusterSetSlotImporting(slotNumber, nodeId);
                                jedis.clusterSetSlotNode(slotNumber, newNodeId);
                                System.out.println("Moved slot " + slotNumber + " from node " + nodeId + " to " + newNodeId);
                            }
                        } else {
                            System.out.println("Slot " + slotNumber + " is not owned by node " + nodeId);
                        }
                    }
                }
            }

            System.out.println("Successfully resharded the cluster to include the new node.");
        } catch (Exception e) {
            System.err.println("Error resharding the cluster: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isNodeRecognized(HostAndPort existingNode, String newNodeId) {
        try (Jedis jedis = new Jedis(existingNode.getHost(), existingNode.getPort())) {
            String nodes = jedis.clusterNodes();
            for (String nodeInfo : nodes.split("\n")) {
                if (nodeInfo.contains(newNodeId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private String getNodeId(String host, int port) throws Exception {
        try (Jedis jedis = new Jedis(host, port)) {
            String clusterNodes = jedis.clusterNodes();
            for (String line : clusterNodes.split("\n")) {
                if (line.contains(host + ":" + port)) {
                    return line.split(" ")[0];
                }
            }
        }
        return null;
    }



    public boolean removeNodeFromCluster(String removeNodeIp, int removeNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Get the node ID of the node to remove
            ProcessBuilder nodeIdProcessBuilder = new ProcessBuilder(
                    "redis-cli", "-h", existingNode.getHost(), "-p", String.valueOf(existingNode.getPort()),
                    "cluster", "nodes");
            System.out.println("Executing command: " + String.join(" ", nodeIdProcessBuilder.command()));
            Process nodeIdProcess = nodeIdProcessBuilder.start();
            nodeIdProcess.waitFor();
            String nodeId = getNodeIdFromOutput(nodeIdProcess, removeNodeIp, removeNodePort);

            if (nodeId != null) {
                // Remove the node from the cluster
                ProcessBuilder removeNodeProcessBuilder = new ProcessBuilder(
                        "redis-cli", "--cluster", "del-node", existingNode.getHost() + ":" + existingNode.getPort(), nodeId, "-p", String.valueOf(existingNode.getPort()));
                System.out.println("Executing command: " + String.join(" ", removeNodeProcessBuilder.command()));
                Process removeNodeProcess = removeNodeProcessBuilder.start();
                removeNodeProcess.waitFor();
                printProcessOutput(removeNodeProcess);

                // Update the cluster nodes
                clusterNodes.remove(new HostAndPort(removeNodeIp, removeNodePort));
                updateClusterNodes(clusterNodes);

                System.out.println("Successfully removed node from the cluster.");
                return true;
            } else {
                System.err.println("Node ID not found for the specified node.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getNodeIdFromOutput(Process process, String removeNodeIp, int removeNodePort) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(removeNodeIp + ":" + removeNodePort)) {
                    return line.split(" ")[0];
                }
            }
        }
        return null;
    }

    private String getProcessOutput(Process process) throws Exception {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }

    private void printProcessOutput(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
