package org.bars4j.encode.api;

/**
 * Thrown when the input text is not supported by the selected encoding mode
 * (e.g. non-numeric text on a numeric-only barcode).
 */
public class UnsupportedTextException extends InvalidSymbolException {

    private static final long serialVersionUID = 1L;

    public UnsupportedTextException(String message) {
        super(message);
    }

    public UnsupportedTextException(String message, Throwable cause) {
        super(message, cause);
    }
}
