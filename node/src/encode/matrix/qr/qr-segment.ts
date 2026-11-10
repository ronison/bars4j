import type { QrBitStream } from "./qr-bit-stream.ts";
import type { QrMode } from "./qr-mode.ts";
import { charCountBitsForVersion } from "./qr-mode.ts";

/**
 * A run of data encoded in a single {@link QrMode}.
 *
 * @param mode      the encoding mode of the segment
 * @param values    the packed values (digit groups, alphanumeric pairs or raw bytes)
 * @param bitWidths bit width of each value in {@code values}
 * @param charCount number of input characters represented by this segment
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrSegment} record.
 */
export interface QrSegment {
  readonly mode: QrMode;
  readonly values: readonly number[];
  readonly bitWidths: readonly number[];
  readonly charCount: number;
}

/** Factory for {@link QrSegment} (Java record constructor). */
export function qrSegment(mode: QrMode, values: number[], bitWidths: number[], charCount: number): QrSegment {
  return { mode, values, bitWidths, charCount };
}

/** Number of bits used by the data portion (excluding mode and character count indicators). */
export function dataBitLength(segment: QrSegment): number {
  let total = 0;
  for (const width of segment.bitWidths) {
    total += width;
  }
  return total;
}

/** Writes mode indicator, character count and data into the given stream. */
export function appendTo(segment: QrSegment, stream: QrBitStream, version: number): void {
  stream.append(segment.mode.indicatorBits, 4);
  stream.append(segment.charCount, charCountBitsForVersion(segment.mode, version));
  for (let i = 0; i < segment.values.length; i++) {
    stream.append(segment.values[i], segment.bitWidths[i]);
  }
}
