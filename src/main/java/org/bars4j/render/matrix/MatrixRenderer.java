package org.bars4j.render.matrix;

import java.awt.image.BufferedImage;

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
        int total = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
        BufferedImage image = new BufferedImage(total, total, BufferedImage.TYPE_INT_RGB);

        int background = spec.background().getRGB();
        int foreground = spec.foreground().getRGB();
        for (int y = 0; y < total; y++) {
            for (int x = 0; x < total; x++) {
                image.setRGB(x, y, background);
            }
        }
        for (int row = 0; row < matrix.heightInModules(); row++) {
            for (int col = 0; col < matrix.widthInModules(); col++) {
                if (matrix.module(row, col)) {
                    int x0 = (col + matrix.quietZone()) * moduleSize;
                    int y0 = (row + matrix.quietZone()) * moduleSize;
                    for (int y = y0; y < y0 + moduleSize; y++) {
                        for (int x = x0; x < x0 + moduleSize; x++) {
                            image.setRGB(x, y, foreground);
                        }
                    }
                }
            }
        }
        return image;
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        MatrixSymbol matrix = (MatrixSymbol) symbol;
        int moduleSize = spec.moduleSize();
        int total = (matrix.widthInModules() + 2 * matrix.quietZone()) * moduleSize;
        String foreground = toHex(spec.foreground().getRGB());
        String background = toHex(spec.background().getRGB());

        StringBuilder svg = new StringBuilder();
        svg.append("<rect width=\"").append(total).append("\" height=\"").append(total)
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
