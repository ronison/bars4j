import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

/**
 * Helpers for the 2D (matrix) conformance fixtures.
 *
 * The fixtures under {@code src/test/resources/golden/matrix} are generated from the Java
 * implementation (see the repository root); the Java sources themselves are left untouched.
 */
const DIR = new URL("../../src/test/resources/golden/matrix/", import.meta.url);

export interface MatrixFixture {
  width: number;
  height: number;
  quietZone: number;
  rows: boolean[][];
}

/** Reads one module-matrix fixture: {@code <ID>-<index>.txt}. */
export function readMatrixFixture(id: string, index: number): MatrixFixture {
  const text = readFileSync(fileURLToPath(new URL(`${id}-${index}.txt`, DIR)), "utf8");
  const lines = text.split("\n").filter((line) => line.length > 0);
  const header = lines[0].split(" ");
  const width = Number(header[0]);
  const height = Number(header[1]);
  const quietZone = Number(header[2]);
  const rows = lines.slice(1).map((line) => [...line].map((c) => c === "1"));
  return { width, height, quietZone, rows };
}

/** Reads the end-to-end SVG fixture: {@code <ID>.svg}. */
export function readMatrixSvg(id: string): string {
  return readFileSync(fileURLToPath(new URL(`${id}.svg`, DIR)), "utf8");
}
