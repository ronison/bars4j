/**
 * The finite field GF(256) underlying QR Code error correction.
 *
 * <p>Field elements are bytes viewed as polynomials over GF(2): addition is bitwise XOR, and
 * multiplication is polynomial multiplication reduced modulo the primitive polynomial
 * {@code x^8 + x^4 + x^3 + x^2 + 1}. The element {@code 0x02} is primitive, so the 255 non-zero
 * elements form a cycle of powers of {@code alpha = 0x02}; multiplication therefore turns into
 * addition of exponents, which is what the two lookup tables below make cheap.
 *
 * Port of {@code org.bars4j.encode.matrix.gf256.Gf256}.
 */

/** Length of the multiplicative cycle: the non-zero elements repeat every 255 powers. */
const CYCLE = 255;

/** Lower eight coefficients of {@code x^8 + x^4 + x^3 + x^2 + 1}; the {@code x^8} term is implicit. */
const MODULUS = 0x1d;

/** {@code POWERS[k]} holds {@code alpha^k} for {@code 0 <= k < 255}. */
const POWERS = new Int32Array(CYCLE);

/** {@code EXPONENTS[v]} holds the {@code k} with {@code alpha^k = v}, for every non-zero {@code v}. */
const EXPONENTS = new Int32Array(256);

function buildTables(): void {
  let power = 1;
  for (let k = 0; k < CYCLE; k++) {
    POWERS[k] = power;
    EXPONENTS[power] = k;
    // Advance to alpha^(k+1); when degree 8 appears, reduce by the modulus and drop it.
    power <<= 1;
    if ((power & 0x100) !== 0) {
      power ^= MODULUS;
    }
    power &= 0xff;
  }
}

buildTables();

export class Gf256 {
  private constructor() {}

  /**
   * Returns {@code alpha} raised to {@code power}. Exponents are taken modulo 255, so negative
   * and out-of-range inputs are accepted and wrap around.
   */
  static exp(power: number): number {
    return POWERS[((power % CYCLE) + CYCLE) % CYCLE];
  }

  /** Returns the exponent {@code k} with {@code alpha^k = value}. Zero has no exponent. */
  static log(value: number): number {
    if (value === 0) {
      throw new Error("log(0) is undefined");
    }
    return EXPONENTS[value];
  }

  /** Multiplies two field elements. */
  static multiply(a: number, b: number): number {
    if (a === 0 || b === 0) {
      return 0;
    }
    return POWERS[(EXPONENTS[a] + EXPONENTS[b]) % CYCLE];
  }
}
