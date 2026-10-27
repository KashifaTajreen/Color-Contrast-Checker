package contrast;

/** Demonstrates a custom checked exception for recoverable input errors. */
public final class InvalidColorException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates an exception with a user-readable explanation. */
    public InvalidColorException(String message) {
        super(message);
    }
}
