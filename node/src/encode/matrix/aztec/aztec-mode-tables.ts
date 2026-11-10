import { AZTEC_MODES } from "./aztec-mode.ts";
import type { AztecMode } from "./aztec-mode.ts";

/**
 * The mode switch and character tables that drive the Aztec encodation.
 *
 * <p>Everything here is data taken from ISO/IEC 24778: the character code of each byte in each mode
 * (referred to as the character set), the sequence of codes needed to latch from one mode to
 * another, and the single codes available as temporary shifts.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecModeTables}.
 */
const MODES = AZTEC_MODES.length;

/** {@code CHARACTER_CODE[mode][byte value]} is the mode code, or 0 when the byte has none. */
const CHARACTER_CODE: number[][] = [];
for (let mode = 0; mode < MODES; mode++) {
  CHARACTER_CODE.push(new Array<number>(256).fill(0));
}

let latchTable: number[][][] = [];
let latchBits: number[][] = [];
let shiftTable: number[][] = [];

function buildCharacterSets(): void {
  // UPPER: blank and A-Z.
  CHARACTER_CODE[0][0x20] = 1;
  for (let c = 0x41; c <= 0x5a; c++) {
    CHARACTER_CODE[0][c] = c - 0x41 + 2;
  }

  // LOWER: blank and a-z.
  CHARACTER_CODE[1][0x20] = 1;
  for (let c = 0x61; c <= 0x7a; c++) {
    CHARACTER_CODE[1][c] = c - 0x61 + 2;
  }

  // DIGIT: blank, 0-9, comma and full stop. Code 14/15 are the U/L and U/S switches.
  CHARACTER_CODE[2][0x20] = 1;
  for (let c = 0x30; c <= 0x39; c++) {
    CHARACTER_CODE[2][c] = c - 0x30 + 2;
  }
  CHARACTER_CODE[2][0x2c] = 12;
  CHARACTER_CODE[2][0x2e] = 13;

  // MIXED: the C0 controls that have no dedicated mode, then a handful of ASCII signs.
  const mixedCharacters = [
    0x00, 0x20, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b,
    0x0c, 0x0d, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
    0x40, 0x5c, 0x5e, 0x5f, 0x60, 0x7c, 0x7e, 0x7f,
  ];
  for (let code = 0; code < mixedCharacters.length; code++) {
    if (code !== 0) {
      CHARACTER_CODE[3][mixedCharacters[code]] = code;
    }
  }

  // PUNCT: CR, the punctuation signs, and the bracket pairs. Codes 2-5 are the two-character
  // sequences CR LF, ". ", ", " and ": ", which the encoder handles as pairs and which have no
  // single-byte entry here.
  const punctuationCharacters = [
    0x00, 0x0d, 0x00, 0x00, 0x00, 0x00,
    0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
    0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f,
    0x5b, 0x5d, 0x7b, 0x7d,
  ];
  for (let code = 0; code < punctuationCharacters.length; code++) {
    if (punctuationCharacters[code] !== 0) {
      CHARACTER_CODE[4][punctuationCharacters[code]] = code;
    }
  }
}

function latch(from: AztecMode, to: AztecMode, ...valueWidthPairs: number[]): void {
  latchTable[from][to] = valueWidthPairs;
}

function buildLatches(): void {
  latchTable = [];
  for (let from = 0; from < MODES; from++) {
    const row: number[][] = [];
    for (let to = 0; to < MODES; to++) {
      row.push([]);
    }
    latchTable.push(row);
  }

  // Cheapest permanent switches between modes. A switch that needs two latches (for example
  // LOWER to PUNCT through MIXED) is stored as the concatenation of both codes.
  latch(0, 1, 28, 5);
  latch(0, 2, 30, 5);
  latch(0, 3, 29, 5);
  latch(0, 4, 29, 5, 30, 5);

  latch(1, 0, 30, 5, 14, 4);
  latch(1, 2, 30, 5);
  latch(1, 3, 29, 5);
  latch(1, 4, 29, 5, 30, 5);

  latch(2, 0, 14, 4);
  latch(2, 1, 14, 4, 28, 5);
  latch(2, 3, 14, 4, 29, 5);
  latch(2, 4, 14, 4, 29, 5, 30, 5);

  latch(3, 0, 29, 5);
  latch(3, 1, 28, 5);
  latch(3, 2, 29, 5, 30, 5);
  latch(3, 4, 30, 5);

  latch(4, 0, 31, 5);
  latch(4, 1, 31, 5, 28, 5);
  latch(4, 2, 31, 5, 30, 5);
  latch(4, 3, 31, 5, 29, 5);

  for (let mode = 0; mode < MODES; mode++) {
    latchTable[mode][mode] = [];
  }
}

function buildShifts(): void {
  shiftTable = [];
  for (let from = 0; from < MODES; from++) {
    shiftTable.push(new Array<number>(MODES).fill(-1));
  }
  // Only a few temporary shifts are defined: they lend the next code of another mode without
  // leaving the current one.
  shiftTable[0][4] = 0;
  shiftTable[1][4] = 0;
  shiftTable[1][0] = 28;
  shiftTable[3][4] = 0;
  shiftTable[2][4] = 0;
  shiftTable[2][0] = 15;
}

buildCharacterSets();
buildLatches();
buildShifts();

latchBits = [];
for (let from = 0; from < MODES; from++) {
  const row: number[] = [];
  for (let to = 0; to < MODES; to++) {
    let bits = 0;
    const codes = latchTable[from][to];
    for (let i = 1; i < codes.length; i += 2) {
      bits += codes[i];
    }
    row.push(bits);
  }
  latchBits.push(row);
}

/** Returns the code for {@code byteValue} in {@code mode}, or 0 when it has none. */
export function characterCode(mode: AztecMode, byteValue: number): number {
  return CHARACTER_CODE[mode][byteValue];
}

/** Returns the {@code value, width} pairs of the latch from {@code from} to {@code to}. */
export function latchCodes(from: AztecMode, to: AztecMode): number[] {
  return latchTable[from][to];
}

/** Returns the number of bits of the latch from {@code from} to {@code to}. */
export function latchBitCount(from: AztecMode, to: AztecMode): number {
  return latchBits[from][to];
}

/** Returns the temporary-shift code from {@code from} to {@code to}, or -1 when unavailable. */
export function shiftCode(from: AztecMode, to: AztecMode): number {
  return shiftTable[from][to];
}
