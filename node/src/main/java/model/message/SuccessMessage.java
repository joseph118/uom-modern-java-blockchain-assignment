package model.message;

public class SuccessMessage extends Message {
    private final boolean closeConnection;
    public SuccessMessage(String message, boolean closeConnection) {
        super(message);

        this.closeConnection = closeConnection;
    }

    public boolean getCloseConnection() {
        return closeConnection;
    }
}
