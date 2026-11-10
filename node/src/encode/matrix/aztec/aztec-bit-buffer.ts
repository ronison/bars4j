/**
 * Growable buffer of bits used while assembling the Aztec bit stream. Bits are appended most
 * significant bit first, so that a word written with {@link appendBits} reads back in the natural
 * left-to-right order.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecBitBuffer}.
 */
export class AztecBitBuffer {
  private bytes: Uint8Array = new Uint8Array(32);
  private bitCountValue = 0;

  /** Returns the number of bits written so far. */
  size(): number {
    return this.bitCountValue;
  }

  /** Returns bit {@code index} ({@code true} = one). */
  get(index: number): boolean {
    if (index < 0 || index >= this.bitCountValue) {
      throw new RangeError(`bit index ${index} out of range [0,${this.bitCountValue})`);
    }
    return (this.bytes[(index / 8) | 0] & (1 << (7 - (index % 8)))) !== 0;
  }

  /** Appends a single bit. */
  appendBit(bit: boolean): void {
    this.ensureCapacity(1);
    if (bit) {
      this.bytes[(this.bitCountValue / 8) | 0] |= 1 << (7 - (this.bitCountValue % 8));
    }
    this.bitCountValue++;
  }

  /** Appends the low {@code numBits} of {@code value}, most significant bit first. */
  appendBits(value: number, numBits: number): void {
    if (numBits < 0 || numBits > 32) {
      throw new RangeError(`numBits out of range: ${numBits}`);
    }
    this.ensureCapacity(numBits);
    for (let i = numBits - 1; i >= 0; i--) {
      this.appendBit(((value >>> i) & 1) !== 0);
    }
  }

  private ensureCapacity(additionalBits: number): void {
    const neededBytes = Math.trunc((this.bitCountValue + additionalBits + 7) / 8);
    if (neededBytes > this.bytes.length) {
      const grown = new Uint8Array(Math.max(neededBytes, this.bytes.length * 2));
      grown.set(this.bytes);
      this.bytes = grown;
    }
  }
}
