package core.message.wallet;

import core.message.Message;

import java.util.Base64;

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
        return "error=".concat(
                new String(
                        Base64.getEncoder().encode(getMessage().getBytes())
                ));
    }
}
