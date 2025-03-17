package care.eka.utils.exceptions;

/**
 * Exception raised when input validation fails.
 */


public class EkaCareValidationError extends EkaCareError {
    /**
     * Create a new EkaCareValidationError.
     *
     * @param message Error message
     */
    public EkaCareValidationError(String message) {
        super(message);
    }

    /**
     * Create a new EkaCareValidationError with a cause.
     *
     * @param message Error message
     * @param cause Cause of the error
     */
    public EkaCareValidationError(String message, Throwable cause) {
        super(message, cause);
    }
}
