package care.eka.tools;

import care.eka.utils.exceptions.EkaCareError;

/**
 * Upload related errors for Eka File Uploader
 */


class EkaUploadError extends EkaCareError {
    public EkaUploadError(String message) {
        super(message);
    }

    public EkaUploadError(String message, Throwable cause) {
        super(message, cause);
    }
}
