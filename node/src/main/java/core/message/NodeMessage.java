package core.message;

import model.ServerNode;

public class NodeMessage extends Message {
    private final ServerNode serverNode;
    private boolean isFinalPhase;

    public NodeMessage(String message, ServerNode serverNode, boolean isFinalPhase) {
        super(message);

        this.serverNode = serverNode;
        this.isFinalPhase = isFinalPhase;
    }

    public NodeMessage(String message, ServerNode serverNode) {
        super(message);
        this.serverNode = serverNode;
        this.isFinalPhase = false;
    }

    public ServerNode getServerNode() {
        return serverNode;
    }

    public boolean getIsFinalPhase() {
        return isFinalPhase;
    }
}
