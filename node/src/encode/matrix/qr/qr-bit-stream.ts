/**
 * Append-only stream of individual bits, used to assemble the QRCode data codewords.
 *
 * <p>Bits are stored most-significant-first in a growable byte array: the first appended bit
 * occupies bit 7 of {@code bytes[0]}, the ninth bit 7 of {@code bytes[1]}, and so on. Only the
 * first {@link size} bits are meaningful.
 *
 * Port of {@code org.bars4j.encode.matrix.qr.QrBitStream}.
 */
export class QrBitStream {
  private bytes = new Uint8Array(16);
  private bitCount = 0;

  /** Number of bits appended so far. */
  size(): number {
    return this.bitCount;
  }

  /** Number of whole bytes needed to hold the appended bits. */
  sizeInBytes(): number {
    return (this.bitCount + 7) >> 3;
  }

  /** Returns the bit at {@code index}; indices outside the stream read as light ({@code false}). */
  bit(index: number): boolean {
    if (index < 0 || index >= this.bitCount) {
      return false;
    }
    return (this.bytes[index >>> 3] & (0x80 >>> (index & 7))) !== 0;
  }

  /**
   * Appends the {@code width} least-significant bits of {@code value}, most significant bit
   * first. The bits of {@code value} above {@code width} must all be zero.
   */
  append(value: number, width: number): void {
    if (width < 0 || width > 31) {
      throw new Error("bit width out of range: " + width);
    }
    if (width < 31 && value >>> width !== 0) {
      throw new Error("value " + value + " does not fit in " + width + " bits");
    }
    this.grow(this.bitCount + width);
    for (let shift = width - 1; shift >= 0; shift--) {
      if (((value >>> shift) & 1) !== 0) {
        this.bytes[this.bitCount >>> 3] |= 0x80 >>> (this.bitCount & 7);
      }
      this.bitCount++;
    }
  }

  /**
   * Grows the stream to exactly {@code capacityBytes} bytes, adding the four-bit terminator (if
   * it fits), zero bits up to the next byte boundary, then the alternating 0xEC / 0x11 pad
   * codewords until the capacity is reached.
   */
  padToCapacity(capacityBytes: number): void {
    const capacity = capacityBytes * 8;
    if (this.bitCount > capacity) {
      throw new Error("bit stream already holds " + this.bitCount + " bits, capacity is " + capacity);
    }
    this.append(0, Math.min(4, capacity - this.bitCount));
    this.append(0, -this.bitCount & 7);
    let padWithEc = true;
    while (this.bitCount < capacity) {
      this.append(padWithEc ? 0xec : 0x11, 8);
      padWithEc = !padWithEc;
    }
  }

  /** Returns the stream as codewords; the stream must end on a byte boundary. */
  toCodewords(): number[] {
    if ((this.bitCount & 7) !== 0) {
      throw new Error("bit stream is not byte aligned: " + this.bitCount + " bits");
    }
    const codewords = new Array<number>(this.bitCount >>> 3);
    for (let i = 0; i < codewords.length; i++) {
      codewords[i] = this.bytes[i] & 0xff;
    }
    return codewords;
  }

  private grow(requiredBits: number): void {
    const requiredBytes = (requiredBits + 7) >> 3;
    if (requiredBytes > this.bytes.length) {
      const next = new Uint8Array(Math.max(requiredBytes, this.bytes.length * 2));
      next.set(this.bytes);
      this.bytes = next;
    }
  }
}
