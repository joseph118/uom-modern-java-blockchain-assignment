package exception;

public class ArgumentsNotFoundException extends Exception {
    public ArgumentsNotFoundException() {
        super("No arguments have been found.");
    }
}
