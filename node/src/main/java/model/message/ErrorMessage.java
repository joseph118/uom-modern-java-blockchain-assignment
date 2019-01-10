package model.message;

public class ErrorMessage extends Message {
    public ErrorMessage(String message) {
        super(message);
    }

    public String getErrorMessage() {
        return "error=".concat(getMessage());
    }
}
