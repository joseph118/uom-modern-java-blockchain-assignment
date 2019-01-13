package model;

import java.nio.channels.SocketChannel;

public class ServerNode {
    private final String name;
    private final String ip;
    private final int port;
    private boolean isConnected;
    private SocketChannel socketChannel;

    public ServerNode(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        isConnected = false;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    @Override
    public String toString() {
        return "ServerNode{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", isConnected='" + isConnected + '\'' +
                '}';
    }
}

