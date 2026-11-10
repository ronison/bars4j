import { BitSemantics } from "../../format/bit-semantics.ts";
import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import type { LinearEncoder } from "../api/barcode-encoder.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { isDigit, symbol } from "./linear-bits.ts";

const CODES_WIDTH = [
  "100100100100", "100100100110", "100100110100", "100100110110", "100110100100",
  "100110100110", "100110110100", "100110110110", "110100100100", "110100100110",
];

const START_CHAR = "110";
const STOP_CHAR = "1001";

/**
 * Native MSI/Plessey encoder. Ported from the legacy {@code MSIPlesseyEncoder}.
 */
export class MsiPlesseyEncoder implements LinearEncoder {
  encode(text: string): LinearSymbol {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[MSIPlessey] Invalid text length (${text.length}).`);
    }
    let bits = "";
    try {
      bits += START_CHAR;
      for (let i = 0; i < text.length; i++) {
        bits += CODES_WIDTH[charToInt(text.charAt(i))];
      }
      bits += STOP_CHAR;
    } catch {
      throw new InvalidSymbolException("[MSIPlessey] Only numbers suported.");
    }
    return symbol(bits, BitSemantics.MODULE_DARK);
  }

  computeCheckSum(text: string): string {
    if (text.length < 1) {
      throw new InvalidSymbolException(`[MSIPlessey] Invalid text length (${text.length}).`);
    }
    for (let i = 0; i < text.length; i++) {
      if (!isDigit(text.charAt(i))) {
        throw new InvalidSymbolException("[MSIPlessey] Only numbers suported.");
      }
    }
    let newNumber = "";
    let numTotal = "";
    for (let i = text.length - 1; i >= 0; i -= 2) {
      newNumber = text.charAt(i) + newNumber;
      if (i > 0) {
        numTotal += text.charAt(i - 1);
      }
    }
    numTotal += doubleDigits(newNumber);

    let sum = 0;
    for (let i = 0; i < numTotal.length; i++) {
      sum += charToInt(numTotal.charAt(i));
    }

    return String(10 - (sum % 10 === 0 ? 10 : sum % 10));
  }
}

/**
 * Doubles a decimal digit string, returning the result as a digit string. Implemented without
 * {@code parseInt} so that arbitrarily long inputs cannot overflow.
 */
function doubleDigits(digits: string): string {
  let doubled = "";
  let carry = 0;
  for (let i = digits.length - 1; i >= 0; i--) {
    const value = (digits.charCodeAt(i) - 48) * 2 + carry;
    doubled += String.fromCharCode(48 + (value % 10));
    carry = Math.trunc(value / 10);
  }
  if (carry > 0) {
    doubled += String.fromCharCode(48 + carry);
  }
  return [...doubled].reverse().join("");
}

function charToInt(val: string): number {
  const code = val.charCodeAt(0);
  if (code < 48 || code > 57) {
    throw new Error("Invalid number");
  }
  return code - 48;
}
