package model;

public class ServerNode {
    private final String name;
    private final String ip;
    private final String port;

    public ServerNode(String name, String ip, String port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ServerNode{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}

