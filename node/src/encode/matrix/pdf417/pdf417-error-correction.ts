/**
 * Reed-Solomon check codeword generation for PDF417, following clause 4.10 and Annex F of
 * ISO/IEC 15438.
 *
 * <p>PDF417 works over the prime field with 929 elements and primitive element 3. A chosen error
 * correction level with {@code k} check codewords uses the generator polynomial
 * {@code g(x) = (x - 3)(x - 9) ... (x - 3^k)}, reduced modulo 929. Rather than carrying those
 * coefficients as a table, this module derives {@code g(x)} from the field itself, once per level,
 * and then obtains the check codewords as the negative remainder of the message polynomial shifted
 * by {@code k} and divided by {@code g(x)}.
 *
 * <p>The correction information is internal to the symbol: it is not an externally visible check
 * digit, so no caller-facing checksum is produced here.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417ErrorCorrection}.
 */

/** Modulus of the PDF417 field. */
const FIELD_MODULUS = 929;

/** Primitive element of the field; its powers enumerate every non-zero field element. */
const FIELD_GENERATOR = 3;

/** Highest error correction level described by the standard. */
export const MAX_LEVEL = 8;

/**
 * Generator polynomials indexed by error correction level. Entry {@code level} holds the
 * coefficients of {@code g(x)} in ascending order of degree, from the constant term up to the
 * leading coefficient, which is always one.
 */
const GENERATOR_POLYNOMIALS: number[][] = buildGeneratorPolynomials();

function buildGeneratorPolynomials(): number[][] {
  const polynomials: number[][] = new Array<number[]>(MAX_LEVEL + 1);
  for (let level = 0; level <= MAX_LEVEL; level++) {
    polynomials[level] = buildGeneratorPolynomial(codewordCount(level));
  }
  return polynomials;
}

/**
 * Number of check codewords for an error correction level.
 *
 * @param level error correction level, 0-8
 * @return the number of check codewords, {@code 2^(level + 1)}
 */
export function codewordCount(level: number): number {
  if (level < 0 || level > MAX_LEVEL) {
    throw new Error("Error correction level must be between 0 and 8");
  }
  return 1 << (level + 1);
}

/**
 * Multiplies out {@code (x - 3^1) ... (x - 3^roots)} over the PDF417 field.
 *
 * @param roots number of roots, equal to the number of check codewords
 * @return coefficients in ascending order of degree, length {@code roots + 1}
 */
function buildGeneratorPolynomial(roots: number): number[] {
  let polynomial: number[] = [1];
  let root = 1;
  for (let i = 0; i < roots; i++) {
    root = (root * FIELD_GENERATOR) % FIELD_MODULUS;
    const grown = new Array<number>(polynomial.length + 1).fill(0);
    for (let degree = 0; degree < polynomial.length; degree++) {
      grown[degree] = reduce(grown[degree] - polynomial[degree] * root);
      grown[degree + 1] = reduce(grown[degree + 1] + polynomial[degree]);
    }
    polynomial = grown;
  }
  return polynomial;
}

/**
 * Computes the check codewords that complete a block of data codewords.
 *
 * <p>The data codewords form a message polynomial whose first entry is the highest-degree
 * coefficient. Shifting it by the number of check codewords and taking the remainder modulo the
 * generator polynomial yields the error-detecting residue; the negated remainder, most
 * significant coefficient first, is what the standard appends to the data region.
 *
 * @param dataCodewords data codewords, each in the range 0-928
 * @param level         error correction level, 0-8
 * @return the check codewords in the order they follow the data
 */
export function generate(dataCodewords: number[], level: number): number[] {
  const checkCount = codewordCount(level);
  const generator = GENERATOR_POLYNOMIALS[level];

  const dividend = new Array<number>(dataCodewords.length + checkCount).fill(0);
  for (let i = 0; i < dataCodewords.length; i++) {
    dividend[checkCount + dataCodewords.length - 1 - i] = reduce(dataCodewords[i]);
  }

  // Synthetic division by the monic generator: cancel each high coefficient in turn.
  for (let degree = dividend.length - 1; degree >= checkCount; degree--) {
    const coefficient = dividend[degree];
    if (coefficient === 0) {
      continue;
    }
    const offset = degree - checkCount;
    for (let j = 0; j <= checkCount; j++) {
      dividend[offset + j] = reduce(dividend[offset + j] - coefficient * generator[j]);
    }
  }

  const check = new Array<number>(checkCount).fill(0);
  for (let i = 0; i < checkCount; i++) {
    check[i] = reduce(-dividend[checkCount - 1 - i]);
  }
  return check;
}

/** Normalises a field value into {@code [0, FIELD_MODULUS)}. */
function reduce(value: number): number {
  const remainder = value % FIELD_MODULUS;
  return remainder < 0 ? remainder + FIELD_MODULUS : remainder;
}
