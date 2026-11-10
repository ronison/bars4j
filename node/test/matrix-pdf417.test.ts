import { test } from "node:test";
import assert from "node:assert/strict";

import { PDF417Encoder } from "../src/encode/matrix/pdf417-encoder.ts";
import { readMatrixFixture } from "./matrix-fixtures.ts";

/**
 * Conformance test for the Node PDF417 port: the full module matrix must match the fixtures
 * generated from the Java implementation (under {@code src/test/resources/golden/matrix}).
 */
const INPUTS = [
  "https://github.com/ronison/bars4j",
  "Hello, world!",
  "PDF417 cross validation",
  "1234567890",
  "PDF417 ECC ".repeat(30),
];

for (let i = 0; i < INPUTS.length; i++) {
  test(`PDF417-${i} module matrix matches fixture`, () => {
    const symbol = new PDF417Encoder().encode(INPUTS[i]);
    const fixture = readMatrixFixture("PDF417", i);

    assert.equal(symbol.widthInModules(), fixture.width, `width mismatch for PDF417-${i}`);
    assert.equal(symbol.heightInModules(), fixture.height, `height mismatch for PDF417-${i}`);
    assert.equal(symbol.quietZone(), fixture.quietZone, `quiet zone mismatch for PDF417-${i}`);

    for (let row = 0; row < fixture.height; row++) {
      for (let col = 0; col < fixture.width; col++) {
        assert.equal(
          symbol.module(row, col),
          fixture.rows[row][col],
          `module mismatch at row ${row}, col ${col} for PDF417-${i}`,
        );
      }
    }
  });
}
