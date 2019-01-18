package core.message.wallet;

import core.message.Message;

public class ErrorMessage extends Message {
    private final String receiverName;
    public ErrorMessage(String message, String receiverName) {
        super(message);

        this.receiverName = receiverName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getErrorMessage() {
        return "error=".concat(getMessage());
    }
}
