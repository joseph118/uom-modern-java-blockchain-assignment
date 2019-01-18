package core.message.node;

import core.message.Message;
import data.ServerNode;

public class NodeMessage extends Message {
    private final ServerNode serverNode;

    public NodeMessage(String message, ServerNode serverNode) {
        super(message);
        this.serverNode = serverNode;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }
}
