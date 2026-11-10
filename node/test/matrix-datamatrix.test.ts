import { test } from "node:test";
import assert from "node:assert/strict";

import { DataMatrixEncoder } from "../src/encode/matrix/data-matrix-encoder.ts";
import { readMatrixFixture } from "./matrix-fixtures.ts";

/**
 * Conformance test: the Node port of the Data Matrix (ECC200) encoder must reproduce, module for
 * module, the golden matrices generated from the Java implementation.
 */
const INPUTS = [
  "https://github.com/ronison/bars4j",
  "Hello, world!",
  "Bars4J DataMatrix cross validation",
  "1234567890",
  "A".repeat(200),
  "caf\u00E9 \u00FF",
];

for (let i = 0; i < INPUTS.length; i++) {
  test(`DATAMATRIX-${i} matches fixture`, () => {
    const expected = readMatrixFixture("DATAMATRIX", i);
    const symbol = new DataMatrixEncoder().encode(INPUTS[i]);

    assert.equal(symbol.widthInModules(), expected.width, "width");
    assert.equal(symbol.heightInModules(), expected.height, "height");
    assert.equal(symbol.quietZone(), expected.quietZone, "quiet zone");

    for (let row = 0; row < expected.height; row++) {
      for (let column = 0; column < expected.width; column++) {
        assert.equal(
          symbol.module(row, column),
          expected.rows[row][column],
          `module mismatch at row ${row}, column ${column}`,
        );
      }
    }
  });
}
