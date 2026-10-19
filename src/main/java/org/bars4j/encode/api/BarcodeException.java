package org.bars4j.encode.api;

/**
 * Base exception for all barcode encoding/decoding failures.
 */
public class BarcodeException extends Exception {

    private static final long serialVersionUID = 1L;

    public BarcodeException(String message) {
        super(message);
    }

    public BarcodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
