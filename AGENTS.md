# AGENTS.md — Bars4J integration guide for AI agents

This file gives coding agents everything needed to integrate and use **Bars4J** correctly in
Java systems. Treat it as the fast path; the javadoc and the tests are the source of truth.

## 1. What Bars4J is

- Pure-Java barcode library. Requires **JDK 17+**. The only runtime dependency is the JDK's
  `java.desktop` module (AWT rendering). No ZXing, no third-party jars.
- Encodes **20 linear (1D)** and **4 matrix (2D)** formats.
- Outputs: AWT `BufferedImage`, standalone SVG document, and an inline-embeddable HTML/SVG snippet.
- Also available for Node.js/TypeScript as `@bars4j/core` (same entry-point design).

**Install (Maven):**

```xml
<dependency>
  <groupId>org.bars4j</groupId>
  <artifactId>bars4j</artifactId>
  <version>1.0.1</version> <!-- check the latest release -->
</dependency>
```

Or download the jar from the [releases page](https://github.com/ronison/bars4j/releases).

## 2. 30-second integration

```java
import java.awt.image.BufferedImage;
import org.bars4j.api.BarcodeGenerator;

BufferedImage img  = BarcodeGenerator.of("QRCODE").toImage("https://example.com");
String svg         = BarcodeGenerator.of("EAN13").toSvg("789100031550");
String html        = BarcodeGenerator.of("CODE128").toHtml("Bars4J-128", 300, 100);
String check       = BarcodeGenerator.of("EAN13").checkSum("789100031550"); // "7"
```

There is a single entry point: `BarcodeGenerator.of(formatId)` returns an **immutable** generator.
Apply options with `.with(RenderSpec)`. Every output method throws the **checked**
`BarcodeException` — always handle it.

To write a PNG file use standard JDK APIs (the library's own `ImageUtil` is internal API):

```java
try (var out = new FileOutputStream("qr.png")) {
    javax.imageio.ImageIO.write(img, "png", out);
}
```

## 3. Format ids (the registry)

Lookups are **case-insensitive and trimmed**. List all ids with
`BarcodeRegistry.getInstance().ids()`; test membership with `contains(id)`.

| id | Name | Kind | Check digit | HRI text¹ | Notes |
|---|---|---|---|---|---|
| `EAN13` | EAN-13 | 1D | ✓ | ✓ | payload 12 / complete 13 digits |
| `EAN8` | EAN-8 | 1D | ✓ | ✓ | payload 7 / complete 8 digits |
| `EAN5` | EAN-5 | 1D | — | ✓ | 5-digit add-on, no check digit |
| `UPCA` | UPC-A | 1D | ✓ | ✓ | payload 11 / complete 12 digits |
| `UPCE` | UPC-E | 1D | ✓ | ✓ | payload 7 / complete 8 digits |
| `CODABAR` | Codabar | 1D | — | ✓ | |
| `CODE39` | Code 39 | 1D | — | ✓ | |
| `CODE39EXT` | Code 39 Extended | 1D | — | ✓ | full ASCII |
| `CODE93` | Code 93 | 1D | ✓ | ✓ | |
| `CODE93EXT` | Code 93 Extended | 1D | ✓ | ✓ | full ASCII |
| `CODE128` | Code 128 | 1D | —² | ✓ | check char computed and appended internally |
| `CODE11` | Code 11 | 1D | ✓ | ✓ | |
| `MSIPLESSEY` | MSI Plessey | 1D | ✓ | ✓ | |
| `STANDARD2OF5` | Standard 2 of 5 | 1D | ✓ | ✓ | |
| `IATA2OF5` | IATA 2 of 5 | 1D | ✓ | ✓ | |
| `INTERLEAVED2OF5` | Interleaved 2 of 5 | 1D | ✓ | ✓ | |
| `ITF14` | ITF-14 | 1D | ✓ | ✓ | GS1 bearer bars; HRI mask `0 00 00000 00000 0`; payload 13 / complete 14 |
| `MATRIX2OF5` | Matrix 2 of 5 | 1D | ✓ | ✓ | |
| `DATALOGIC2OF5` | Datalogic 2 of 5 | 1D | ✓ | ✓ | |
| `POSTNET` | PostNet | 1D | ✓ | — | height-coded bars, draws no label |
| `QRCODE` | QRCode | 2D | — | — | versions 1-40, ECC L/M/Q/H (default M), numeric/alphanumeric/byte/kanji modes |
| `DATAMATRIX` | Data Matrix | 2D | — | — | ECC200, square + rectangular symbols, ISO-8859-1 input |
| `PDF417` | PDF417 | 2D | — | — | text/byte/numeric compaction (auto), ECC levels 0-8 |
| `AZTEC` | Aztec | 2D | — | — | compact/full symbols, auto layer + ECC selection, ECI-aware |

¹ HRI = human-readable interaction interval: the label drawn under the symbol.
² `CODE128` has no *external* check digit: `checkSum()` returns an empty string (the check
character is computed and appended internally during encoding).

**Check-digit duality (important):** for formats with an external check digit the input may be
given as *payload only* (the digit is computed and appended) or as the *complete code* (the digit
is validated and kept verbatim; a wrong digit throws `InvalidSymbolException`).
UPC conversions: `UpcaEncoder.convertUPCAtoUPCE(...)` and `UpceEncoder.convertUPCEtoUPCA(...)`
(obtain encoders via `LinearBarcodeFormat` or register-level access; they live in the exported
`org.bars4j.encode.api`/`org.bars4j.encode.linear` packages — the latter only on the classpath,
see §9).

## 4. Output API (`BarcodeGenerator`)

| Method | Returns | Use for |
|---|---|---|
| `toImage(String)` | `BufferedImage` | AWT painting, printing, PNG/JPEG via `ImageIO` |
| `toSvg(String)` | `String` | Standalone SVG **document** (XML prologue, fixed canvas) |
| `toHtml(String)` | `String` | Inline SVG **snippet** for web pages: no prologue, `viewBox` (scales with container), `role="img"`, `aria-label` |
| `toHtml(String, int w, int h)` | `String` | Same snippet with explicit display size in px (geometry preserved via `viewBox`) |
| `checkSum(String)` | `String` | The external check value; empty string when the format has none |
| `with(RenderSpec)` | `BarcodeGenerator` | Options for the next render |
| `format()` | `FormatSpec` | Metadata (id, display name, capabilities) |

All render methods throw `BarcodeException` (checked).

## 5. RenderSpec (rendering options)

`org.bars4j.render.RenderSpec` is an immutable record:

```java
RenderSpec spec = new RenderSpec(
        1,                          // barWidth:    narrow-module width for LINEAR symbols (px)
        70,                         // barHeight:   bar height for LINEAR symbols (px)
        2.0,                        // wideRatio:   wide-to-narrow ratio (wide-ratio coded formats)
        4,                          // moduleSize:  module size for MATRIX symbols (px)
        Color.BLACK, Color.WHITE,   // foreground / background
        10,                         // quietZone:   quiet zone per side, in modules (0 = none)
        14);                        // fontSize:    HRI label font size (px)

BufferedImage img = BarcodeGenerator.of("EAN13").with(spec).toImage("789100031550");
```

**Validated bounds** (the canonical constructor throws `IllegalArgumentException` outside them —
this is intentional DoS hardening, do not bypass it for untrusted input):

| Field | Range |
|---|---|
| `barWidth` | 1 … `MAX_BAR_WIDTH` (100) |
| `barHeight` | 1 … `MAX_BAR_HEIGHT` (10 000) |
| `moduleSize` | 1 … `MAX_MODULE_SIZE` (100) |
| `wideRatio` | ≥ 1.0 |
| `quietZone` | 0 … `MAX_QUIET_ZONE` (100) |
| `fontSize` | 1 … `MAX_FONT_SIZE` (200) |

Presets: `RenderSpec.linearDefaults()` / `RenderSpec.matrixDefaults()` — both
`(1, 70, 2.0, 4, BLACK, WHITE, quiet 10, font 14)`. `null` colors fall back to black on white.

**Per-kind semantics (avoid wrong expectations):**

- **Linear formats** keep their registered geometry (x-dimension, bar height, wide ratio).
  From the spec only `foreground`, `background`, `quietZone` and `fontSize` are used;
  `barWidth`/`barHeight`/`moduleSize` are effectively ignored on this path.
- **Matrix formats** use `moduleSize` (px per module), `quietZone` (modules per side) and the colors.
- EAN/UPC labels are drawn digit-by-digit in fixed canonical slots, so label geometry is stable
  across platforms and font metrics.

## 6. Exceptions and error handling

```
BarcodeException (checked)
└── InvalidSymbolException        — input cannot be represented (length, characters, range)
    └── UnsupportedTextException  — valid alphabet but not for the chosen mode (e.g. letters on numeric-only)
```

- `null`/empty text → `InvalidSymbolException`.
- Unknown format id → **`IllegalArgumentException`** (runtime) from `BarcodeGenerator.of(id)` /
  `BarcodeRegistry.get(id)`. When the id comes from user input, check
  `BarcodeRegistry.getInstance().contains(id)` first.
- Recommended mapping for services: `BarcodeException` → user-facing 4xx-style error message;
  `IllegalArgumentException` → programming/configuration error (5xx / bug).

## 7. Input rules

- Text length is capped at `LinearBarcodeFormat.DEFAULT_MAX_TEXT_LENGTH` = **4000** chars
  (`InvalidSymbolException` above). Override globally with `LinearBarcodeFormat.setMaxTextLength(n)`
  / `resetMaxTextLength()` — it is **process-global**: set it once at startup, never per request
  in concurrent applications.
- Each symbology accepts only its own alphabet; violations throw `UnsupportedTextException`
  (e.g. digits-only formats, Codabar's `A-D/-.$/+` set, Data Matrix ISO-8859-1 bytes).
  Prefer `CODE39EXT`, `CODE128`, QRCode, Data Matrix (Latin-1) or Aztec (ECI) for arbitrary text.
- 2D formats auto-select modes/versions; do not pre-compute capacities yourself.

## 8. Custom formats

Register on the singleton registry — duplicate ids are rejected, built-ins **cannot be shadowed**:

```java
BarcodeRegistry.getInstance().register(new MatrixBarcodeFormat(
        myMatrixEncoder,            // implements org.bars4j.encode.api.MatrixEncoder
        new MatrixRenderer(),       // org.bars4j.render.matrix.MatrixRenderer
        new FormatSpec("MY2D", "My 2D", SymbolKind.MATRIX, false, false, false)));
```

SPI packages (exported): `org.bars4j.encode.api` (`BarcodeEncoder`, `LinearEncoder`,
`MatrixEncoder`), `org.bars4j.render.linear`, `org.bars4j.render.matrix`, and
`org.bars4j.format` (`BarcodeFormat`, `FormatSpec`, `LinearBarcodeFormat`, `MatrixBarcodeFormat`).

## 9. JPMS / module-system notes

Module name: **`org.bars4j`** (`requires java.desktop` — add `requires org.bars4j;` in your
`module-info.java`).

- **Exported:** `org.bars4j.api`, `org.bars4j.demo`, `org.bars4j.encode.api`, `org.bars4j.format`,
  `org.bars4j.render`, `org.bars4j.render.linear`, `org.bars4j.render.matrix`.
- **Internal — do not import** (not exported; reflection into them is not part of the contract):
  `org.bars4j.encode.linear`, `org.bars4j.encode.matrix.*`, `org.bars4j.symbol`, `org.bars4j.util`.
- In modular applications, `FormatSpec.kind()` returns a type from an internal package — prefer
  `id()`, `displayName()` and the boolean capability flags (`supportsCheckDigit`, `allowsText`,
  `allowsWideRatio`).

## 10. Thread safety

- `BarcodeGenerator`, `RenderSpec` and the format objects are immutable — share them freely.
- `BarcodeRegistry` methods are `synchronized`.
- `LinearBarcodeFormat.setMaxTextLength` is process-global (see §7).

## 11. Building and verifying (contributors)

```sh
mvn test     # unit tests
mvn verify   # full verification, incl. the PerformanceIT throughput/latency gate
mvn exec:java                                            # QRCodeDemo
mvn exec:java -Dexec.mainClass=org.bars4j.demo.SimpleDemo # all formats
mvn exec:java -Dexec.mainClass=org.bars4j.demo.SwingDemo  # Swing UI
```

The jar is a **library** (no `Main-Class`): `java -jar target/bars4j-*.jar` does not work.

## 12. Pitfall checklist (TL;DR)

1. Always catch `BarcodeException` around `toImage`/`toSvg`/`toHtml`/`checkSum`.
2. Validate user-supplied format ids with `registry.contains(id)` — unknown ids throw
   `IllegalArgumentException`.
3. EAN-13 wants 12 **or** 13 digits (not 11); ITF-14 wants 13 **or** 14.
4. Web page → `toHtml`; SVG file → `toSvg`; AWT/print pipeline → `toImage`.
5. PNG export: use `javax.imageio.ImageIO`, not the internal `ImageUtil`.
6. `POSTNET` draws no label; `EAN5` has no check digit.
7. Never relax the `RenderSpec` bounds or the text-length cap for untrusted input — they are
   deliberate DoS hardening.
8. Don't import `org.bars4j.encode.matrix.*`, `org.bars4j.encode.linear`, `org.bars4j.symbol`
   or `org.bars4j.util` — internal API, not covered by compatibility guarantees.
