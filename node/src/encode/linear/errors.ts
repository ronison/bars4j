import { InvalidSymbolException } from "../api/invalid-symbol-exception.ts";
import { NumberFormatError } from "./linear-bits.ts";

/**
 * Converts a {@link NumberFormatError} (the JS stand-in for Java's {@code NumberFormatException})
 * into an {@link InvalidSymbolException}, re-throwing anything else unchanged.
 */
export function numericOrInvalid(e: unknown, message: string): never {
  if (e instanceof NumberFormatError) {
    throw new InvalidSymbolException(message);
  }
  throw e;
}
