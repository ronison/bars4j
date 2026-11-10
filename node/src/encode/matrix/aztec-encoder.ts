import type { MatrixEncoder } from "../api/matrix-encoder.ts";
import { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import { encodeAztec } from "./aztec/aztec-symbol-encoder.ts";

/**
 * Self-contained Aztec (ISO/IEC 24778) encoder.
 *
 * <p>By default it targets the minimal error correction recommended for Aztec (33%) and picks the
 * smallest symbol that fits, alternating between compact and full layouts. No runtime dependency on
 * any external library is required; the default text encoding is ISO-8859-1 (Latin-1), matching the
 * implicit Aztec character set.
 *
 * Port of {@code org.bars4j.encode.matrix.AztecEncoder}.
 */
export class AztecEncoder implements MatrixEncoder {
  /** Default minimum percentage of error correction words. */
  static readonly DEFAULT_ECC_PERCENT = 33;
  /** Quiet zone required by the Aztec standard, in modules. */
  private static readonly QUIET_ZONE = 1;

  private readonly eccPercentValue: number;
  private readonly forcedLayersValue: number | null;
  private readonly charsetValue: string | null;

  /**
   * Creates a fully configured encoder.
   *
   * @param eccPercent   minimum percentage of error correction words
   * @param forcedLayers {@code null} or {@code 0} to size automatically; a negative value forces
   *                     a compact symbol with {@code abs(value)} layers, a positive value forces
   *                     a full symbol with that many layers
   * @param charset      character set announced with an ECI, or {@code null} for no ECI (the text
   *                     is then encoded as ISO-8859-1)
   */
  constructor(
    eccPercent: number = AztecEncoder.DEFAULT_ECC_PERCENT,
    forcedLayers: number | null = null,
    charset: string | null = null,
  ) {
    this.eccPercentValue = eccPercent;
    this.forcedLayersValue = forcedLayers;
    this.charsetValue = charset;
  }

  encode(text: string): MatrixSymbol {
    const code = encodeAztec(text, this.eccPercentValue, this.forcedLayersValue, this.charsetValue);
    return new MatrixSymbol(code.modules(), AztecEncoder.QUIET_ZONE);
  }

  computeCheckSum(_text: string): string {
    // Aztec error correction is computed internally; there is no external check value.
    return "";
  }
}
