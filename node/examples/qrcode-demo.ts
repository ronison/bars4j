import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { BarcodeGenerator, BarcodeRegistry, encodePng, renderSpec } from "../src/index.ts";
import { rasterImage } from "./render.ts";

/**
 * Demonstrates the unified API, including the 2D formats, mirroring
 * {@code org.bars4j.demo.QRCodeDemo} in the Java project.
 *
 * <p>Run with {@code npm run demo:qrcode} (optionally passing an output directory).
 */
const outDir = process.argv[2] ?? fileURLToPath(new URL("../images/", import.meta.url));
mkdirSync(outDir, { recursive: true });

console.log(`Registered formats: ${BarcodeRegistry.getInstance().ids().join(", ")}`);

const text = "https://github.com/ronison/bars4j";

// A QRCode rendered with the default settings (error correction level M, module size 4).
writeFileSync(`${outDir}/QRCode.svg`, BarcodeGenerator.of("QRCODE").toSvg(text));
writeFileSync(`${outDir}/QRCode.png`, encodePng(rasterImage("QRCODE", text)));

// A larger QRCode: bigger modules.
const big = renderSpec(1, 70, 2.0, 8, "#000000", "#ffffff");
const bigImage = BarcodeGenerator.of("QRCODE").with(big).toImage(text);
writeFileSync(`${outDir}/QRCode-big.png`, encodePng(bigImage));
console.log(`QRCODE: ${bigImage.width}x${bigImage.height}px -> QRCode-big.png`);

// The same unified entry point works for the 1D formats too.
writeFileSync(
  `${outDir}/EAN13-from-new-api.svg`,
  BarcodeGenerator.of("EAN13").toSvg("789100031550"),
);

// The remaining 2D formats, rendered through the same API.
for (const id of ["DATAMATRIX", "PDF417", "AZTEC"]) {
  writeFileSync(`${outDir}/${id}.svg`, BarcodeGenerator.of(id).toSvg(text));
  const image = rasterImage(id, text);
  writeFileSync(`${outDir}/${id}.png`, encodePng(image));
  console.log(`${id}: ${image.width}x${image.height}px -> ${id}.svg / ${id}.png`);
}

console.log(`\nWritten to ${outDir}`);
