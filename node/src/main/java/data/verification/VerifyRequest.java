package data.verification;

import java.util.Map;

public class VerifyRequest {
    private final String nodeName;
    private final String guid;
    private final String senderKey;
    private final String receiverKey;
    private final String amountString;
    private final String senderSignature;
    private final String timestamp;
    private final String hash;
    private final String signature;

    public VerifyRequest(Map<String, String> requestMessage) {
        this.nodeName = requestMessage.get("nodename");
        this.guid = requestMessage.get("guid");
        this.senderKey = requestMessage.get("senderkey");
        this.receiverKey = requestMessage.get("receiverkey");
        this.amountString = requestMessage.get("amount");
        this.senderSignature = requestMessage.get("sendersignature");
        this.timestamp = requestMessage.get("timestamp");
        this.hash = requestMessage.get("hash");
        this.signature = requestMessage.get("signature");
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getGuid() {
        return guid;
    }

    public String getSenderKey() {
        return senderKey;
    }

    public String getReceiverKey() {
        return receiverKey;
    }

    public String getAmountString() {
        return amountString;
    }

    public String getSenderSignature() {
        return senderSignature;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public String toString() {
        return "VerifyRequest{" +
                "nodeName='" + nodeName + '\'' +
                ", guid='" + guid + '\'' +
                ", senderKey='" + senderKey + '\'' +
                ", receiverKey='" + receiverKey + '\'' +
                ", amountString='" + amountString + '\'' +
                ", senderSignature='" + senderSignature + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", hash='" + hash + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
