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
            ProcessBuilder createProcessBuilder = new ProcessBuilder(createCommand.toString().split(" "));
            System.out.println("Executing command: " + String.join(" ", createProcessBuilder.command()));
            Process createProcess = createProcessBuilder.start();
            createProcess.waitFor();
            printProcessOutput(createProcess);

            System.out.println("Successfully created the Redis cluster.");

            // Initialize JedisCluster
            this.jedisCluster = new JedisCluster(clusterNodes);

            // Wait until the cluster is up
            if (waitForClusterState("ok", 10000)) {
                return true;
            } else {
                System.err.println("Cluster did not reach a healthy state in time.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean waitForClusterState(String desiredState, long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                HostAndPort node = clusterNodes.iterator().next();
                ProcessBuilder clusterInfoProcessBuilder = new ProcessBuilder(
                        "redis-cli", "-p", String.valueOf(node.getPort()), "cluster", "info");
                Process clusterInfoProcess = clusterInfoProcessBuilder.start();
                clusterInfoProcess.waitFor();
                String output = getProcessOutput(clusterInfoProcess);
                if (output.contains("cluster_state:" + desiredState)) {
                    System.out.println("Cluster state is now: " + desiredState);
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

    public boolean addNewNodeToCluster(String newNodeIp, int newNodePort) {
        try {
            HostAndPort existingNode = clusterNodes.iterator().next();

            // Meet the new node with an existing node in the cluster
            ProcessBuilder meetProcessBuilder = new ProcessBuilder(
                    "redis-cli", "-h", newNodeIp, "-p", String.valueOf(newNodePort),
                    "cluster", "meet", existingNode.getHost(), String.valueOf(existingNode.getPort()));
            System.out.println("Executing command: " + String.join(" ", meetProcessBuilder.command()));
            Process meetProcess = meetProcessBuilder.start();
            meetProcess.waitFor();
            printProcessOutput(meetProcess);

            // Add a delay to ensure the node has time to join the cluster
//            Thread.sleep(5000);

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

            int totalSlots = 16384;
            int slotsPerNode = totalSlots / (clusterNodes.size());

            for (HostAndPort node : clusterNodes) {
                String nodeId = getNodeId(node.getHost(), node.getPort());
                if (nodeId == null || nodeId.equals(newNodeId)) {
                    continue;
                }

                int slotsToMove = Math.min(slotsPerNode, totalSlots);
                totalSlots -= slotsToMove;

                ProcessBuilder reshardProcessBuilder = new ProcessBuilder(
                        "redis-cli", "--cluster", "reshard", node.getHost() + ":" + node.getPort(),
                        "--cluster-from", nodeId,
                        "--cluster-to", newNodeId,
                        "--cluster-slots", String.valueOf(slotsToMove),
                        "--cluster-yes", "-p", String.valueOf(node.getPort()));
                System.out.println("Executing command: " + String.join(" ", reshardProcessBuilder.command()));
                Process reshardProcess = reshardProcessBuilder.start();

                StreamLogger outputLogger = new StreamLogger(reshardProcess.getInputStream());
                StreamLogger errorLogger = new StreamLogger(reshardProcess.getErrorStream());
                outputLogger.start();
                errorLogger.start();

                reshardProcess.waitFor();
                outputLogger.join();
                errorLogger.join();
            }

            System.out.println("Successfully resharded the cluster to include the new node.");
        } catch (Exception e) {
            System.err.println("Error resharding the cluster: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getNodeId(String host, int port) throws Exception {
        ProcessBuilder nodeIdProcessBuilder = new ProcessBuilder(
                "redis-cli", "-h", host, "-p", String.valueOf(port), "cluster", "nodes");
        Process nodeIdProcess = nodeIdProcessBuilder.start();
        nodeIdProcess.waitFor();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(nodeIdProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
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
