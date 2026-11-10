import type { MatrixEncoder } from "../api/matrix-encoder.ts";
import { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import { Pdf417Barcode } from "./pdf417/pdf417-barcode.ts";
import { Pdf417Compaction } from "./pdf417/pdf417-compaction.ts";
import { standardPdf417Dimensions } from "./pdf417/pdf417-dimensions.ts";
import { ISO_8859_1, type Pdf417Charset } from "./pdf417/pdf417-high-level-encoder.ts";

/**
 * Self-contained PDF417 encoder (ISO/IEC 15438).
 *
 * <p>Uses automatic compaction (Text/Byte/Numeric), Reed-Solomon error correction and the standard
 * codeword patterns. No runtime dependency on any external library. The default configuration is
 * error correction level 2, ISO-8859-1 input, non-compact layout and a two-module quiet zone.
 *
 * Port of {@code org.bars4j.encode.matrix.PDF417Encoder}.
 */

/** Quiet zone required on each side of a PDF417 symbol, in modules. */
const QUIET_ZONE = 2;

/** Error correction level recommended by the standard for small amounts of data. */
const DEFAULT_ERROR_CORRECTION_LEVEL = 2;

export class PDF417Encoder implements MatrixEncoder {
  private readonly ecLevel: number;
  private readonly charset: Pdf417Charset;
  private readonly compact: boolean;

  /**
   * Creates an encoder.
   *
   * @param errorCorrectionLevel error correction level, 0-8
   * @param charset              character set used for the message
   * @param compact              whether to build the compact (reduced-width) variant
   */
  constructor(
    errorCorrectionLevel: number = DEFAULT_ERROR_CORRECTION_LEVEL,
    charset: Pdf417Charset | null = ISO_8859_1,
    compact = false,
  ) {
    if (errorCorrectionLevel < 0 || errorCorrectionLevel > 8) {
      throw new Error("Error correction level must be between 0 and 8");
    }
    this.ecLevel = errorCorrectionLevel;
    this.charset = charset !== null ? charset : ISO_8859_1;
    this.compact = compact;
  }

  /** Returns the configured error correction level. */
  errorCorrectionLevel(): number {
    return this.ecLevel;
  }

  encode(text: string): MatrixSymbol {
    const barcode = new Pdf417Barcode(
      Pdf417Compaction.AUTO,
      this.charset,
      this.compact,
      standardPdf417Dimensions(),
    );
    const matrix = barcode.generateBarcodeLogic(text, this.ecLevel);
    return new MatrixSymbol(matrix.toModules(), QUIET_ZONE);
  }

  computeCheckSum(_text: string): string {
    // PDF417 error correction is internal; there is no external check digit.
    return "";
  }
}
