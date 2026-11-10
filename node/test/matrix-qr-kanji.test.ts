import { test } from "node:test";
import assert from "node:assert/strict";

import { QRCodeEncoder } from "../src/index.ts";
import { readMatrixFixture } from "./matrix-fixtures.ts";

/**
 * Kanji-mode conformance: exercises the Shift-JIS based densest-mode selection (the only QRCode
 * data mode not already covered by the ASCII fixtures).
 */
test("QRCODE kanji mode matches golden", () => {
  const symbol = new QRCodeEncoder().encode("\u3053\u3093\u306B\u3061\u306F\u4E16\u754C");
  const fixture = readMatrixFixture("QRCODE", 6);

  assert.equal(symbol.widthInModules(), fixture.width);
  assert.equal(symbol.heightInModules(), fixture.height);
  assert.equal(symbol.quietZone(), fixture.quietZone);
  for (let row = 0; row < fixture.height; row++) {
    for (let col = 0; col < fixture.width; col++) {
      assert.equal(symbol.module(row, col), fixture.rows[row][col], `module mismatch at ${row},${col}`);
    }
  }
});
