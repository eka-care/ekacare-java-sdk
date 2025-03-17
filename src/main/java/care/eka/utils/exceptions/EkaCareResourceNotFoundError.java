package care.eka.utils.exceptions;



/**
 * Exception raised when a requested resource is not found.
 */



public class EkaCareResourceNotFoundError extends EkaCareError {
    /**
     * Create a new EkaCareResourceNotFoundError.
     *
     * @param message Error message
     */
    public EkaCareResourceNotFoundError(String message) {
        super(message);
    }

    /**
     * Create a new EkaCareResourceNotFoundError with a cause.
     *
     * @param message Error message
     * @param cause Cause of the error
     */
    public EkaCareResourceNotFoundError(String message, Throwable cause) {
        super(message, cause);
    }
}