import { AztecBitBuffer } from "./aztec-bit-buffer.ts";
import { eciValueFor } from "./aztec-eci.ts";
import { AztecEncoderState } from "./aztec-encoder-state.ts";
import { AztecMode, AZTEC_MODES } from "./aztec-mode.ts";
import * as AztecModeTables from "./aztec-mode-tables.ts";

/**
 * First level of Aztec encoding: converts the raw input bytes into the bit stream of mode codes,
 * temporary shifts and binary-shift runs.
 *
 * <p>The stream is grown one input symbol at a time while a frontier of candidate
 * {@link AztecEncoderState}s is kept. Every candidate can branch into the encodings allowed at the
 * next byte (a latch into a mode that holds it, a shift, or a binary shift), and candidates that can
 * never win are pruned. When the input is exhausted the cheapest surviving candidate is serialised.
 *
 * <p>Four punctuation symbols are encoded together with their following space or newline as a
 * single two-character code; those pairs are consumed in one step.
 *
 * Port of {@code org.bars4j.encode.matrix.aztec.AztecHighLevelEncoder}.
 */
export class AztecHighLevelEncoder {
  private readonly input: Uint8Array;
  private readonly charsetName: string | null;

  /** Creates an encoder for {@code input} with no ECI announcement. */
  constructor(input: Uint8Array, charsetName: string | null = null) {
    this.input = input;
    this.charsetName = charsetName;
  }

  /** Encodes the input into the first-level bit stream. */
  encode(): AztecBitBuffer {
    let initial = AztecEncoderState.initial();
    if (this.charsetName !== null) {
      initial = initial.announceCharset(eciValueFor(this.charsetName));
    }
    let frontier: AztecEncoderState[] = [initial];

    let index = 0;
    while (index < this.input.length) {
      const pairCode = this.pairCodeAt(index);
      if (pairCode !== 0) {
        frontier = this.branchOverPair(frontier, index, pairCode);
        index += 2;
      } else {
        frontier = this.branchOverByte(frontier, index);
        index++;
      }
    }

    let best = frontier[0];
    for (const candidate of frontier) {
      if (candidate.bitCount() < best.bitCount()) {
        best = candidate;
      }
    }
    return best.toBitBuffer(this.input);
  }

  /**
   * Returns the two-character punctuation code starting at {@code index}, or 0 when the byte does
   * not begin one of the four paired symbols.
   */
  private pairCodeAt(index: number): number {
    const next = index + 1 < this.input.length ? this.input[index + 1] & 0xff : -1;
    switch (this.input[index] & 0xff) {
      case 0x0d:
        return next === 0x0a ? 2 : 0;
      case 0x2e:
        return next === 0x20 ? 3 : 0;
      case 0x2c:
        return next === 0x20 ? 4 : 0;
      case 0x3a:
        return next === 0x20 ? 5 : 0;
      default:
        return 0;
    }
  }

  /** Adds the candidates that encode the single byte at {@code index}. */
  private branchOverByte(
    frontier: AztecEncoderState[],
    index: number,
  ): AztecEncoderState[] {
    const value = this.input[index] & 0xff;
    const candidates: AztecEncoderState[] = [];
    for (const state of frontier) {
      const codeInCurrentMode = AztecModeTables.characterCode(state.mode(), value);
      const definedInCurrentMode = codeInCurrentMode !== 0;

      // The open binary shift, if any, is closed only once the first non-binary candidate is
      // actually produced.
      let withoutShift: AztecEncoderState | null = null;
      for (const target of AZTEC_MODES) {
        const code = AztecModeTables.characterCode(target, value);
        if (code === 0) {
          continue;
        }
        if (withoutShift === null) {
          withoutShift = state.closeShift(index);
        }
        if (
          !definedInCurrentMode ||
          target === state.mode() ||
          target === AztecMode.DIGIT
        ) {
          // Latching is worthwhile when the byte is absent from the current mode, when it
          // stays in the same mode, or when DIGIT's cheaper codes make it pay off.
          candidates.push(withoutShift.switchAndAppend(target, code));
        }
        if (!definedInCurrentMode && AztecModeTables.shiftCode(state.mode(), target) >= 0) {
          // A shift into a mode that already holds the byte could never save bits.
          candidates.push(withoutShift.appendWithShift(target, code));
        }
      }

      if (state.pendingShiftBytes() > 0 || !definedInCurrentMode) {
        candidates.push(state.addShiftedByte(index));
      }
    }
    return AztecHighLevelEncoder.prune(candidates);
  }

  /** Adds the candidates that encode the two-character punctuation pair at {@code index}. */
  private branchOverPair(
    frontier: AztecEncoderState[],
    index: number,
    pairCode: number,
  ): AztecEncoderState[] {
    const candidates: AztecEncoderState[] = [];
    for (const state of frontier) {
      const withoutShift = state.closeShift(index);
      candidates.push(withoutShift.switchAndAppend(AztecMode.PUNCT, pairCode));
      if (state.mode() !== AztecMode.PUNCT) {
        candidates.push(withoutShift.appendWithShift(AztecMode.PUNCT, pairCode));
      }
      if (pairCode === 3 || pairCode === 4) {
        // ". " and ", " are also expressible as two DIGIT codes.
        candidates.push(
          withoutShift
            .switchAndAppend(AztecMode.DIGIT, 16 - pairCode)
            .switchAndAppend(AztecMode.DIGIT, 1),
        );
      }
      if (state.pendingShiftBytes() > 0) {
        candidates.push(state.addShiftedByte(index).addShiftedByte(index + 1));
      }
    }
    return AztecHighLevelEncoder.prune(candidates);
  }

  /** Keeps only the candidates that are not dominated by another one. */
  private static prune(candidates: AztecEncoderState[]): AztecEncoderState[] {
    const kept: AztecEncoderState[] = [];
    for (const candidate of candidates) {
      let survives = true;
      for (let k = 0; k < kept.length; k++) {
        const existing = kept[k];
        if (existing.dominates(candidate)) {
          survives = false;
          break;
        }
        if (candidate.dominates(existing)) {
          kept.splice(k, 1);
          k--;
        }
      }
      if (survives) {
        kept.unshift(candidate);
      }
    }
    return kept.slice();
  }
}
