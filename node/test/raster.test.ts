import { test } from "node:test";
import assert from "node:assert/strict";

import {
  BarcodeGenerator,
  BarcodeRegistry,
  canvasAvailable,
  encodePng,
  getArgb,
  linearDefaults,
  renderSpec,
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

/** Formats whose raster output shortens the data bars to leave room for the human-readable text. */
const GAP_CASES: Array<[string, string]> = [
  ["EAN13", "789100031550"],
  ["EAN8", "96385074"],
  ["UPCA", "03600029145"],
  ["UPCE", "01234565"],
  ["EAN5", "12345"],
];

test("human-readable text keeps a 2px gap above the data bars", { skip: !canvasAvailable() }, () => {
  for (const [id, code] of GAP_CASES) {
    const format = BarcodeRegistry.getInstance().get(id) as LinearBarcodeFormat;
    const image = format.withOptions({ ...format.options(), showText: true }).toImage(code, linearDefaults());
    const isInk = (x: number, y: number) => (((getArgb(image, x, y) >>> 16) & 0xff) < 128);

    // For each column, find the bar run that spans the bar area (contains row 10), then the first
    // inked row below it (the top of the glyph). Guards extend lower than data bars, so only the
    // dominant (modal) bar end — the data bars — is asserted.
    const barEnds: number[] = [];
    const gaps: number[] = [];
    for (let x = 0; x < image.width; x++) {
      let inRun = false;
      let runStart = -1;
      for (let y = 0; y < image.height; y++) {
        const ink = isInk(x, y);
        if (ink && !inRun) {
          inRun = true;
          runStart = y;
        } else if (!ink && inRun) {
          inRun = false;
          const runEnd = y - 1;
          if (runStart <= 10 && 10 <= runEnd) {
            let glyphTop = -1;
            for (let z = runEnd + 1; z < image.height; z++) {
              if (isInk(x, z)) {
                glyphTop = z;
                break;
              }
            }
            if (glyphTop >= 0) {
              barEnds.push(runEnd);
              gaps.push(glyphTop - runEnd - 1);
            }
            break;
          }
        }
      }
    }

    const counts = new Map<number, number>();
    for (const end of barEnds) {
      counts.set(end, (counts.get(end) ?? 0) + 1);
    }
    let modalEnd = -1;
    let best = -1;
    for (const [end, count] of counts) {
      if (count > best) {
        best = count;
        modalEnd = end;
      }
    }

    let checked = 0;
    for (let i = 0; i < barEnds.length; i++) {
      if (barEnds[i] <= modalEnd) {
        assert.ok(gaps[i] >= 2, `${id}: gap of ${gaps[i]}px between bars and text (expected >= 2)`);
        checked++;
      }
    }
    assert.ok(checked > 0, `${id}: expected at least one data bar with text below it`);
  }
});

test("1D raster and SVG honour user-provided spec colours", () => {
  // Red bars on a blue background (mirrors the Java RenderSpecColorsTest).
  const redBlue = renderSpec(1, 70, 2.0, 4, "#e53935", "#0d47a1");
  const image = BarcodeGenerator.of("CODE128").with(redBlue).toImage("BARS4J");

  const hasColour = (argb: number) => image.pixels.some((p) => (p >>> 0) === argb);
  assert.ok(hasColour(0xffe53935), "red foreground not found in raster");
  assert.ok(hasColour(0xff0d47a1), "blue background not found in raster");
  assert.ok(!hasColour(0xff000000), "default black unexpectedly found");
  assert.ok(!hasColour(0xffffffff), "default white unexpectedly found");

  const svg = BarcodeGenerator.of("CODE128").with(redBlue).toSvg("BARS4J");
  assert.ok(svg.includes('fill="#e53935"'), "foreground hex in SVG path");
  assert.ok(svg.includes('fill="#0d47a1"'), "background hex in SVG path");
  assert.ok(!svg.includes('fill="#fff"'), "no hardcoded white fill");
});
