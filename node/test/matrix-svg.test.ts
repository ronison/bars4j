import { test } from "node:test";
import assert from "node:assert/strict";

import { BarcodeGenerator } from "../src/index.ts";
import { readMatrixSvg } from "./matrix-fixtures.ts";

/**
 * End-to-end conformance for the 2D formats: the whole SVG document produced by the unified
 * {@code BarcodeGenerator} must match the Java reference byte-for-byte (fixtures generated from
 * the Java implementation).
 */
const SAMPLE = "https://github.com/ronison/bars4j";
const IDS = ["QRCODE", "DATAMATRIX", "PDF417", "AZTEC"];

for (const id of IDS) {
  test(`${id} end-to-end SVG matches golden`, () => {
    const actual = BarcodeGenerator.of(id).toSvg(SAMPLE);
    assert.equal(actual, readMatrixSvg(id), `SVG mismatch for ${id}`);
  });
}
