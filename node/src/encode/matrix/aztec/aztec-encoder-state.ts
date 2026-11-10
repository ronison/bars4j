import { AztecBitBuffer } from "./aztec-bit-buffer.ts";
import { AztecMode, modeCodeWidth } from "./aztec-mode.ts";
import type { AztecMode as AztecModeType } from "./aztec-mode.ts";
import * as AztecModeTables from "./aztec-mode-tables.ts";

/**
 * One candidate reached by the Aztec high-level encoder.
 *
 * <p>A state is immutable and records four things: the mode the stream is currently left in, the
 * number of pending bytes belonging to a binary-shift run that has been started but not yet closed,
 * the total number of bits emitted so far, and a backwards-linked chain of the output steps.
 *
 * <p>The chain is shared between states, so branching the search costs only the newly appended
 * steps. A step is either a fixed-width mode code or a run of raw input bytes emitted through a
 * binary shift.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecEncoderState}.
 */

/** Longest binary-shift run that the encoding can describe (31 implicit + 2047 explicit bytes). */
const MAX_SHIFT_BYTES = 2078;

/** A single already-decided output fragment. */
class Step {
  readonly previous: Step | null;
  readonly rawBytes: boolean;
  /** For a code: its value. For raw bytes: index of the first byte. */
  readonly value: number;
  /** For a code: its width in bits. For raw bytes: the number of bytes. */
  readonly width: number;

  constructor(previous: Step | null, rawBytes: boolean, value: number, width: number) {
    this.previous = previous;
    this.rawBytes = rawBytes;
    this.value = value;
    this.width = width;
  }
}

export class AztecEncoderState {
  private readonly steps: Step | null;
  private readonly modeValue: AztecModeType;
  private readonly pendingShiftBytesValue: number;
  private readonly bitCountValue: number;
  private readonly shiftCost: number;

  private constructor(
    steps: Step | null,
    mode: AztecModeType,
    pendingShiftBytes: number,
    bitCount: number,
  ) {
    this.steps = steps;
    this.modeValue = mode;
    this.pendingShiftBytesValue = pendingShiftBytes;
    this.bitCountValue = bitCount;
    this.shiftCost = AztecEncoderState.binaryShiftCost(pendingShiftBytes);
  }

  /** Returns the empty state: nothing emitted, in UPPER mode. */
  static initial(): AztecEncoderState {
    return new AztecEncoderState(null, AztecMode.UPPER, 0, 0);
  }

  mode(): AztecModeType {
    return this.modeValue;
  }

  pendingShiftBytes(): number {
    return this.pendingShiftBytesValue;
  }

  bitCount(): number {
    return this.bitCountValue;
  }

  /**
   * Emits the {@code FLG(n)} escape used to announce an ECI. A negative {@code eci} selects the
   * FNC1 flag instead of a digit string.
   */
  announceCharset(eci: number): AztecEncoderState {
    // FLG(n) is PUNCT code 0; a temporary shift into PUNCT keeps the current mode.
    const shifted = this.appendWithShift(AztecMode.PUNCT, 0);
    if (eci < 0) {
      return shifted.append(0, 3);
    }
    if (eci > 999999) {
      throw new RangeError("ECI code must be between 0 and 999999");
    }
    const digits = String(eci);
    let state = shifted.append(digits.length, 3);
    for (let i = 0; i < digits.length; i++) {
      state = state.append(digits.charCodeAt(i) - 0x30 + 2, 4);
    }
    return state;
  }

  /** Latches permanently to {@code target} (if needed) and appends {@code value} there. */
  switchAndAppend(target: AztecModeType, value: number): AztecEncoderState {
    let newSteps = this.steps;
    let newBits = this.bitCountValue;
    if (target !== this.modeValue) {
      newSteps = AztecEncoderState.latch(newSteps, this.modeValue, target);
      newBits += AztecModeTables.latchBitCount(this.modeValue, target);
    }
    newSteps = new Step(newSteps, false, value, modeCodeWidth(target));
    return new AztecEncoderState(newSteps, target, 0, newBits + modeCodeWidth(target));
  }

  /** Temporarily shifts to {@code target} for one {@code value} and returns to the current mode. */
  appendWithShift(target: AztecModeType, value: number): AztecEncoderState {
    const shiftWidth = modeCodeWidth(this.modeValue);
    const shifted = new Step(
      this.steps,
      false,
      AztecModeTables.shiftCode(this.modeValue, target),
      shiftWidth,
    );
    const code = new Step(shifted, false, value, 5);
    return new AztecEncoderState(code, this.modeValue, 0, this.bitCountValue + shiftWidth + 5);
  }

  /** Extends the open binary-shift run by the input byte at {@code index}. */
  addShiftedByte(index: number): AztecEncoderState {
    let newSteps = this.steps;
    let newMode = this.modeValue;
    let newBits = this.bitCountValue;
    if (this.modeValue === AztecMode.PUNCT || this.modeValue === AztecMode.DIGIT) {
      // Binary shift is not directly available from these modes; latch to UPPER first.
      newSteps = AztecEncoderState.latch(newSteps, this.modeValue, AztecMode.UPPER);
      newBits += AztecModeTables.latchBitCount(this.modeValue, AztecMode.UPPER);
      newMode = AztecMode.UPPER;
    }
    // The first byte and byte 31 of a short run open a header; byte 63 switches to the extended
    // length header, which is smaller than starting a second run.
    const deltaBits =
      this.pendingShiftBytesValue === 0 || this.pendingShiftBytesValue === 31
        ? 18
        : this.pendingShiftBytesValue === 62
          ? 9
          : 8;
    let next = new AztecEncoderState(
      newSteps,
      newMode,
      this.pendingShiftBytesValue + 1,
      newBits + deltaBits,
    );
    if (next.pendingShiftBytesValue === MAX_SHIFT_BYTES) {
      next = next.closeShift(index + 1);
    }
    return next;
  }

  /** Closes any open binary-shift run, recording the range of input bytes it covers. */
  closeShift(index: number): AztecEncoderState {
    if (this.pendingShiftBytesValue === 0) {
      return this;
    }
    const run = new Step(
      this.steps,
      true,
      index - this.pendingShiftBytesValue,
      this.pendingShiftBytesValue,
    );
    return new AztecEncoderState(run, this.modeValue, 0, this.bitCountValue);
  }

  /**
   * Returns {@code true} if this state can be no worse than {@code other} for every possible
   * continuation, so that {@code other} may be discarded. The comparison anticipates the latch
   * needed to reach the other state's mode and the worst-case cost of closing or extending the
   * pending binary-shift runs.
   */
  dominates(other: AztecEncoderState): boolean {
    let projected =
      this.bitCountValue + AztecModeTables.latchBitCount(this.modeValue, other.modeValue);
    if (this.pendingShiftBytesValue < other.pendingShiftBytesValue) {
      projected += other.shiftCost - this.shiftCost;
    } else if (
      this.pendingShiftBytesValue > other.pendingShiftBytesValue &&
      other.pendingShiftBytesValue > 0
    ) {
      // This state carries more open shift bytes and may have to pay for an extra header.
      projected += 10;
    }
    return projected <= other.bitCountValue;
  }

  /** Serialises the chain of steps, reading the raw bytes of a binary shift from {@code input}. */
  toBitBuffer(input: Uint8Array): AztecBitBuffer {
    const reversed: Step[] = [];
    for (
      let step = this.closeShift(input.length).steps;
      step !== null;
      step = step.previous
    ) {
      reversed.push(step);
    }
    const out = new AztecBitBuffer();
    for (let i = reversed.length - 1; i >= 0; i--) {
      const step = reversed[i];
      if (step.rawBytes) {
        AztecEncoderState.writeBinaryShift(out, input, step.value, step.width);
      } else {
        out.appendBits(step.value, step.width);
      }
    }
    return out;
  }

  /** Appends a code without changing the mode or disturbing a pending shift. */
  private append(value: number, width: number): AztecEncoderState {
    const step = new Step(this.steps, false, value, width);
    return new AztecEncoderState(step, this.modeValue, 0, this.bitCountValue + width);
  }

  /** Pushes the codes of a latch onto a step chain, in emission order. */
  private static latch(head: Step | null, from: AztecModeType, to: AztecModeType): Step | null {
    const codes = AztecModeTables.latchCodes(from, to);
    let result = head;
    for (let i = 0; i < codes.length; i += 2) {
      result = new Step(result, false, codes[i], codes[i + 1]);
    }
    return result;
  }

  private static writeBinaryShift(
    out: AztecBitBuffer,
    input: Uint8Array,
    start: number,
    byteCount: number,
  ): void {
    for (let i = 0; i < byteCount; i++) {
      if (i === 0 || (i === 31 && byteCount <= 62)) {
        // A binary-shift header: the B/S code, then a length field. Runs longer than 62
        // bytes use the 16-bit extended length; otherwise a five-bit length follows the
        // B/S code, and a second header opens byte 31 when needed.
        out.appendBits(31, 5);
        if (byteCount > 62) {
          out.appendBits(byteCount - 31, 16);
        } else if (i === 0) {
          out.appendBits(Math.min(byteCount, 31), 5);
        } else {
          out.appendBits(byteCount - 31, 5);
        }
      }
      out.appendBits(input[start + i] & 0xff, 8);
    }
  }

  /** Rough number of header bits an open run of {@code byteCount} bytes will need. */
  private static binaryShiftCost(byteCount: number): number {
    if (byteCount > 62) {
      return 21;
    }
    if (byteCount > 31) {
      return 20;
    }
    return byteCount > 0 ? 10 : 0;
  }
}
