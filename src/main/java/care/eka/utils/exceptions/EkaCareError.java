package care.eka.utils.exceptions;

/**
 * Base exception for all Eka Care SDK errors.
 */

public class EkaCareError extends RuntimeException {
    /**
     * Create a new EkaCareError.
     *
     * @param message Error message
     */
    public EkaCareError(String message) {
        super(message);
    }

    /**
     * Create a new EkaCareError with a cause.
     *
     * @param message Error message
     * @param cause Cause of the error
     */
    public EkaCareError(String message, Throwable cause) {
        super(message, cause);
    }
}
