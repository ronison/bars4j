import { test } from "node:test";
import assert from "node:assert/strict";

import {
  BarcodeGenerator,
  BarcodeRegistry,
  encodePng,
  linearDefaults,
  LinearBarcodeFormat,
  matrixDefaults,
} from "../src/index.ts";
import { sampleFor } from "../examples/sample-content.ts";

/**
 * Smoke test covering every registered format with the shared demo payloads: SVG and raster output
 * must be produced for all of them (the raster uses the dependency-free geometry path).
 */
for (const id of BarcodeRegistry.getInstance().ids()) {
  test(`${id} encodes the shared sample`, () => {
    const text = sampleFor(id);

    const svg = BarcodeGenerator.of(id).toSvg(text);
    assert.ok(svg.startsWith("<?xml"), `${id}: SVG must be a document`);
    assert.ok(svg.includes("</svg>"), `${id}: SVG must be closed`);

    const format = BarcodeRegistry.getInstance().get(id);
    const image = format instanceof LinearBarcodeFormat
      ? format.withOptions({ ...format.options(), showText: false }).toImage(text, linearDefaults())
      : format.toImage(text, matrixDefaults());

    assert.ok(image.width > 0 && image.height > 0, `${id}: empty raster`);
    const png = encodePng(image);
    assert.deepEqual(
      Array.from(png.subarray(0, 4)),
      [0x89, 0x50, 0x4e, 0x47],
      `${id}: invalid PNG signature`,
    );
  });
}
