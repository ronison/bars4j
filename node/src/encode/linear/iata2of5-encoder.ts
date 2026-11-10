import { Standard2of5Encoder } from "./standard2of5-encoder.ts";

/**
 * Native IATA 2 of 5 encoder. Ported from the legacy {@code IATA2of5Encoder}, which only changes the
 * start/stop patterns of the standard 2 of 5 encoder.
 */
export class Iata2of5Encoder extends Standard2of5Encoder {
  constructor() {
    super();
    this.startChar = "0000";
    this.stopChar = "1000";
  }
}
