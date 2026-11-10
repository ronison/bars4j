import { createHash } from "node:crypto";

/**
 * A raster image produced by the renderers.
 *
 * <p>Pixels are packed as 32-bit ARGB integers ({@code 0xAARRGGBB}), stored row-major with no
 * padding, exactly like a {@code java.awt.image.BufferedImage} of type {@code TYPE_INT_RGB}. This
 * keeps the golden fingerprints (SHA-256 of the ARGB pixels) identical between the Java and the
 * Node implementation and lets the raster path stay free of any native dependency.
 */
export interface RasterImage {
  /** Image width in pixels. */
  readonly width: number;
  /** Image height in pixels. */
  readonly height: number;
  /** Packed ARGB pixels, {@code width * height} entries, row-major from the top-left corner. */
  readonly pixels: Int32Array;
}

/** Opaque black ({@code 0xFF000000}). */
export const ARGB_BLACK = 0xff000000;
/** Opaque white ({@code 0xFFFFFFFF}). */
export const ARGB_WHITE = 0xffffffff;

/**
 * Parses a CSS-ish colour ({@code #rgb}, {@code #rrggbb} or {@code #aarrggbb}) into an opaque
 * ARGB integer.
 */
export function parseArgb(hex: string): number {
  let value = hex.trim();
  if (value.startsWith("#")) {
    value = value.slice(1);
  }
  if (value.length === 3) {
    value = value[0] + value[0] + value[1] + value[1] + value[2] + value[2];
  }
  if (value.length === 6) {
    return (0xff000000 | Number.parseInt(value, 16)) >>> 0;
  }
  if (value.length === 8) {
    return Number.parseInt(value, 16) >>> 0;
  }
  throw new Error(`Unsupported colour: ${hex}`);
}

/** Allocates an image of the given size, filled with {@code background} (an ARGB integer). */
export function createRasterImage(width: number, height: number, background: number): RasterImage {
  if (!Number.isInteger(width) || !Number.isInteger(height) || width <= 0 || height <= 0) {
    throw new Error(`Invalid raster size: ${width}x${height}`);
  }
  const pixels = new Int32Array(width * height);
  pixels.fill(background | 0);
  return { width, height, pixels };
}

/** Fills an axis-aligned rectangle, clipping it to the image bounds (Java {@code fillRect}). */
export function fillRect(
  image: RasterImage,
  x: number,
  y: number,
  width: number,
  height: number,
  argb: number,
): void {
  if (width <= 0 || height <= 0) {
    return;
  }
  const x0 = Math.max(0, Math.trunc(x));
  const y0 = Math.max(0, Math.trunc(y));
  const x1 = Math.min(image.width, Math.trunc(x) + Math.trunc(width));
  const y1 = Math.min(image.height, Math.trunc(y) + Math.trunc(height));
  const value = argb | 0;
  for (let row = y0; row < y1; row++) {
    const base = row * image.width;
    image.pixels.fill(value, base + x0, base + x1);
  }
}

/** Reads one ARGB pixel ({@code 0xAARRGGBB}, unsigned). */
export function getArgb(image: RasterImage, x: number, y: number): number {
  return image.pixels[y * image.width + x] >>> 0;
}

/** Copies an opaque ARGB image into {@code target} at the given offset (Java {@code drawImage}). */
export function blit(target: RasterImage, source: RasterImage, offsetX: number, offsetY: number): void {
  for (let row = 0; row < source.height; row++) {
    const dy = offsetY + row;
    if (dy < 0 || dy >= target.height) {
      continue;
    }
    const srcBase = row * source.width;
    for (let col = 0; col < source.width; col++) {
      const dx = offsetX + col;
      if (dx < 0 || dx >= target.width) {
        continue;
      }
      target.pixels[dy * target.width + dx] = source.pixels[srcBase + col];
    }
  }
}

/**
 * Draws {@code source} into {@code target} through an affine scale + translation, using
 * nearest-neighbour sampling. Reproduces the legacy {@code Graphics2D.drawRenderedImage(img, at)}
 * pipeline of the {@code LinearBarcodeFormat} facade (whose default interpolation hint is
 * nearest-neighbour).
 */
export function drawScaled(
  target: RasterImage,
  source: RasterImage,
  scaleX: number,
  scaleY: number,
  offsetX: number,
  offsetY: number,
): void {
  for (let dy = 0; dy < target.height; dy++) {
    const sy = Math.floor((dy + 0.5 - offsetY) / scaleY);
    if (sy < 0 || sy >= source.height) {
      continue;
    }
    const srcBase = sy * source.width;
    const dstBase = dy * target.width;
    for (let dx = 0; dx < target.width; dx++) {
      const sx = Math.floor((dx + 0.5 - offsetX) / scaleX);
      if (sx < 0 || sx >= source.width) {
        continue;
      }
      target.pixels[dstBase + dx] = source.pixels[srcBase + sx];
    }
  }
}

/**
 * Computes the golden fingerprint of an image: the SHA-256 of the big-endian width, height and
 * every ARGB pixel. Mirrors {@code GoldenTestSupport#fingerprint} in the Java tests.
 */
export function fingerprint(image: RasterImage): string {
  const digest = createHash("sha256");
  const header = Buffer.alloc(8);
  header.writeInt32BE(image.width, 0);
  header.writeInt32BE(image.height, 4);
  digest.update(header);

  // Hash the pixels in chunks to avoid materialising a second copy of large images.
  const chunk = new Int32Array(8192);
  const chunkBytes = Buffer.alloc(chunk.length * 4);
  for (let start = 0; start < image.pixels.length; start += chunk.length) {
    const end = Math.min(start + chunk.length, image.pixels.length);
    for (let i = start; i < end; i++) {
      chunkBytes.writeInt32BE(image.pixels[i], (i - start) * 4);
    }
    digest.update(chunkBytes.subarray(0, (end - start) * 4));
  }

  const hex = digest.digest("hex");
  return `width=${image.width}\nheight=${image.height}\nsha256=${hex}\n`;
}
