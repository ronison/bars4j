import { test } from "node:test";
import assert from "node:assert/strict";

import {
  BarcodeGenerator,
  BarcodeRegistry,
  canvasAvailable,
  encodePng,
  getArgb,
  linearDefaults,
  LinearBarcodeFormat,
  ARGB_BLACK,
  ARGB_WHITE,
} from "../src/index.ts";

/** QRCode symbol used across the raster smoke tests. */
const SAMPLE = "https://github.com/ronison/bars4j";

/** Renders a linear format with the human-readable text disabled (the dependency-free path). */
function renderNoText(id: string, code: string) {
  const format = BarcodeRegistry.getInstance().get(id) as LinearBarcodeFormat;
  return format.withOptions({ ...format.options(), showText: false }).toImage(code, linearDefaults());
}

test("1D raster dimensions match the Java layout", () => {
  const image = renderNoText("EAN13", "789100031550");
  // X-dimension module == MIN_XDIMENSION -> no scaling; 95 element modules + 20 quiet-zone modules.
  assert.equal(image.width, 115 + 1);
  // barHeight(72) + 8 px vertical padding.
  assert.equal(image.height, 80);
});

test("1D raster honours the quiet zone and the dark bars", () => {
  const image = renderNoText("EAN13", "789100031550");
  // The 10-module leading quiet zone must stay white.
  for (let x = 0; x < 10; x++) {
    assert.equal(getArgb(image, x, 40), ARGB_WHITE, `quiet zone not white at x=${x}`);
  }
  // The first module of the EAN-13 symbol is a guard bar (dark).
  assert.equal(getArgb(image, 10, 40), ARGB_BLACK);
});

test("2D raster is square, draws the quiet zone and dark modules", () => {
  const image = BarcodeGenerator.of("QRCODE").toImage(SAMPLE);
  assert.equal(image.width, image.height);
  assert.ok(image.width > 100, "unexpectedly small QRCode image");
  // Top-left corner is the quiet zone.
  assert.equal(getArgb(image, 0, 0), ARGB_WHITE);
  // There is at least one dark module somewhere.
  let dark = 0;
  for (let i = 0; i < image.pixels.length; i++) {
    if ((image.pixels[i] >>> 0) === ARGB_BLACK) {
      dark++;
    }
  }
  assert.ok(dark > 0, "expected dark modules");
});

test("encodePng produces a valid PNG header", () => {
  const image = renderNoText("EAN13", "789100031550");
  const png = encodePng(image);
  assert.deepEqual(Array.from(png.subarray(0, 8)), [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const view = Buffer.from(png);
  assert.equal(view.toString("ascii", 12, 16), "IHDR");
  assert.equal(view.readUInt32BE(16), image.width);
  assert.equal(view.readUInt32BE(20), image.height);
});

test("human-readable text is drawn with the optional canvas backend", { skip: !canvasAvailable() }, () => {
  const format = BarcodeRegistry.getInstance().get("EAN13") as LinearBarcodeFormat;
  const noText = format.withOptions({ ...format.options(), showText: false }).toImage("789100031550", linearDefaults());
  const withText = format.withOptions({ ...format.options(), showText: true }).toImage("789100031550", linearDefaults());

  assert.equal(withText.width, noText.width);
  assert.equal(withText.height, noText.height);

  let differences = 0;
  for (let i = 0; i < noText.pixels.length; i++) {
    if (noText.pixels[i] !== withText.pixels[i]) {
      differences++;
    }
  }
  assert.ok(differences > 0, "expected the human-readable text to change some pixels");
});
