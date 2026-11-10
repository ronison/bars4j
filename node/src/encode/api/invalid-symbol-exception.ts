import { BarcodeException } from "./barcode-exception.ts";

/**
 * Thrown when the input cannot be represented by the target barcode format
 * (wrong length, unsupported characters, value out of range, ...).
 *
 * Port of {@code org.bars4j.encode.api.InvalidSymbolException}.
 */
export class InvalidSymbolException extends BarcodeException {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = "InvalidSymbolException";
  }
}
