import type { LinearSymbol } from "../../symbol/linear-symbol.ts";
import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { Code39Encoder } from "./code39-encoder.ts";

const CODES_TABLE = [
  "%U", "$A", "$B", "$C", "$D", "$E", "$F", "$G",
  "$H", "$I", "$J", "$K", "$L", "$M", "$N", "$O",
  "$P", "$Q", "$R", "$S", "$T", "$U", "$V", "$W",
  "$X", "$Y", "$Z", "%A", "%B", "%C", "%D", "%E",
  " ", "/A", "/B", "/C", "/D", "/E", "/F", "/G",
  "/H", "/I", "/J", "/K", "/L", "-", ".", "/O",
  "0", "1", "2", "3", "4", "5", "6", "7",
  "8", "9", "/Z", "%F", "%G", "%H", "%I", "%J",
  "%V", "A", "B", "C", "D", "E", "F", "G", "H", "I",
  "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S",
  "T", "U", "V", "W", "X", "Y", "Z", "%K", "%L", "%M",
  "%N", "%O", "%W", "+A", "+B", "+C", "+D", "+E",
  "+F", "+G", "+H", "+I", "+J", "+K", "+L", "+M",
  "+N", "+O", "+P", "+Q", "+R", "+S", "+T", "+U",
  "+V", "+W", "+X", "+Y", "+Z", "%P", "%Q", "%R",
  "%S", "%T",
];

/**
 * Native Code 39 Extended encoder. Ported from the legacy {@code Code39ExtEncoder}.
 */
export class Code39ExtEncoder extends Code39Encoder {
  encode(text: string): LinearSymbol {
    return super.encode(convertText(text));
  }

  computeCheckSum(text: string): string {
    return super.computeCheckSum(convertText(text));
  }
}

function convertText(str: string): string {
  let tmp = "";
  for (let i = 0; i < str.length; i++) {
    const c = str.charAt(i);
    const code = c.charCodeAt(0);
    if (code >= CODES_TABLE.length) {
      throw new InvalidSymbolException("[Code39] The text contains unsuported chars.");
    }
    tmp += CODES_TABLE[code];
  }
  return tmp;
}
