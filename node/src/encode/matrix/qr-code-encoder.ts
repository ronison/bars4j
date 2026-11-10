import type { MatrixEncoder } from "../api/matrix-encoder.ts";
import { UnsupportedTextException } from "../api/unsupported-text-exception.ts";
import { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import { QrBitStream } from "./qr/qr-bit-stream.ts";
import { interleave } from "./qr/qr-data-blocks.ts";
import type { QrEccLevel } from "./qr/qr-ecc-level.ts";
import { QrEccLevel as EccLevel } from "./qr/qr-ecc-level.ts";
import { build } from "./qr/qr-matrix-builder.ts";
import type { QrCharset } from "./qr/qr-segment-encoder.ts";
import { QrSegmentEncoder, defaultCharset } from "./qr/qr-segment-encoder.ts";
import { appendTo, dataBitLength } from "./qr/qr-segment.ts";
import type { QrSegment } from "./qr/qr-segment.ts";
import type { QrVersion } from "./qr/qr-version.ts";
import { getVersionForNumber, maxVersion, totalDataCodewords } from "./qr/qr-version.ts";
import { charCountBitsForVersion } from "./qr/qr-mode.ts";

/**
 * Self-contained QRCode encoder (versions 1-40, ECC levels L/M/Q/H, numeric/alphanumeric/byte
 * modes). No runtime dependency on any external QR library.
 *
 * Port of {@code org.bars4j.encode.matrix.QRCodeEncoder}.
 */
export class QRCodeEncoder implements MatrixEncoder {
  private static readonly QUIET_ZONE = 4;

  private readonly level: QrEccLevel;
  private readonly textCharset: QrCharset;
  private readonly forcedVersion: number | null;

  constructor(eccLevel: QrEccLevel = EccLevel.M, charset: QrCharset = defaultCharset(), forcedVersion: number | null = null) {
    this.level = eccLevel;
    this.textCharset = charset;
    this.forcedVersion = forcedVersion;
  }

  eccLevel(): QrEccLevel {
    return this.level;
  }

  encode(text: string): MatrixSymbol {
    const segment = QrSegmentEncoder.encode(text, this.textCharset);
    const version = this.chooseVersion(segment);
    const ecBlocks = version.ecBlocks(this.level);

    const stream = new QrBitStream();
    appendTo(segment, stream, version.versionNumber());
    stream.padToCapacity(totalDataCodewords(ecBlocks));

    const dataCodewords = stream.toCodewords();
    const codewords = interleave(dataCodewords, ecBlocks);
    const modules = build(version, this.level, codewords);
    return new MatrixSymbol(modules, QRCodeEncoder.QUIET_ZONE);
  }

  computeCheckSum(_text: string): string {
    // QRCode error correction is computed internally, there is no external check digit.
    return "";
  }

  private chooseVersion(segment: QrSegment): QrVersion {
    if (this.forcedVersion !== null) {
      const version = getVersionForNumber(this.forcedVersion);
      if (requiredBits(segment, version.versionNumber()) > totalDataCodewords(version.ecBlocks(this.level)) * 8) {
        throw new UnsupportedTextException(
          "Data does not fit in forced version " + this.forcedVersion + " at level " + this.level,
        );
      }
      return version;
    }
    for (let number = 1; number <= maxVersion(); number++) {
      const version = getVersionForNumber(number);
      if (requiredBits(segment, number) <= totalDataCodewords(version.ecBlocks(this.level)) * 8) {
        return version;
      }
    }
    throw new UnsupportedTextException("Data too large for QRCode versions 1-" + maxVersion());
  }
}

function requiredBits(segment: QrSegment, version: number): number {
  return 4 + charCountBitsForVersion(segment.mode, version) + dataBitLength(segment);
}
