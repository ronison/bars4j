import { QrEccLevel } from "./qr-ecc-level.ts";

/**
 * QRCode version metadata (versions 1-40).
 *
 * <p>Holds the symbol size, alignment pattern centres and the error-correction block layout per
 * ECC level. Data source: ISO/IEC 18004.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrVersion}.
 */

/** One block group: {@code count} blocks with {@code dataCodewords} data codewords each. */
export interface EcBlock {
  readonly count: number;
  readonly dataCodewords: number;
}

/** Factory for {@link EcBlock} (Java {@code QrVersion.EcBlock} record). */
export function ecBlock(count: number, dataCodewords: number): EcBlock {
  return { count, dataCodewords };
}

/** Error-correction layout for one ECC level of one version. */
export interface EcBlocks {
  readonly ecCodewordsPerBlock: number;
  readonly blocks: readonly EcBlock[];
}

/** Factory for {@link EcBlocks} (Java {@code QrVersion.EcBlocks} record). */
export function ecBlocks(ecCodewordsPerBlock: number, ...blocks: EcBlock[]): EcBlocks {
  return { ecCodewordsPerBlock, blocks };
}

export function numBlocks(ec: EcBlocks): number {
  let total = 0;
  for (const block of ec.blocks) {
    total += block.count;
  }
  return total;
}

export function totalDataCodewords(ec: EcBlocks): number {
  let total = 0;
  for (const block of ec.blocks) {
    total += block.count * block.dataCodewords;
  }
  return total;
}

export class QrVersion {
  private readonly numberValue: number;
  private readonly sizeValue: number;
  private readonly alignmentCenters: number[];
  private readonly ecBlocksByLevel: readonly EcBlocks[];

  constructor(versionNumber: number, alignmentPatternCenters: number[], ecBlocksByLevel: EcBlocks[]) {
    this.numberValue = versionNumber;
    this.sizeValue = versionNumber * 4 + 17;
    this.alignmentCenters = alignmentPatternCenters;
    this.ecBlocksByLevel = ecBlocksByLevel;
  }

  versionNumber(): number {
    return this.numberValue;
  }

  size(): number {
    return this.sizeValue;
  }

  alignmentPatternCenters(): number[] {
    return this.alignmentCenters.slice();
  }

  ecBlocks(level: QrEccLevel): EcBlocks {
    return this.ecBlocksByLevel[level.ordinal];
  }

  /** Number of unused remainder bits at the end of the data area. */
  remainderBits(): number {
    if (this.numberValue === 1) {
      return 0;
    }
    if (this.numberValue <= 6) {
      return 7;
    }
    if (this.numberValue <= 13) {
      return 0;
    }
    if (this.numberValue <= 20) {
      return 3;
    }
    if (this.numberValue <= 27) {
      return 4;
    }
    if (this.numberValue <= 34) {
      return 3;
    }
    return 0;
  }
}

function eb(ecPerBlock: number, ...blocks: EcBlock[]): EcBlocks {
  return ecBlocks(ecPerBlock, ...blocks);
}

function b(count: number, dataCodewords: number): EcBlock {
  return ecBlock(count, dataCodewords);
}

const VERSIONS: QrVersion[] = [
  new QrVersion(1, [], [eb(7, b(1, 19)), eb(10, b(1, 16)), eb(13, b(1, 13)), eb(17, b(1, 9))]),
  new QrVersion(2, [6, 18], [eb(10, b(1, 34)), eb(16, b(1, 28)), eb(22, b(1, 22)), eb(28, b(1, 16))]),
  new QrVersion(3, [6, 22], [eb(15, b(1, 55)), eb(26, b(1, 44)), eb(18, b(2, 17)), eb(22, b(2, 13))]),
  new QrVersion(4, [6, 26], [eb(20, b(1, 80)), eb(18, b(2, 32)), eb(26, b(2, 24)), eb(16, b(4, 9))]),
  new QrVersion(5, [6, 30], [eb(26, b(1, 108)), eb(24, b(2, 43)), eb(18, b(2, 15), b(2, 16)), eb(22, b(2, 11), b(2, 12))]),
  new QrVersion(6, [6, 34], [eb(18, b(2, 68)), eb(16, b(4, 27)), eb(24, b(4, 19)), eb(28, b(4, 15))]),
  new QrVersion(7, [6, 22, 38], [eb(20, b(2, 78)), eb(18, b(4, 31)), eb(18, b(2, 14), b(4, 15)), eb(26, b(4, 13), b(1, 14))]),
  new QrVersion(8, [6, 24, 42], [eb(24, b(2, 97)), eb(22, b(2, 38), b(2, 39)), eb(22, b(4, 18), b(2, 19)), eb(26, b(4, 14), b(2, 15))]),
  new QrVersion(9, [6, 26, 46], [eb(30, b(2, 116)), eb(22, b(3, 36), b(2, 37)), eb(20, b(4, 16), b(4, 17)), eb(24, b(4, 12), b(4, 13))]),
  new QrVersion(10, [6, 28, 50], [eb(18, b(2, 68), b(2, 69)), eb(26, b(4, 43), b(1, 44)), eb(24, b(6, 19), b(2, 20)), eb(28, b(6, 15), b(2, 16))]),
  new QrVersion(11, [6, 30, 54], [eb(20, b(4, 81)), eb(30, b(1, 50), b(4, 51)), eb(28, b(4, 22), b(4, 23)), eb(24, b(3, 12), b(8, 13))]),
  new QrVersion(12, [6, 32, 58], [eb(24, b(2, 92), b(2, 93)), eb(22, b(6, 36), b(2, 37)), eb(26, b(4, 20), b(6, 21)), eb(28, b(7, 14), b(4, 15))]),
  new QrVersion(13, [6, 34, 62], [eb(26, b(4, 107)), eb(22, b(8, 37), b(1, 38)), eb(24, b(8, 20), b(4, 21)), eb(22, b(12, 11), b(4, 12))]),
  new QrVersion(14, [6, 26, 46, 66], [eb(30, b(3, 115), b(1, 116)), eb(24, b(4, 40), b(5, 41)), eb(20, b(11, 16), b(5, 17)), eb(24, b(11, 12), b(5, 13))]),
  new QrVersion(15, [6, 26, 48, 70], [eb(22, b(5, 87), b(1, 88)), eb(24, b(5, 41), b(5, 42)), eb(30, b(5, 24), b(7, 25)), eb(24, b(11, 12), b(7, 13))]),
  new QrVersion(16, [6, 26, 50, 74], [eb(24, b(5, 98), b(1, 99)), eb(28, b(7, 45), b(3, 46)), eb(24, b(15, 19), b(2, 20)), eb(30, b(3, 15), b(13, 16))]),
  new QrVersion(17, [6, 30, 54, 78], [eb(28, b(1, 107), b(5, 108)), eb(28, b(10, 46), b(1, 47)), eb(28, b(1, 22), b(15, 23)), eb(28, b(2, 14), b(17, 15))]),
  new QrVersion(18, [6, 30, 56, 82], [eb(30, b(5, 120), b(1, 121)), eb(26, b(9, 43), b(4, 44)), eb(28, b(17, 22), b(1, 23)), eb(28, b(2, 14), b(19, 15))]),
  new QrVersion(19, [6, 30, 58, 86], [eb(28, b(3, 113), b(4, 114)), eb(26, b(3, 44), b(11, 45)), eb(26, b(17, 21), b(4, 22)), eb(26, b(9, 13), b(16, 14))]),
  new QrVersion(20, [6, 34, 62, 90], [eb(28, b(3, 107), b(5, 108)), eb(26, b(3, 41), b(13, 42)), eb(30, b(15, 24), b(5, 25)), eb(28, b(15, 15), b(10, 16))]),
  new QrVersion(21, [6, 28, 50, 72, 94], [eb(28, b(4, 116), b(4, 117)), eb(26, b(17, 42)), eb(28, b(17, 22), b(6, 23)), eb(30, b(19, 16), b(6, 17))]),
  new QrVersion(22, [6, 26, 50, 74, 98], [eb(28, b(2, 111), b(7, 112)), eb(28, b(17, 46)), eb(30, b(7, 24), b(16, 25)), eb(24, b(34, 13))]),
  new QrVersion(23, [6, 30, 54, 78, 102], [eb(30, b(4, 121), b(5, 122)), eb(28, b(4, 47), b(14, 48)), eb(30, b(11, 24), b(14, 25)), eb(30, b(16, 15), b(14, 16))]),
  new QrVersion(24, [6, 28, 54, 80, 106], [eb(30, b(6, 117), b(4, 118)), eb(28, b(6, 45), b(14, 46)), eb(30, b(11, 24), b(16, 25)), eb(30, b(30, 16), b(2, 17))]),
  new QrVersion(25, [6, 32, 58, 84, 110], [eb(26, b(8, 106), b(4, 107)), eb(28, b(8, 47), b(13, 48)), eb(30, b(7, 24), b(22, 25)), eb(30, b(22, 15), b(13, 16))]),
  new QrVersion(26, [6, 30, 58, 86, 114], [eb(28, b(10, 114), b(2, 115)), eb(28, b(19, 46), b(4, 47)), eb(28, b(28, 22), b(6, 23)), eb(30, b(33, 16), b(4, 17))]),
  new QrVersion(27, [6, 34, 62, 90, 118], [eb(30, b(8, 122), b(4, 123)), eb(28, b(22, 45), b(3, 46)), eb(30, b(8, 23), b(26, 24)), eb(30, b(12, 15), b(28, 16))]),
  new QrVersion(28, [6, 26, 50, 74, 98, 122], [eb(30, b(3, 117), b(10, 118)), eb(28, b(3, 45), b(23, 46)), eb(30, b(4, 24), b(31, 25)), eb(30, b(11, 15), b(31, 16))]),
  new QrVersion(29, [6, 30, 54, 78, 102, 126], [eb(30, b(7, 116), b(7, 117)), eb(28, b(21, 45), b(7, 46)), eb(30, b(1, 23), b(37, 24)), eb(30, b(19, 15), b(26, 16))]),
  new QrVersion(30, [6, 26, 52, 78, 104, 130], [eb(30, b(5, 115), b(10, 116)), eb(28, b(19, 47), b(10, 48)), eb(30, b(15, 24), b(25, 25)), eb(30, b(23, 15), b(25, 16))]),
  new QrVersion(31, [6, 30, 56, 82, 108, 134], [eb(30, b(13, 115), b(3, 116)), eb(28, b(2, 46), b(29, 47)), eb(30, b(42, 24), b(1, 25)), eb(30, b(23, 15), b(28, 16))]),
  new QrVersion(32, [6, 34, 60, 86, 112, 138], [eb(30, b(17, 115)), eb(28, b(10, 46), b(23, 47)), eb(30, b(10, 24), b(35, 25)), eb(30, b(19, 15), b(35, 16))]),
  new QrVersion(33, [6, 30, 58, 86, 114, 142], [eb(30, b(17, 115), b(1, 116)), eb(28, b(14, 46), b(21, 47)), eb(30, b(29, 24), b(19, 25)), eb(30, b(11, 15), b(46, 16))]),
  new QrVersion(34, [6, 34, 62, 90, 118, 146], [eb(30, b(13, 115), b(6, 116)), eb(28, b(14, 46), b(23, 47)), eb(30, b(44, 24), b(7, 25)), eb(30, b(59, 16), b(1, 17))]),
  new QrVersion(35, [6, 30, 54, 78, 102, 126, 150], [eb(30, b(12, 121), b(7, 122)), eb(28, b(12, 47), b(26, 48)), eb(30, b(39, 24), b(14, 25)), eb(30, b(22, 15), b(41, 16))]),
  new QrVersion(36, [6, 24, 50, 76, 102, 128, 154], [eb(30, b(6, 121), b(14, 122)), eb(28, b(6, 47), b(34, 48)), eb(30, b(46, 24), b(10, 25)), eb(30, b(2, 15), b(64, 16))]),
  new QrVersion(37, [6, 28, 54, 80, 106, 132, 158], [eb(30, b(17, 122), b(4, 123)), eb(28, b(29, 46), b(14, 47)), eb(30, b(49, 24), b(10, 25)), eb(30, b(24, 15), b(46, 16))]),
  new QrVersion(38, [6, 32, 58, 84, 110, 136, 162], [eb(30, b(4, 122), b(18, 123)), eb(28, b(13, 46), b(32, 47)), eb(30, b(48, 24), b(14, 25)), eb(30, b(42, 15), b(32, 16))]),
  new QrVersion(39, [6, 26, 54, 82, 110, 138, 166], [eb(30, b(20, 117), b(4, 118)), eb(28, b(40, 47), b(7, 48)), eb(30, b(43, 24), b(22, 25)), eb(30, b(10, 15), b(67, 16))]),
  new QrVersion(40, [6, 30, 58, 86, 114, 142, 170], [eb(30, b(19, 118), b(6, 119)), eb(28, b(18, 47), b(31, 48)), eb(30, b(34, 24), b(34, 25)), eb(30, b(20, 15), b(61, 16))]),
];

export function getVersionForNumber(versionNumber: number): QrVersion {
  if (versionNumber < 1 || versionNumber > VERSIONS.length) {
    throw new Error("Unsupported version: " + versionNumber);
  }
  return VERSIONS[versionNumber - 1];
}

export function maxVersion(): number {
  return VERSIONS.length;
}

export function smallestVersionForData(dataBits: number, level: QrEccLevel): QrVersion {
  for (const version of VERSIONS) {
    if (totalDataCodewords(version.ecBlocks(level)) * 8 >= dataBits) {
      return version;
    }
  }
  throw new Error("Data too large for QRCode versions 1-" + VERSIONS.length);
}
