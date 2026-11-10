import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { BarcodeRegistry, fingerprint, linearDefaults, LinearBarcodeFormat } from "../src/index.ts";

/**
 * Raster conformance test: the bar geometry produced by the Node port must reproduce, byte-for-byte
 * (SHA-256 of the ARGB pixels), the {@code *.image.txt} fingerprints of the Java implementation.
 *
 * <p>The Java goldens fingerprint only the bar geometry (human-readable text disabled), which is
 * exactly the dependency-free raster path exercised here.
 *
 * Mirrors {@code org.bars4j.FormatGoldenTest#imageGolden}.
 */
interface Case {
  id: string;
  goldenName: string;
  code: string;
}

const CASES: Case[] = [
  { id: "EAN13", goldenName: "EAN13", code: "789100031550" },
  { id: "EAN8", goldenName: "EAN8", code: "9788515" },
  { id: "EAN5", goldenName: "EAN5", code: "54495" },
  { id: "UPCA", goldenName: "UPCA", code: "07567816415" },
  { id: "UPCE", goldenName: "UPCE", code: "0756785" },
  { id: "CODABAR", goldenName: "Codabar", code: "97885150040-85" },
  { id: "CODE39", goldenName: "Code39", code: "BARS4J-39" },
  { id: "CODE39EXT", goldenName: "Code39Ext", code: "Bars4J-39Ext" },
  { id: "CODE93", goldenName: "Code93", code: "BARS4J-93" },
  { id: "CODE93EXT", goldenName: "Code93Ext", code: "Bars4J-93Ext" },
  { id: "CODE128", goldenName: "Code128", code: "Bars4J-128" },
  { id: "CODE11", goldenName: "Code11", code: "9788515004041" },
  { id: "MSIPLESSEY", goldenName: "MSIPlessey", code: "9788515004041" },
  { id: "STANDARD2OF5", goldenName: "Standard2of5", code: "978851500404" },
  { id: "IATA2OF5", goldenName: "IATA2of5", code: "978851500404" },
  { id: "MATRIX2OF5", goldenName: "Matrix2of5", code: "123456" },
  { id: "DATALOGIC2OF5", goldenName: "Datalogic2of5", code: "123456" },
  { id: "INTERLEAVED2OF5", goldenName: "Interleaved2of5", code: "9876543210921" },
  { id: "POSTNET", goldenName: "PostNet", code: "805365961" },
];

const GOLDEN_DIR = new URL("../../src/test/resources/golden/", import.meta.url);

for (const c of CASES) {
  test(`${c.id} image fingerprint matches golden`, () => {
    const format = BarcodeRegistry.getInstance().get(c.id) as LinearBarcodeFormat;
    const noText = format.withOptions({ ...format.options(), showText: false });
    const image = noText.toImage(c.code, linearDefaults());

    const expected = readFileSync(fileURLToPath(new URL(c.goldenName + ".image.txt", GOLDEN_DIR)), "utf8");
    assert.equal(fingerprint(image), expected, `image mismatch for ${c.id}`);
  });
}
