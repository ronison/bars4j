import type { AztecField } from "./aztec-field.ts";

/**
 * Reed-Solomon check-word generation for Aztec symbols.
 *
 * <p>Aztec appends its check words as the remainder of a polynomial division. With {@code n} check
 * words the divisor is {@code g(x) = (x + a^1)(x + a^2)...(x + a^n)} over the symbol's field, and
 * the data words are multiplied by {@code x^n} before the division, which is implemented here as a
 * running synthetic division.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecReedSolomon}.
 */

/**
 * Fills the last {@code checkWords} slots of {@code words} with the check words of the leading
 * {@code words.length - checkWords} data words. The data words themselves are left untouched.
 */
export function appendCheckWords(words: number[], checkWords: number, field: AztecField): void {
  const dataWords = words.length - checkWords;
  const divisor = generator(field, checkWords);

  const division = new Array<number>(words.length).fill(0);
  for (let i = 0; i < dataWords; i++) {
    division[i] = words[i];
  }
  for (let i = 0; i < dataWords; i++) {
    const leading = division[i];
    if (leading !== 0) {
      for (let j = 0; j < divisor.length; j++) {
        division[i + j] ^= field.multiply(divisor[j], leading);
      }
    }
  }
  for (let i = 0; i < checkWords; i++) {
    words[dataWords + i] = division[dataWords + i];
  }
}

/** Builds {@code (x + a^1)...(x + a^n)} as highest-degree-first coefficients. */
function generator(field: AztecField, checkWords: number): number[] {
  let polynomial = [1];
  for (let root = 1; root <= checkWords; root++) {
    polynomial = multiply(field, polynomial, [1, field.exp(root)]);
  }
  return polynomial;
}

/** Multiplies two highest-degree-first polynomials over {@code field}. */
function multiply(field: AztecField, a: number[], b: number[]): number[] {
  const product = new Array<number>(a.length + b.length - 1).fill(0);
  for (let i = 0; i < a.length; i++) {
    if (a[i] === 0) {
      continue;
    }
    for (let j = 0; j < b.length; j++) {
      product[i + j] ^= field.multiply(a[i], b[j]);
    }
  }
  return product;
}
