import { mkdirSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

import { BarcodeGenerator, BarcodeRegistry, encodePng } from "../src/index.ts";
import { rasterImage } from "./render.ts";
import { sampleFor } from "./sample-content.ts";

/**
 * Generates an SVG and a PNG for every registered format, mirroring
 * {@code org.bars4j.demo.SimpleDemo} in the Java project.
 *
 * <p>Run with {@code npm run demo} (optionally passing an output directory).
 */
const outDir = process.argv[2] ?? fileURLToPath(new URL("../images/", import.meta.url));
mkdirSync(outDir, { recursive: true });

for (const id of BarcodeRegistry.getInstance().ids()) {
  const text = sampleFor(id);
  writeFileSync(`${outDir}/${id}.svg`, BarcodeGenerator.of(id).toSvg(text));
  writeFileSync(`${outDir}/${id}.png`, encodePng(rasterImage(id, text)));
  console.log(`${id.padEnd(14)} "${text}" -> ${id}.svg / ${id}.png`);
}

console.log(`\nGenerated ${BarcodeRegistry.getInstance().ids().length} formats in ${outDir}`);
