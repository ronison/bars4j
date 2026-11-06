package org.bars4j.render.matrix;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import java.util.Arrays;

import org.bars4j.render.BarcodeRenderer;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Renders a {@link MatrixSymbol} as a grid of square modules. The quiet zone is always drawn and
 * the wide-to-narrow ratio / human-readable text are not applicable.
 */
public final class MatrixRenderer implements BarcodeRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        MatrixSymbol matrix = (MatrixSymbol) symbol;
        int moduleSize = spec.moduleSize();
        int width = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
        int height = (matrix.heightInModules() + 2 * matrix.quietZone()) * moduleSize;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int background = spec.background().getRGB();
        int foreground = spec.foreground().getRGB();
        // Build the whole raster in a plain int[] (background first, dark modules on top). For the
        // common TYPE_INT_RGB case we write straight into the image's backing DataBufferInt, so no
        // per-pixel setRGB and no final full-image copy are needed.
        DataBufferInt direct = null;
        if (image.getRaster().getDataBuffer() instanceof DataBufferInt db
                && image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel sm
                && sm.getScanlineStride() == width) {
            direct = db;
        }
        int[] pixels;
        if (direct != null && direct.getData().length >= width * height) {
            pixels = direct.getData();
        } else {
            pixels = new int[width * height];
        }
        Arrays.fill(pixels, 0, width * height, background);
        int quiet = matrix.quietZone();
        for (int row = 0; row < matrix.heightInModules(); row++) {
            int y0 = (row + quiet) * moduleSize;
            int rowBase = y0 * width;
            for (int col = 0; col < matrix.widthInModules(); col++) {
                if (matrix.module(row, col)) {
                    int x0 = (col + quiet) * moduleSize;
                    for (int y = 0; y < moduleSize; y++) {
                        int start = rowBase + y * width + x0;
                        Arrays.fill(pixels, start, start + moduleSize, foreground);
                    }
                }
            }
        }
        if (direct == null) {
            image.setRGB(0, 0, width, height, pixels, 0, width);
        }
        return image;
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        MatrixSymbol matrix = (MatrixSymbol) symbol;
        int moduleSize = spec.moduleSize();
        int width = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
        int height = (matrix.heightInModules() + 2 * matrix.quietZone()) * moduleSize;
        String foreground = toHex(spec.foreground().getRGB());
        String background = toHex(spec.background().getRGB());

        StringBuilder svg = new StringBuilder();
        svg.append("<rect width=\"").append(width).append("\" height=\"").append(height)
                .append("\" fill=\"").append(background).append("\"/>");
        svg.append("<path fill=\"").append(foreground).append("\" d=\"");
        for (int row = 0; row < matrix.heightInModules(); row++) {
            for (int col = 0; col < matrix.widthInModules(); col++) {
                if (matrix.module(row, col)) {
                    int x = (col + matrix.quietZone()) * moduleSize;
                    int y = (row + matrix.quietZone()) * moduleSize;
                    svg.append("M").append(x).append(" ").append(y)
                            .append("h").append(moduleSize).append("v").append(moduleSize)
                            .append("h-").append(moduleSize).append("z");
                }
            }
        }
        svg.append("\"/>");
        return svg.toString();
    }

    private static String toHex(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }
}
