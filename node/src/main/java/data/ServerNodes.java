package data;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServerNodes {
    private final List<ServerNode> serverNodes;

    public ServerNodes(List<ServerNode> serverNodes) {
        this.serverNodes = serverNodes;
    }

    public long getConnectedNodesCount() {
        return this.serverNodes.parallelStream()
                .filter(ServerNode::isConnected).count();
    }

    public ServerNode getNodeByName(String name) {
        return this.serverNodes.parallelStream()
                .filter(serverNode -> serverNode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public List<ServerNode> getConnectedNodes() {
        return this.serverNodes.parallelStream()
                .filter(ServerNode::isConnected)
                .collect(Collectors.toList());
    }

    public List<ServerNode> getNodes(String nodeName) {
        return serverNodes.parallelStream()
                .filter(serverNode -> !serverNode.getName().equals(nodeName))
                .collect(Collectors.toList());
    }

    public void updateNodeConnection(String nodeName, boolean isConnected, SocketChannel client) {
        Optional<ServerNode> serverNodeOptional = this.serverNodes.parallelStream()
                .filter(serverNode -> serverNode.getName().equals(nodeName))
                .findFirst();

        if (serverNodeOptional.isPresent()) {
            ServerNode serverNode = serverNodeOptional.get();
            serverNode.setSocketChannel(client);
            serverNode.setConnected(isConnected);
        }
    }
}
