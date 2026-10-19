package org.bars4j.encode.api;

/**
 * Thrown when the input cannot be represented by the target barcode format
 * (wrong length, unsupported characters, value out of range, ...).
 *
 * <p>Replaces the legacy {@code org.bars4j.encode.InvalidAtributeException} typo while keeping
 * the old type working as an alias.
 */
public class InvalidSymbolException extends BarcodeException {

    private static final long serialVersionUID = 1L;

    public InvalidSymbolException(String message) {
        super(message);
    }

    public InvalidSymbolException(String message, Throwable cause) {
        super(message, cause);
    }
}
