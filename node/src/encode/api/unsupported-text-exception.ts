import { InvalidSymbolException } from "./invalid-symbol-exception.ts";

/**
 * Thrown when the input text is not supported by the selected encoding mode
 * (e.g. non-numeric text on a numeric-only barcode).
 *
 * Port of {@code org.bars4j.encode.api.UnsupportedTextException}.
 */
export class UnsupportedTextException extends InvalidSymbolException {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = "UnsupportedTextException";
  }
}
