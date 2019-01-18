package data;

public class ServerNodeVerify extends ServerNode {
    private final String senderKey;

    public ServerNodeVerify(ServerNode serverNode, String senderKey) {
        super(serverNode.getName(), serverNode.getIp(), serverNode.getPort());
        this.senderKey = senderKey;
    }

    public String getSenderKey() {
        return senderKey;
    }
}
