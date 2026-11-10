import { Matrix2of5Encoder } from "./matrix2of5-encoder.ts";

/**
 * Native Datalogic 2 of 5 encoder. Ported from the legacy {@code Datalogic2of5Encoder}, which only
 * changes the start/stop patterns of the matrix 2 of 5 encoder.
 */
export class Datalogic2of5Encoder extends Matrix2of5Encoder {
  constructor() {
    super();
    this.startChar = "0000";
    this.stopChar = "1000";
  }
}
