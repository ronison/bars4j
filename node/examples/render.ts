import {
  BarcodeGenerator,
  BarcodeRegistry,
  canvasAvailable,
  linearDefaults,
  LinearBarcodeFormat,
  matrixDefaults,
  type RasterImage,
} from "../src/index.ts";

/**
 * Renders the raster image for a format without requiring the optional canvas backend: when it is
 * not installed, the human-readable text is disabled for the linear formats (the bar geometry is
 * identical either way).
 */
export function rasterImage(formatId: string, text: string): RasterImage {
  if (canvasAvailable()) {
    return BarcodeGenerator.of(formatId).toImage(text);
  }
  const format = BarcodeRegistry.getInstance().get(formatId);
  if (format instanceof LinearBarcodeFormat) {
    return format.withOptions({ ...format.options(), showText: false }).toImage(text, linearDefaults());
  }
  return format.toImage(text, matrixDefaults());
}
