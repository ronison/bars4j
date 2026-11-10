import type { QrEccLevel } from "./qr-ecc-level.ts";
import type { QrVersion } from "./qr-version.ts";

/**
 * Renders a QRCode symbol: function patterns, codeword placement, data masking and the
 * BCH-protected format/version markers.
 *
 * <p>A {@code true} entry is a dark module. The layout, the eight data-mask formulas, the penalty
 * scoring and the BCH generator polynomials follow ISO/IEC 18004.
 *
 * <p>The working state is kept in flat {@code boolean[]} arrays ({@code row * size + col}) for
 * cache locality, and the same raster buffer is reused for every candidate mask.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrMatrixBuilder}.
 */

// Penalty weights for the four "undesirable feature" rules.
const RUN_WEIGHT = 3;
const BLOCK_WEIGHT = 3;
const FINDER_WEIGHT = 40;
const BALANCE_WEIGHT = 10;

// Generator polynomials and the fixed mask applied to the format bits (ISO/IEC 18004).
const FORMAT_GENERATOR = 0x537;
const FORMAT_XOR_MASK = 0x5412;
const VERSION_GENERATOR = 0x1f25;

const MASK_COUNT = 8;

export function build(version: QrVersion, eccLevel: QrEccLevel, codewords: number[]): boolean[][] {
  const size = version.size();
  const board = new Board(size);
  stampFinders(board);
  stampTiming(board);
  stampAlignment(board, version);
  stampDarkModule(board);
  reserveFormatAreas(board);
  reserveVersionAreas(board, version);
  placeCodewords(board, codewords);

  const work = new Array<boolean>(size * size);
  let bestMask = 0;
  let bestScore = Number.MAX_SAFE_INTEGER;
  for (let mask = 0; mask < MASK_COUNT; mask++) {
    maskedInto(work, board, version, eccLevel, mask);
    const score = penalty(work, size);
    if (score < bestScore) {
      bestScore = score;
      bestMask = mask;
    }
  }

  const winner = new Array<boolean>(size * size);
  maskedInto(winner, board, version, eccLevel, bestMask);
  const grid: boolean[][] = new Array(size);
  for (let row = 0; row < size; row++) {
    grid[row] = winner.slice(row * size, row * size + size);
  }
  return grid;
}

// ----- function patterns ----------------------------------------------------------------

function stampFinders(board: Board): void {
  const far = board.size - 7;
  stampFinder(board, 0, 0);
  stampFinder(board, 0, far);
  stampFinder(board, far, 0);
}

/**
 * Stamps one 7x7 finder pattern anchored at {@code (top,left)} plus its one-module light
 * separator, clipping whatever falls outside the symbol.
 */
function stampFinder(board: Board, top: number, left: number): void {
  const size = board.size;
  for (let dy = -1; dy <= 7; dy++) {
    for (let dx = -1; dx <= 7; dx++) {
      const row = top + dy;
      const col = left + dx;
      if (row < 0 || row >= size || col < 0 || col >= size) {
        continue;
      }
      const inside = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6;
      const border = dx === 0 || dx === 6 || dy === 0 || dy === 6;
      const core = dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4;
      board.fix(row, col, inside && (border || core));
    }
  }
}

/** Stamps the alternating timing modules along row 6 and column 6. */
function stampTiming(board: Board): void {
  const size = board.size;
  for (let i = 8; i <= size - 9; i++) {
    const dark = i % 2 === 0;
    board.fix(6, i, dark);
    board.fix(i, 6, dark);
  }
}

function stampAlignment(board: Board, version: QrVersion): void {
  const centers = version.alignmentPatternCenters();
  const last = centers.length - 1;
  for (let rowIndex = 0; rowIndex < centers.length; rowIndex++) {
    for (let colIndex = 0; colIndex < centers.length; colIndex++) {
      const overlapsFinder =
        (rowIndex === 0 && colIndex === 0) ||
        (rowIndex === 0 && colIndex === last) ||
        (rowIndex === last && colIndex === 0);
      if (overlapsFinder) {
        continue;
      }
      stampAlignmentPattern(board, centers[rowIndex], centers[colIndex]);
    }
  }
}

/** Stamps the 5x5 alignment pattern centred on {@code (centerRow,centerCol)}. */
function stampAlignmentPattern(board: Board, centerRow: number, centerCol: number): void {
  const size = board.size;
  for (let dy = -2; dy <= 2; dy++) {
    for (let dx = -2; dx <= 2; dx++) {
      const row = centerRow + dy;
      const col = centerCol + dx;
      if (row < 0 || row >= size || col < 0 || col >= size) {
        continue;
      }
      const dark = Math.max(Math.abs(dy), Math.abs(dx)) === 2 || (dy === 0 && dx === 0);
      board.fix(row, col, dark);
    }
  }
}

/** The mandatory dark module just above the bottom-left finder. */
function stampDarkModule(board: Board): void {
  board.fix(board.size - 8, 8, true);
}

// ----- reserved areas -------------------------------------------------------------------

/** Marks every cell occupied by the two copies of the format information. */
function reserveFormatAreas(board: Board): void {
  const size = board.size;
  // Copy 1: along column 8 above the top-left finder, then along row 8 to its left.
  for (let row = 0; row <= 8; row++) {
    if (row !== 6) {
      board.reserve(row, 8);
    }
  }
  for (let col = 0; col <= 8; col++) {
    if (col !== 6) {
      board.reserve(8, col);
    }
  }
  // Copy 2: along row 8 at the right edge, then along column 8 at the bottom edge.
  for (let i = 0; i < 8; i++) {
    board.reserve(8, size - 1 - i);
  }
  for (let i = 0; i < 7; i++) {
    board.reserve(size - 7 + i, 8);
  }
}

/** Marks the two 6x3 version-information blocks present from version 7 onwards. */
function reserveVersionAreas(board: Board, version: QrVersion): void {
  if (version.versionNumber() < 7) {
    return;
  }
  const size = board.size;
  for (let i = 0; i < 6; i++) {
    for (let j = 0; j < 3; j++) {
      board.reserve(size - 11 + j, i);
      board.reserve(i, size - 11 + j);
    }
  }
}

// ----- codeword placement ---------------------------------------------------------------

/**
 * Walks the data area in the standard two-column zig-zag: upward when the pair index is even,
 * downward when odd, skipping the vertical timing column and any function module.
 */
function placeCodewords(board: Board, codewords: number[]): void {
  const size = board.size;
  const totalBits = codewords.length * 8;
  let placed = 0;
  let rowStep = -1;
  let right = size - 1;
  while (right > 0) {
    const col = right === 6 ? 5 : right;
    for (let step = 0; step < size; step++) {
      const row = rowStep < 0 ? size - 1 - step : step;
      for (let side = 0; side < 2; side++) {
        const c = col - side;
        if (board.isFixed(row, c)) {
          continue;
        }
        let bit = false;
        if (placed < totalBits) {
          bit = ((codewords[placed >> 3] >>> (7 - (placed & 7))) & 1) !== 0;
          placed++;
        }
        board.setDark(row, c, bit);
      }
    }
    rowStep = -rowStep;
    right = col - 2;
  }
}

// ----- masking and information markers ---------------------------------------------------

/** Fills {@code out} from the board's data modules, applies {@code mask} and writes markers. */
function maskedInto(
  out: boolean[],
  board: Board,
  version: QrVersion,
  eccLevel: QrEccLevel,
  mask: number,
): void {
  const size = board.size;
  const dark = board.dark;
  const fixed = board.fixedMask;
  for (let row = 0; row < size; row++) {
    const base = row * size;
    for (let col = 0; col < size; col++) {
      const i = base + col;
      let value = dark[i];
      if (!fixed[i] && maskFlips(mask, row, col)) {
        value = !value;
      }
      out[i] = value;
    }
  }
  writeFormatInfo(out, size, eccLevel, mask);
  writeVersionInfo(out, size, version);
}

/** Data-mask condition: {@code true} where the module colour must be inverted. */
function maskFlips(mask: number, row: number, col: number): boolean {
  switch (mask) {
    case 0:
      return (row + col) % 2 === 0;
    case 1:
      return row % 2 === 0;
    case 2:
      return col % 3 === 0;
    case 3:
      return (row + col) % 3 === 0;
    case 4:
      return (Math.trunc(row / 2) + Math.trunc(col / 3)) % 2 === 0;
    case 5:
      return (row * col) % 2 + (row * col) % 3 === 0;
    case 6:
      return ((row * col) % 2 + (row * col) % 3) % 2 === 0;
    case 7:
      return ((row + col) % 2 + (row * col) % 3) % 2 === 0;
    default:
      throw new Error("Unknown mask pattern: " + mask);
  }
}

/**
 * Writes the 15 format bits (ECC level plus mask, BCH-protected and XOR-masked) around the
 * top-left finder and again along the top-right / bottom-left edges.
 */
function writeFormatInfo(grid: boolean[], size: number, eccLevel: QrEccLevel, mask: number): void {
  const summary = (eccLevel.formatBits << 3) | mask;
  const check = bchRemainder(summary, FORMAT_GENERATOR);
  const word = ((summary << 10) | check) ^ FORMAT_XOR_MASK;

  let bit = 0;
  for (let row = 0; row <= 8; row++) {
    if (row !== 6) {
      grid[row * size + 8] = bitAt(word, bit++);
    }
  }
  for (let col = 7; col >= 0; col--) {
    if (col !== 6) {
      grid[8 * size + col] = bitAt(word, bit++);
    }
  }

  bit = 0;
  for (let i = 0; i < 8; i++) {
    grid[8 * size + size - 1 - i] = bitAt(word, bit++);
  }
  for (let i = 0; i < 7; i++) {
    grid[(size - 7 + i) * size + 8] = bitAt(word, bit++);
  }
}

/** Writes the 18 version bits into the two 6x3 blocks for versions 7 and above. */
function writeVersionInfo(grid: boolean[], size: number, version: QrVersion): void {
  const number = version.versionNumber();
  if (number < 7) {
    return;
  }
  const word = (number << 12) | bchRemainder(number, VERSION_GENERATOR);
  let bit = 0;
  for (let i = 0; i < 6; i++) {
    for (let j = 0; j < 3; j++) {
      const value = bitAt(word, bit++);
      grid[(size - 11 + j) * size + i] = value;
      grid[i * size + size - 11 + j] = value;
    }
  }
}

function bitAt(word: number, index: number): boolean {
  return ((word >>> index) & 1) !== 0;
}

/**
 * Remainder of {@code value * x^degree} divided by {@code generator} over GF(2), where
 * {@code degree} is the generator's own degree. This is the BCH check value appended to the
 * format and version data.
 */
function bchRemainder(value: number, generator: number): number {
  const degree = 31 - Math.clz32(generator);
  let work = value << degree;
  for (let bit = 31; bit >= degree; bit--) {
    if ((work & (1 << bit)) !== 0) {
      work ^= generator << (bit - degree);
    }
  }
  return work;
}

// ----- penalty scoring ------------------------------------------------------------------

/**
 * All four penalty rules in a few flat passes. The dark-module count needed by rule 4 is
 * gathered for free during the row pass of rule 1.
 */
function penalty(grid: boolean[], size: number): number {
  let total = 0;
  let dark = 0;

  // Rule 1, horizontal runs (+ dark count).
  for (let row = 0; row < size; row++) {
    const base = row * size;
    let prev = grid[base];
    if (prev) {
      dark++;
    }
    let run = 1;
    for (let col = 1; col < size; col++) {
      const value = grid[base + col];
      if (value) {
        dark++;
      }
      if (value === prev) {
        run++;
      } else {
        total += runScore(run);
        run = 1;
        prev = value;
      }
    }
    total += runScore(run);
  }

  // Rule 1, vertical runs.
  for (let col = 0; col < size; col++) {
    let prev = grid[col];
    let run = 1;
    for (let row = 1; row < size; row++) {
      const value = grid[row * size + col];
      if (value === prev) {
        run++;
      } else {
        total += runScore(run);
        run = 1;
        prev = value;
      }
    }
    total += runScore(run);
  }

  // Rule 2: 2x2 same-colour blocks.
  for (let row = 0; row < size - 1; row++) {
    const base = row * size;
    for (let col = 0; col < size - 1; col++) {
      const i = base + col;
      const value = grid[i];
      if (value === grid[i + 1] && value === grid[i + size] && value === grid[i + size + 1]) {
        total += BLOCK_WEIGHT;
      }
    }
  }

  // Rule 3: finder-like 1:1:3:1:1 sequences (pattern T F T T T F T).
  for (let row = 0; row < size; row++) {
    const base = row * size;
    for (let col = 0; col + 7 <= size; col++) {
      const i = base + col;
      if (
        grid[i] &&
        !grid[i + 1] &&
        grid[i + 2] &&
        grid[i + 3] &&
        grid[i + 4] &&
        !grid[i + 5] &&
        grid[i + 6] &&
        ((col >= 4 && light(grid, i - 4, i - 1, 1)) || (col + 11 <= size && light(grid, i + 7, i + 10, 1)))
      ) {
        total += FINDER_WEIGHT;
      }
    }
  }
  for (let col = 0; col < size; col++) {
    for (let row = 0; row + 7 <= size; row++) {
      const i = row * size + col;
      if (
        grid[i] &&
        !grid[i + size] &&
        grid[i + 2 * size] &&
        grid[i + 3 * size] &&
        grid[i + 4 * size] &&
        !grid[i + 5 * size] &&
        grid[i + 6 * size] &&
        ((row >= 4 && light(grid, i - 4 * size, i - size, size)) ||
          (row + 11 <= size && light(grid, i + 7 * size, i + 10 * size, size)))
      ) {
        total += FINDER_WEIGHT;
      }
    }
  }

  // Rule 4: deviation of the dark-module proportion from 50%.
  const modules = size * size;
  const excess = Math.trunc((Math.abs(dark * 2 - modules) * 10) / modules);
  return total + excess * BALANCE_WEIGHT;
}

function runScore(run: number): number {
  return run >= 5 ? RUN_WEIGHT + (run - 5) : 0;
}

/** True when every module in the flat range {@code [from,to]} (step {@code stride}) is light. */
function light(grid: boolean[], from: number, to: number, stride: number): boolean {
  for (let i = from; i <= to; i += stride) {
    if (grid[i]) {
      return false;
    }
  }
  return true;
}

// -----------------------------------------------------------------------------------------

/** Symbol under construction: module colours plus a marker for locked (function) modules. */
class Board {
  readonly dark: boolean[];
  readonly fixedMask: boolean[];
  readonly size: number;

  constructor(size: number) {
    this.size = size;
    this.dark = new Array<boolean>(size * size).fill(false);
    this.fixedMask = new Array<boolean>(size * size).fill(false);
  }

  fix(row: number, col: number, value: boolean): void {
    const i = row * this.size + col;
    this.dark[i] = value;
    this.fixedMask[i] = true;
  }

  reserve(row: number, col: number): void {
    this.fixedMask[row * this.size + col] = true;
  }

  setDark(row: number, col: number, value: boolean): void {
    this.dark[row * this.size + col] = value;
  }

  isFixed(row: number, col: number): boolean {
    return this.fixedMask[row * this.size + col];
  }
}
