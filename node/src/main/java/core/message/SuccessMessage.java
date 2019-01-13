package core.message;

public class SuccessMessage extends Message {
    private final boolean closeConnection;
    private final String receiverName;

    public SuccessMessage(String message, String receiverName, boolean closeConnection) {
        super(message);

        this.receiverName = receiverName;
        this.closeConnection = closeConnection;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public boolean getCloseConnection() {
        return closeConnection;
    }
}
