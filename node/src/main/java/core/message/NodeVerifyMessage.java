package core.message;

import model.ServerNode;

public class NodeVerifyMessage extends Message  {
        private final ServerNode serverNode;
        private final String senderPublicKey;

    public NodeVerifyMessage(String message, ServerNode serverNode, String senderPublicKey) {
        super(message);
        this.serverNode = serverNode;
        this.senderPublicKey = senderPublicKey;
    }

    public ServerNode getServerNode() {
            return serverNode;
        }

    public String getSenderPublicKey() {
        return senderPublicKey;
    }
}
