import { test } from "node:test";
import assert from "node:assert/strict";

import { BarcodeGenerator } from "../src/index.ts";
import { renderSpec } from "../src/render/render-spec.ts";

/**
 * {@code toHtml} returns an SVG snippet ready to be embedded inline in an HTML page: no XML
 * prologue, no generator comment, no fallback text, with a {@code viewBox}, {@code role="img"}
 * and an {@code aria-label}. The drawing markup inside the snippet is identical to the validated
 * {@code toSvg} output.
 *
 * Mirrors {@code org.bars4j.HtmlOutputTest}.
 */
interface Case {
  id: string;
  code: string;
}

const CASES: Case[] = [
  { id: "EAN13", code: "789100031550" },
  { id: "EAN8", code: "9788515" },
  { id: "EAN5", code: "54495" },
  { id: "UPCA", code: "07567816415" },
  { id: "UPCE", code: "0756785" },
  { id: "CODABAR", code: "97885150040-85" },
  { id: "CODE39", code: "BARS4J-39" },
  { id: "CODE39EXT", code: "Bars4J-39Ext" },
  { id: "CODE93", code: "BARS4J-93" },
  { id: "CODE93EXT", code: "Bars4J-93Ext" },
  { id: "CODE128", code: "Bars4J-128" },
  { id: "CODE11", code: "9788515004041" },
  { id: "MSIPLESSEY", code: "9788515004041" },
  { id: "STANDARD2OF5", code: "978851500404" },
  { id: "IATA2OF5", code: "978851500404" },
  { id: "MATRIX2OF5", code: "123456" },
  { id: "DATALOGIC2OF5", code: "123456" },
  { id: "INTERLEAVED2OF5", code: "9876543210921" },
  { id: "ITF14", code: "1540014128876" },
  { id: "POSTNET", code: "805365961" },
  { id: "QRCODE", code: "Bars4J" },
  { id: "DATAMATRIX", code: "Bars4J" },
  { id: "PDF417", code: "Bars4J" },
  { id: "AZTEC", code: "Bars4J" },
];

for (const c of CASES) {
  test(`${c.id} HTML snippet is embeddable`, () => {
    const html = BarcodeGenerator.of(c.id).toHtml(c.code);
    assert.ok(html.startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""), `starts with the svg element: ${html}`);
    assert.ok(!html.includes("<?xml"), "no XML prologue");
    assert.ok(!html.includes("Created with Bars4J"), "no generator comment");
    assert.ok(!html.includes("Sorry, your browser"), "no fallback text");
    assert.ok(html.includes("role=\"img\""), "img role");
    assert.ok(html.includes(`aria-label="${displayName(c.id)}`), "aria label");
    assert.ok(html.endsWith("</svg>"), "closed svg element");
  });

  test(`${c.id} HTML carries viewBox`, () => {
    const html = BarcodeGenerator.of(c.id).toHtml(c.code);
    assert.ok(html.includes("viewBox=\"0 0 "), "viewBox present");
    assert.ok(html.includes("preserveAspectRatio=\"xMidYMid meet\""), "standard preserveAspectRatio");
  });

  test(`${c.id} HTML drawing markup matches SVG document`, () => {
    const generator = BarcodeGenerator.of(c.id);
    const svg = generator.toSvg(c.code);
    const html = generator.toHtml(c.code);
    assert.equal(drawMarkup(svg), drawMarkup(html), `drawing markup for ${c.id}`);
  });
}

test("explicit display size overrides width and height only", () => {
  const generator = BarcodeGenerator.of("CODE128");
  const natural = generator.toHtml("B4J");
  const sized = generator.toHtml("B4J", 200, 60);
  assert.ok(sized.includes("width=\"200\" height=\"60\""), "display size applied");
  assert.equal(extractAttribute(natural, "viewBox"), extractAttribute(sized, "viewBox"), "viewBox unchanged");
});

test("rejects non-positive display size", () => {
  const generator = BarcodeGenerator.of("CODE128");
  assert.throws(() => generator.toHtml("B4J", 0, 60), /must be positive/);
  assert.throws(() => generator.toHtml("B4J", 200, -1), /must be positive/);
});

test("HTML honours spec colours", () => {
  const redBlue = renderSpec(1, 70, 2.0, 4, "#e53935", "#0d47a1");
  const html = BarcodeGenerator.of("CODE128").with(redBlue).toHtml("B4J");
  assert.ok(html.includes("fill=\"#e53935\""), "foreground hex");
  assert.ok(html.includes("fill=\"#0d47a1\""), "background hex");
});

test("HTML escapes markup characters", () => {
  const generator = BarcodeGenerator.of("CODE128");
  const html = generator.toHtml("A<B&C>D");
  assert.ok(html.includes("&lt;"), "escaped <");
  assert.ok(html.includes("&amp;"), "escaped &");
  assert.ok(html.includes("&gt;"), "escaped >");
  // and the standalone document gets the same treatment
  assert.ok(generator.toSvg("A<B&C>D").includes("&amp;"), "toSvg escapes too");
});

test("EAN-13 HTML has no leading chevron", () => {
  const html = BarcodeGenerator.of("EAN13").toHtml("789100031550");
  assert.ok(html.includes(">315507</text>"), "check digit in the third text group");
  assert.equal(html.indexOf("textLength=\"8\">>"), -1, "no chevron placeholder");
});

/** Extracts everything between the end of the opening {@code <svg ...>} tag and {@code </svg>}. */
function drawMarkup(document: string): string {
  const svgOpen = document.indexOf("<svg");
  const start = document.indexOf(">", svgOpen);
  const end = document.lastIndexOf("</svg>");
  let markup = document.substring(start + 1, end);
  // The standalone document appends a legacy fallback text before the closing tag and (for
  // the matrix formats) carries no <style> element.
  markup = markup.replace("Sorry, your browser does not support inline SVG.", "");
  markup = markup.replace("<style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>", "");
  return markup;
}

function displayName(id: string): string {
  return BarcodeGenerator.of(id).format().displayName;
}

function extractAttribute(document: string, attribute: string): string {
  const from = document.indexOf(attribute + "=\"") + attribute.length + 2;
  const to = document.indexOf("\"", from);
  return document.substring(from, to);
}
