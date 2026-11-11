import { createRequire } from "node:module";

import type { RasterImage } from "./raster-image.ts";

const require = createRequire(import.meta.url);

interface CanvasLike {
  width: number;
  height: number;
  getContext(type: "2d"): Context2DLike;
}

interface Context2DLike {
  font: string;
  fillStyle: string;
  fillRect(x: number, y: number, width: number, height: number): void;
  fillText(text: string, x: number, y: number): void;
  measureText(text: string): TextMetricsLike;
  createImageData(width: number, height: number): { data: Uint8ClampedArray };
  putImageData(data: { data: Uint8ClampedArray }, x: number, y: number): void;
  getImageData(x: number, y: number, width: number, height: number): { data: Uint8ClampedArray };
}

interface TextMetricsLike {
  width: number;
  fontBoundingBoxAscent?: number;
  fontBoundingBoxDescent?: number;
  actualBoundingBoxAscent?: number;
  actualBoundingBoxDescent?: number;
}

interface CanvasModule {
  createCanvas(width: number, height: number): CanvasLike;
}

let cached: CanvasModule | null | undefined;

function loadCanvas(): CanvasModule {
  if (cached === undefined) {
    try {
      cached = require("@napi-rs/canvas") as CanvasModule;
    } catch {
      cached = null;
    }
  }
  if (cached === null) {
    throw new Error(
      "Raster text rendering requires the optional dependency '@napi-rs/canvas'. "
        + "Install it (npm install @napi-rs/canvas) or render with the human-readable text disabled.",
    );
  }
  return cached;
}

/** True when the optional canvas backend is installed and usable. */
export function canvasAvailable(): boolean {
  try {
    loadCanvas();
    return true;
  } catch {
    return false;
  }
}

/**
 * A minimal drawing pen over a {@link RasterImage}, backed by the optional canvas. Only the
 * operations used by the human-readable text renderers are exposed.
 */
export interface RasterPen {
  readonly width: number;
  readonly height: number;
  /** Selects a monospaced font of the given pixel size. */
  setFont(sizePx: number, family?: string): void;
  /** Font height in pixels (ascender + descender), approximating {@code FontMetrics.getHeight()}. */
  fontHeight(): number;
  /** Ascender height in pixels above the baseline, approximating {@code FontMetrics.getAscent()}. */
  fontAscent(): number;
  /** Advance width of {@code text} in pixels (approximating {@code FontMetrics.stringWidth}). */
  stringWidth(text: string): number;
  fillRect(x: number, y: number, width: number, height: number, argb: number): void;
  /** Draws {@code text} with its baseline at {@code y}. */
  drawString(text: string, x: number, y: number, argb: number): void;
}

/**
 * Runs {@code draw} against a pen that mutates {@code image} in place, seeding the canvas with the
 * current pixels and copying the result back.
 *
 * <p>The geometry produced by the renderers is exact; only the human-readable glyphs depend on the
 * canvas font, so text is not expected to be pixel-identical to the Java AWT output.
 */
export function drawText(image: RasterImage, draw: (pen: RasterPen) => void): void {
  const { createCanvas } = loadCanvas();
  const canvas = createCanvas(image.width, image.height);
  const ctx = canvas.getContext("2d");

  const seed = ctx.createImageData(image.width, image.height);
  for (let i = 0; i < image.pixels.length; i++) {
    const p = image.pixels[i];
    seed.data[i * 4] = (p >>> 16) & 0xff;
    seed.data[i * 4 + 1] = (p >>> 8) & 0xff;
    seed.data[i * 4 + 2] = p & 0xff;
    seed.data[i * 4 + 3] = (p >>> 24) & 0xff;
  }
  ctx.putImageData(seed, 0, 0);

  const pen = createPen(ctx, image);
  draw(pen);

  const out = ctx.getImageData(0, 0, image.width, image.height).data;
  for (let i = 0; i < image.pixels.length; i++) {
    const a = out[i * 4 + 3];
    const r = out[i * 4];
    const g = out[i * 4 + 1];
    const b = out[i * 4 + 2];
    image.pixels[i] = ((a << 24) | (r << 16) | (g << 8) | b) | 0;
  }
}

function createPen(ctx: Context2DLike, image: RasterImage): RasterPen {
  const currentFont = { size: 14, family: "monospace" };

  const applyFont = () => {
    ctx.font = `${currentFont.size}px ${currentFont.family}`;
  };

  return {
    width: image.width,
    height: image.height,
    setFont(sizePx: number, family = "monospace"): void {
      currentFont.size = sizePx;
      currentFont.family = family;
      applyFont();
    },
    fontHeight(): number {
      applyFont();
      const metrics = ctx.measureText("Mg");
      const ascent = metrics.fontBoundingBoxAscent ?? metrics.actualBoundingBoxAscent ?? 0;
      const descent = metrics.fontBoundingBoxDescent ?? metrics.actualBoundingBoxDescent ?? 0;
      const height = ascent + descent;
      return height > 0 ? height : currentFont.size;
    },
    fontAscent(): number {
      applyFont();
      const metrics = ctx.measureText("0123456789");
      const ascent = metrics.actualBoundingBoxAscent ?? metrics.fontBoundingBoxAscent ?? 0;
      return ascent > 0 ? ascent : currentFont.size;
    },
    stringWidth(text: string): number {
      applyFont();
      return ctx.measureText(text).width;
    },
    fillRect(x: number, y: number, width: number, height: number, argb: number): void {
      ctx.fillStyle = toCss(argb);
      ctx.fillRect(x, y, width, height);
    },
    drawString(text: string, x: number, y: number, argb: number): void {
      applyFont();
      ctx.fillStyle = toCss(argb);
      ctx.fillText(text, x, y);
    },
  };
}

function toCss(argb: number): string {
  const rgb = argb & 0xffffff;
  return `#${rgb.toString(16).padStart(6, "0")}`;
}
