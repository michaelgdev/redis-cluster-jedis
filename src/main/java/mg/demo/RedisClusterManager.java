package mg.demo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.HostAndPort;
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
            StringBuilder createCommand = new StringBuilder("redis-cli --cluster create");
            for (HostAndPort node : clusterNodes) {
                createCommand.append(" ").append(node.getHost()).append(":").append(node.getPort());
            }
            createCommand.append(" --cluster-yes");

            // Execute the create cluster command
            System.out.println("Created Command: " + createCommand);
            ProcessBuilder createProcessBuilder = new ProcessBuilder(createCommand.toString().split(" "));
            Process createProcess = createProcessBuilder.start();
            createProcess.waitFor();
            printProcessOutput(createProcess);

            System.out.println("Successfully created the Redis cluster.");

            // Initialize JedisCluster
            this.jedisCluster = new JedisCluster(clusterNodes);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean addNewNodeToCluster(String newNodeIp, int newNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Meet the new node with an existing node in the cluster
            ProcessBuilder meetProcessBuilder = new ProcessBuilder(
                    "redis-cli", "-h", newNodeIp, "-p", String.valueOf(newNodePort),
                    "cluster", "meet", existingNode.getHost(), String.valueOf(existingNode.getPort()));
            System.out.println("Created Command: " + String.join(" ", meetProcessBuilder.command()));
            Process meetProcess = meetProcessBuilder.start();
            meetProcess.waitFor();
            printProcessOutput(meetProcess);

            // Add the new node to the cluster
            ProcessBuilder addNodeProcessBuilder = new ProcessBuilder(
                    "redis-cli", "--cluster", "add-node", newNodeIp + ":" + newNodePort,
                    existingNode.getHost() + ":" + existingNode.getPort());
            System.out.println("Created Command: " + String.join(" ", addNodeProcessBuilder.command()));
            Process addNodeProcess = addNodeProcessBuilder.start();
            addNodeProcess.waitFor();
            printProcessOutput(addNodeProcess);

            // Reshard the cluster to distribute slots to the new node
            reshardCluster(newNodeIp, newNodePort);

            // Update the cluster nodes
            clusterNodes.add(new HostAndPort(newNodeIp, newNodePort));
            updateClusterNodes(clusterNodes);

            System.out.println("Successfully added new node to the cluster.");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void reshardCluster(String newNodeIp, int newNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Calculate the number of slots to move to the new node
            int totalSlots = 16384;
            int slotsPerNode = totalSlots / (clusterNodes.size() + 1); // Include the new node

            // Reshard the cluster
            ProcessBuilder reshardProcessBuilder = new ProcessBuilder(
                    "redis-cli", "--cluster", "reshard", existingNode.getHost() + ":" + existingNode.getPort(),
                    "--cluster-from", existingNode.getHost() + ":" + existingNode.getPort(),
                    "--cluster-to", newNodeIp + ":" + newNodePort,
                    "--cluster-slots", String.valueOf(slotsPerNode),
                    "--cluster-yes");
            System.out.println("Created Command: " + String.join(" ", reshardProcessBuilder.command()));
            Process reshardProcess = reshardProcessBuilder.start();
            reshardProcess.waitFor();
            printProcessOutput(reshardProcess);

            System.out.println("Successfully resharded the cluster to include the new node.");
        } catch (Exception e) {
            System.err.println("Error resharding the cluster: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean removeNodeFromCluster(String removeNodeIp, int removeNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Get the node ID of the node to remove
            ProcessBuilder nodeIdProcessBuilder = new ProcessBuilder(
                    "redis-cli", "-h", existingNode.getHost(), "-p", String.valueOf(existingNode.getPort()),
                    "cluster", "nodes");
            Process nodeIdProcess = nodeIdProcessBuilder.start();
            nodeIdProcess.waitFor();
            String nodeId = getNodeIdFromOutput(nodeIdProcess, removeNodeIp, removeNodePort);

            if (nodeId != null) {
                // Remove the node from the cluster
                ProcessBuilder removeNodeProcessBuilder = new ProcessBuilder(
                        "redis-cli", "--cluster", "del-node", existingNode.getHost() + ":" + existingNode.getPort(), nodeId);
                System.out.println("Created Command: " + String.join(" ", removeNodeProcessBuilder.command()));
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

    private void printProcessOutput(Process process) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
