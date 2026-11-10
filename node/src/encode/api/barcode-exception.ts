/**
 * Base exception for all barcode encoding/decoding failures.
 *
 * Port of {@code org.bars4j.encode.api.BarcodeException}.
 */
export class BarcodeException extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = "BarcodeException";
  }
}
