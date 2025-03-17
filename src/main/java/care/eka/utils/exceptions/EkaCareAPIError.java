package care.eka.utils.exceptions;

/**
 * Exception raised when the Eka Care API returns an error.
 */


public class EkaCareAPIError extends EkaCareError {
    /**
     * Create a new EkaCareAPIError.
     *
     * @param message Error message
     */
    public EkaCareAPIError(String message) {
        super(message);
    }

    /**
     * Create a new EkaCareAPIError with a cause.
     *
     * @param message Error message
     * @param cause Cause of the error
     */
    public EkaCareAPIError(String message, Throwable cause) {
        super(message, cause);
    }
}
