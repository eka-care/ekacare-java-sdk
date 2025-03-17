package care.eka.utils.exceptions;


/**
 * Exception raised when authentication with the Eka Care API fails.
 */


public class EkaCareAuthError extends EkaCareError {
    /**
     * Create a new EkaCareAuthError.
     *
     * @param message Error message
     */
    public EkaCareAuthError(String message) {
        super(message);
    }

    /**
     * Create a new EkaCareAuthError with a cause.
     *
     * @param message Error message
     * @param cause Cause of the error
     */
    public EkaCareAuthError(String message, Throwable cause) {
        super(message, cause);
    }
}
