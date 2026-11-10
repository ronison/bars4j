import type { BarcodeSymbol } from "../../symbol/barcode-symbol.ts";
import type { MatrixSymbol } from "../../symbol/matrix-symbol.ts";
import type { BarcodeRenderer } from "../barcode-renderer.ts";
import { blit, createRasterImage, fillRect, parseArgb, type RasterImage } from "../raster-image.ts";
import type { RenderSpec } from "../render-spec.ts";

/**
 * Renders a {@link MatrixSymbol} as a grid of square modules. The quiet zone is always drawn and
 * the wide-to-narrow ratio / human-readable text are not applicable.
 *
 * Port of {@code org.bars4j.render.matrix.MatrixRenderer}.
 */
export class MatrixRenderer implements BarcodeRenderer {
  toImage(symbol: BarcodeSymbol, spec: RenderSpec): RasterImage {
    const matrix = symbol as MatrixSymbol;
    const moduleSize = spec.moduleSize;
    const width = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
    const height = (matrix.heightInModules() + 2 * matrix.quietZone()) * moduleSize;
    const image = createRasterImage(width, height, parseArgb(spec.background));

    const foreground = parseArgb(spec.foreground);
    const quiet = matrix.quietZone();
    for (let row = 0; row < matrix.heightInModules(); row++) {
      const y0 = (row + quiet) * moduleSize;
      for (let col = 0; col < matrix.widthInModules(); col++) {
        if (matrix.module(row, col)) {
          const x0 = (col + quiet) * moduleSize;
          fillRect(image, x0, y0, moduleSize, moduleSize, foreground);
        }
      }
    }
    return image;
  }

  renderInto(
    target: RasterImage,
    offsetX: number,
    offsetY: number,
    symbol: BarcodeSymbol,
    spec: RenderSpec,
  ): void {
    blit(target, this.toImage(symbol, spec), offsetX, offsetY);
  }


  toSvg(symbol: BarcodeSymbol, spec: RenderSpec): string {
    const matrix = symbol as MatrixSymbol;
    const moduleSize = spec.moduleSize;
    const width = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
    const height = (matrix.heightInModules() + 2 * matrix.quietZone()) * moduleSize;

    let svg = `<rect width="${width}" height="${height}" fill="${spec.background}"/>`;
    svg += `<path fill="${spec.foreground}" d="`;
    for (let row = 0; row < matrix.heightInModules(); row++) {
      for (let col = 0; col < matrix.widthInModules(); col++) {
        if (matrix.module(row, col)) {
          const x = (col + matrix.quietZone()) * moduleSize;
          const y = (row + matrix.quietZone()) * moduleSize;
          svg += `M${x} ${y}h${moduleSize}v${moduleSize}h-${moduleSize}z`;
        }
      }
    }
    svg += `"/>`;
    return svg;
  }
}
