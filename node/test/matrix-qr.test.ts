import { test } from "node:test";
import assert from "node:assert/strict";

import { QRCodeEncoder } from "../src/encode/matrix/qr-code-encoder.ts";
import { readMatrixFixture } from "./matrix-fixtures.ts";

/**
 * Conformance test: the Node port of {@code QRCodeEncoder} must reproduce, module-for-module, the
 * Java implementation's golden matrices under {@code src/test/resources/golden/matrix} (QRCODE-<i>).
 */
const INPUTS: string[] = [
  "https://github.com/ronison/bars4j",
  "Hello, world!",
  "Bars4J QRCode cross validation",
  "1234567890",
  "HELLO WORLD 1234",
  "https://github.com/ronison/bars4j/very/long/path?query=value&more=1",
];

for (let i = 0; i < INPUTS.length; i++) {
  test(`QRCODE-${i} matches golden`, () => {
    const symbol = new QRCodeEncoder().encode(INPUTS[i]);
    const fixture = readMatrixFixture("QRCODE", i);

    assert.equal(symbol.widthInModules(), fixture.width, `width mismatch for input ${i}`);
    assert.equal(symbol.heightInModules(), fixture.height, `height mismatch for input ${i}`);
    assert.equal(symbol.quietZone(), fixture.quietZone, `quiet zone mismatch for input ${i}`);

    for (let row = 0; row < fixture.height; row++) {
      for (let col = 0; col < fixture.width; col++) {
        assert.equal(
          symbol.module(row, col),
          fixture.rows[row][col],
          `module mismatch at row ${row}, col ${col} for input ${i}`,
        );
      }
    }
  });
}
