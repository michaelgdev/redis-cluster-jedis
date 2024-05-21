package mg.demo;

import redis.clients.jedis.HostAndPort;

class ClusterNode extends HostAndPort {
    private int slotsSize;

    public ClusterNode(String host, int port, int slotsSize) {
        super(host, port);
        this.slotsSize = slotsSize;
    }

    public int getSlotsSize() {
        return slotsSize;
    }

    public void setSlotsSize(int slotsSize) {
        this.slotsSize = slotsSize;
    }
}