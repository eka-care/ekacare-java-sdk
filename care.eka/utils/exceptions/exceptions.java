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
