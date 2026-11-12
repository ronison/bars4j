import { completeLength, type LinearEncoder } from "../encode/api/barcode-encoder.ts";
import { InvalidSymbolException } from "../encode/api/invalid-symbol-exception.ts";
import type { LinearRenderer } from "../render/linear/linear-renderer.ts";
import type { LinearTextRenderer } from "../render/linear/linear-text-renderer.ts";
import {
  createRasterImage,
  drawScaled,
  parseArgb,
  type RasterImage,
} from "../render/raster-image.ts";
import { renderSpec, type RenderSpec } from "../render/render-spec.ts";
import type { LinearSymbol } from "../symbol/linear-symbol.ts";
import type { BarcodeFormat } from "./barcode-format.ts";
import type { FormatSpec } from "./format-spec.ts";

/** Smallest meaningful x-dimension (mirrors the legacy {@code Barcode.MIN_XDIMENSION}). */
export const MIN_XDIMENSION = 0.264583333;

/**
 * Default upper bound for the user-supplied text length. Variable-length symbologies (Code 128,
 * Code 39, MSI, 2-of-5, ...) accept arbitrary input, so without a cap an untrusted request can
 * drive a multi-megabyte string into the encoder and exhaust memory/CPU (DoS).
 */
export const DEFAULT_MAX_TEXT_LENGTH = 4_000;

let maxTextLengthValue = DEFAULT_MAX_TEXT_LENGTH;

/** The effective text-length cap. */
export function maxTextLength(): number {
  return maxTextLengthValue;
}

/**
 * Overrides the global text-length cap (e.g. lowered for an internet-facing service, or raised
 * for a trusted batch job). Values below 1 are ignored.
 */
export function setMaxTextLength(value: number): void {
  if (Number.isInteger(value) && value >= 1) {
    maxTextLengthValue = value;
  }
}

/** Restores the default text-length cap. */
export function resetMaxTextLength(): void {
  maxTextLengthValue = DEFAULT_MAX_TEXT_LENGTH;
}

const FONT_SIZE = 14;

/**
 * User-configurable options of a linear format. All values map to the legacy facade attributes.
 */
export interface LinearOptions {
  /** Narrow bar width (x-dimension) in millimetres. */
  readonly xDimensionMm: number;
  /** Bar height in millimetres. */
  readonly barHeightMm: number;
  /** Wide-to-narrow ratio (only used by wide-ratio coded formats). */
  readonly wideRatio: number;
  /** Whether to append the computed check digit before encoding. */
  readonly checkDigit: boolean;
  /** Whether to include the check digit in the human readable label. */
  readonly showCheckDigit: boolean;
  /** Whether to draw the human readable label. */
  readonly showText: boolean;
}

/**
 * A linear (1D) format backed by the native encoder/renderer pipeline.
 *
 * <p>Reproduces the legacy {@code org.bars4j.Barcode} assembly (check-digit handling, quiet zone,
 * x-dimension scaling, margins and human-readable text) for SVG output.
 *
 * Port of {@code org.bars4j.format.LinearBarcodeFormat}.
 */
export class LinearBarcodeFormat implements BarcodeFormat {
  private readonly encoder: LinearEncoder;
  private readonly renderer: LinearRenderer;
  private readonly textRenderer: LinearTextRenderer;
  private readonly specValue: FormatSpec;

  private readonly module: number;
  private readonly barHeightMmValue: number;
  private readonly wideToNarrow: number;
  private readonly barHeightValue: number;
  private readonly xdimension: number;
  private readonly checkDigitFlag: boolean;
  private readonly showCheckDigitFlag: boolean;
  private readonly showTextFlag: boolean;

  /** Extra vertical space (px) reserved below the bars so the human-readable text does not overlap. */
  private readonly textGap: number;

  constructor(
    encoder: LinearEncoder,
    renderer: LinearRenderer,
    textRenderer: LinearTextRenderer,
    spec: FormatSpec,
    xDimension: number,
    barHeightMm: number,
    wideRatio: number,
    checkDigit: boolean,
    showCheckDigit: boolean,
    showText: boolean,
    textGap = 0,
  ) {
    this.encoder = encoder;
    this.renderer = renderer;
    this.textRenderer = textRenderer;
    this.specValue = spec;
    this.module = xDimension;
    this.xdimension = Math.max(Math.trunc(xDimension / MIN_XDIMENSION), 1);
    this.barHeightMmValue = barHeightMm;
    this.barHeightValue = Math.round(barHeightMm / MIN_XDIMENSION);
    this.wideToNarrow = wideRatio;
    this.checkDigitFlag = checkDigit;
    this.showCheckDigitFlag = showCheckDigit;
    this.showTextFlag = showText;
    this.textGap = textGap;
  }

  /** Returns the current configuration of this format. */
  options(): LinearOptions {
    return {
      xDimensionMm: this.module,
      barHeightMm: this.barHeightMmValue,
      wideRatio: this.wideToNarrow,
      checkDigit: this.checkDigitFlag,
      showCheckDigit: this.showCheckDigitFlag,
      showText: this.showTextFlag,
    };
  }

  /** Returns a copy of this format with the given options applied. */
  withOptions(options: LinearOptions): LinearBarcodeFormat {
    return new LinearBarcodeFormat(
      this.encoder,
      this.renderer,
      this.textRenderer,
      this.specValue,
      options.xDimensionMm,
      options.barHeightMm,
      options.wideRatio,
      options.checkDigit,
      options.showCheckDigit,
      options.showText,
      this.textGap,
    );
  }

  id(): string {
    return this.specValue.id;
  }

  spec(): FormatSpec {
    return this.specValue;
  }

  /** Effective code (with check digit when applicable) and the text label to draw. */
  private resolve(text: string): [string, string] {
    if (text == null || text.length === 0) {
      throw new InvalidSymbolException(`[${this.specValue.id}] Invalid text length (0).`);
    }
    if (text.length > maxTextLengthValue) {
      throw new InvalidSymbolException(
        `[${this.specValue.id}] Text length (${text.length}) exceeds the maximum of ${maxTextLengthValue}.`,
      );
    }
    let code = text;
    let label = text;
    if (this.checkDigitFlag) {
      if (text.length === completeLength(this.encoder)) {
        // The caller already supplied the complete code (payload + check digit): keep it verbatim.
        code = text;
      } else {
        code = text + this.encoder.computeCheckSum(text);
      }
      if (this.showCheckDigitFlag) {
        label = code;
      }
    }
    return [code, label];
  }

  /**
   * Builds the internal render spec from this format's geometry, taking the colours from the
   * user-provided spec when given (port of the Java {@code internalSpec(RenderSpec)}).
   */
  private internalSpec(userSpec?: RenderSpec): RenderSpec {
    const foreground = userSpec?.foreground ?? "#000000";
    const background = userSpec?.background ?? "#ffffff";
    return renderSpec(1, this.barHeightValue, this.wideToNarrow, 1, foreground, background);
  }

  toImage(text: string, userSpec?: RenderSpec): RasterImage {
    const [code, label] = this.resolve(text);
    const symbol: LinearSymbol = this.encoder.encode(code);
    const internal = this.internalSpec(userSpec);
    const scale = this.module / MIN_XDIMENSION;

    const unscaledWidth = this.renderer.calcTotalWidth(
      symbol,
      internal.barWidth,
      internal.barHeight,
      internal.wideRatio,
    );
    const width = Math.trunc(unscaledWidth * scale) + 1;
    const height = this.barHeightValue + 8 + (this.showTextFlag ? this.textGap : 0);
    const image = createRasterImage(width, height, parseArgb(internal.background));

    if (Math.abs(scale - 1.0) < 1e-9) {
      // No horizontal scaling required: draw straight into the destination.
      this.renderer.renderInto(image, 0, 4, symbol, internal);
    } else {
      const unscaled = this.renderer.toImage(symbol, internal);
      drawScaled(image, unscaled, scale, this.barHeightValue / unscaled.height, 0, 4);
    }

    if (this.showTextFlag) {
      // Prefer the colour-aware variant when the renderer provides it.
      const colourAware = this.textRenderer as LinearTextRenderer & {
        paintTextWithSpec?: (img: RasterImage, t: string, w: number, s: RenderSpec) => void;
      };
      if (typeof colourAware.paintTextWithSpec === "function") {
        colourAware.paintTextWithSpec(image, label, 1, internal);
      } else {
        this.textRenderer.paintText(image, label, 1);
      }
    }
    return image;
  }

  toSvg(text: string, userSpec?: RenderSpec): string {
    const [code, label] = this.resolve(text);
    const symbol: LinearSymbol = this.encoder.encode(code);

    // The SVG paths are drawn in barWidth=1 units; compute the canvas width with the same
    // unit so the document is not over-sized for x-dimensions above the minimum.
    const totalWidth = this.renderer.calcTotalWidth(symbol, 1, this.barHeightValue, this.wideToNarrow);
    let totalHeight = this.barHeightValue + 4;

    let pcode = this.renderer.toSvg(symbol, this.internalSpec(userSpec));
    if (this.showTextFlag) {
      const textTop = this.barHeightValue + this.textGap;
      // Prefer the colour-aware variant when the renderer provides it.
      const colourAware = this.textRenderer as LinearTextRenderer & {
        paintTextSvgWithSpec?: (t: string, h: number, w: number, s: RenderSpec) => string;
      };
      if (typeof colourAware.paintTextSvgWithSpec === "function") {
        pcode += colourAware.paintTextSvgWithSpec(label, textTop, totalWidth, this.internalSpec(userSpec));
      } else {
        pcode += this.textRenderer.paintTextSvg(label, textTop, totalWidth);
      }
      totalHeight = this.textRenderer.calcTotalHeight(textTop, FONT_SIZE);
    }

    let res = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
      + "<!-- Created with Bars4J -->\n"
      + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"";
    res += String(totalWidth);
    res += "\" height=\"";
    res += String(totalHeight);
    // A previous version emitted the canvas size as the preserveAspectRatio alignment
    // ("x226Y78 meet"), which is not a valid SVG value; xMidYMid is the spec-correct
    // centred alignment.
    res += "\" preserveAspectRatio=\"xMidYMid meet\"><style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>";
    res += pcode;
    res += "Sorry, your browser does not support inline SVG.</svg>";
    return res;
  }

  checkSum(text: string): string {
    return this.encoder.computeCheckSum(text);
  }
}
