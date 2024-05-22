package mg.demo.component.impl;

import mg.demo.component.RedisClusterManager;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class RedisClusterManagerImpl implements RedisClusterManager {
    private JedisCluster jedisCluster;
    private Set<HostAndPort> clusterNodes;

    public RedisClusterManagerImpl() {
        this.clusterNodes = new HashSet<>();
    }

    @Override
    public JedisCluster getJedisCluster() {
        return jedisCluster;
    }

    @Override
    public synchronized void updateClusterNodes() {
        try {
            if (this.jedisCluster != null) {
                this.jedisCluster.close();
            }
            this.jedisCluster = new JedisCluster(clusterNodes);
            System.out.println("Updated Redis Cluster nodes.");
        } catch (JedisClusterOperationException e)
        {
            System.out.println("Cluster is not created, SKIP");
        }
        catch (Exception e) {
            System.err.println("Error updating Redis Cluster nodes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void addNode(String nodeIp, int nodePort) {
        clusterNodes.add(new HostAndPort(nodeIp, nodePort));
    }

    @Override
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
            waitForClusterState("cluster_known_nodes:" + clusterNodes.size(), 10000);

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

    @Override
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
            updateClusterNodes();

            // Rebalance the cluster after adding new node
            waitForClusterState("cluster_state:ok", 10000);
            rebalanceCluster();

            System.out.println("Successfully added new node to the cluster.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public void rebalanceCluster() {
        try {
            HostAndPort node = clusterNodes.iterator().next();
            String[] args = {"redis-cli", "--cluster", "rebalance", node.getHost() + ":" + node.getPort(), "--cluster-use-empty-masters"};
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);  // Redirect error stream to standard output
            Process process = pb.start();

            // Capture and print the output of the process
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Cluster rebalanced successfully.");
            } else {
                System.err.println("Error rebalancing the cluster.");
            }
        } catch (Exception e) {
            System.err.println("Error rebalancing the cluster: " + e.getMessage());
            e.printStackTrace();
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

    private boolean waitForClusterNodes(String newNodeIp, int newNodePort, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try (Jedis jedis = new Jedis(newNodeIp, newNodePort)) {
                String output = jedis.clusterNodes();
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

    @Override
    public boolean removeNodeFromCluster(String removeNodeIp, int removeNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Get the node ID of the node to remove
            String nodeId = getNodeId(removeNodeIp, removeNodePort);

            if (nodeId != null) {
                // Remove the node from the cluster
                try (Jedis jedis = new Jedis(existingNode.getHost(), existingNode.getPort())) {
                    jedis.clusterForget(nodeId);
                }

                // Update the cluster nodes
                clusterNodes.remove(new HostAndPort(removeNodeIp, removeNodePort));
                updateClusterNodes();

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
}