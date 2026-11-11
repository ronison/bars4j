# Bars4J for Node.js / TypeScript (`@bars4j/core`)

A Node.js/TypeScript port of the [Bars4J](https://github.com/ronison/bars4j) barcode library,
keeping the same characteristics and functionality.

This package lives in the same repository as the Java implementation and shares its golden
fixtures, so the two stay in sync. The port is being delivered in phases:

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Core + SVG for all 20 linear (1D) formats | **done** |
| 2 | 2D formats (QRCode, Data Matrix, PDF417, Aztec) + SVG | **done** |
| 3 | Raster output (ARGB image + PNG) and optional canvas text | **done** |
| 4 | Demos, CLI, packaging and CI | **done** |

## Usage

```ts
import { BarcodeGenerator, encodePng } from "@bars4j/core";

const svg = BarcodeGenerator.of("EAN13").toSvg("789100031550");

// Raster: an opaque ARGB image (width, height, Int32Array pixels).
const image = BarcodeGenerator.of("EAN13").toImage("789100031550");
const png = encodePng(image); // Uint8Array, no native dependency required
```

The raster path is split in two:

- **Geometry (dependency-free).** Bars and modules are rasterised directly into an
  [`RasterImage`](src/render/raster-image.ts) — an opaque ARGB buffer matching the Java
  `TYPE_INT_RGB` layout — and PNG export uses Node's built-in `zlib`. This path is
  byte-for-byte identical to the Java `*.image.txt` goldens.
- **Human-readable text (optional).** Drawing glyphs needs a font rasteriser, so it uses the
  optional [`@napi-rs/canvas`](https://www.npmjs.com/package/@napi-rs/canvas) backend. Glyphs are
  not guaranteed to be pixel-identical to the Java AWT output (only the bar geometry is), and
  requesting text without the backend installed throws a descriptive error. Use
  `canvasAvailable()` to probe, or disable the label via `LinearBarcodeFormat.withOptions`.

`BarcodeGenerator.of("<format id>")` accepts any id registered in the `BarcodeRegistry`
(case-insensitive):

- 1D: `EAN13`, `EAN8`, `EAN5`, `UPCA`, `UPCE`, `CODABAR`, `CODE39`, `CODE39EXT`, `CODE93`,
  `CODE93EXT`, `CODE128`, `CODE11`, `MSIPLESSEY`, `STANDARD2OF5`, `IATA2OF5`,
  `INTERLEAVED2OF5`, `ITF14`, `MATRIX2OF5`, `DATALOGIC2OF5`, `POSTNET`.
- 2D: `QRCODE`, `DATAMATRIX`, `PDF417`, `AZTEC`.

## Running the tests

Node.js 22.18+ runs the TypeScript sources directly (type stripping), so no build step is needed:

```sh
npm test          # conformance tests against the shared Java goldens
npm run typecheck # type-check with tsc
npm run check     # both
```

## Demos and CLI

```sh
npm run demo          # SVG + PNG for every registered format, into node/images/
npm run demo:qrcode   # 2D showcase (bigger modules, all four 2D formats)

# Command line (installed as the `bars4j` binary):
bars4j EAN13 789100031550 > barcode.svg
bars4j QRCODE "https://github.com/ronison/bars4j" --png qr.png --module-size 8
bars4j --list
```

## Conformance

The SVG output is verified byte-for-byte against the Java goldens stored in
`../src/test/resources/golden`. For the 1D formats these are the shared `.svg` files produced by the
Java build; for the 2D formats the module matrices and the end-to-end SVG documents live in
`../src/test/resources/golden/matrix` and are generated from the (unmodified) Java implementation.
The 1D raster geometry is additionally verified through the `*.image.txt` pixel fingerprints
(SHA-256 of the ARGB pixels), which the Node port reproduces exactly. Every registered format is
also smoke-tested for SVG and PNG output with the shared demo payloads.

## Packaging

The package ships the TypeScript sources (`src/`, `bin/`, `examples/`) and runs directly on
Node.js 22.18+; there is no build step. Consumers therefore need Node.js 22.18+ (native type
stripping) or a bundler that understands TypeScript; TypeScript projects may need
`allowImportingTsExtensions` (or `moduleResolution: "bundler"`). The package contents are checked
with `npm pack --dry-run` in CI.

## License

LGPL-2.1-or-later, inherited from the Java project. See the repository root `LICENSE`.
