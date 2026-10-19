package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Width-coded rendering strategy (bit = dark module), natively reproducing the legacy
 * {@code WidthCodedPainter} for both raster and SVG output.
 */
public final class WidthCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();

        int width = bits.length * barWidth;
        width += 20 * barWidth; // 20 x Xdimension is quiet zone width

        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.clearRect(0, 0, width, barHeight);

        int x = 10 * barWidth; // 10 x Xdimension is leading quiet zone
        for (boolean bit : bits) {
            g2d.setColor(bit ? Color.BLACK : Color.WHITE);
            g2d.fillRect(x, 0, barWidth, barHeight);
            x += barWidth;
        }
        return result;
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        StringBuilder res = new StringBuilder();

        int totalWidth = calcTotalWidth((LinearSymbol) symbol, barWidth, barHeight, spec.wideRatio());

        res.append("<path fill=\"#fff\" d=\"M0 0h");
        res.append(totalWidth);
        res.append("v");
        res.append(barHeight + 4);
        res.append("H0z\"/>");

        res.append("<path d=\"M");
        res.append(10 * barWidth);

        int width = 0;
        int white = 0;
        int relY = 2;
        for (boolean bit : bits) {
            if (!bit) {
                white += barWidth;
            } else {
                if (white > 0 && width > 0) {
                    res.append(" ");
                    res.append(relY);
                    res.append("h");
                    res.append(width);
                    res.append("v");
                    res.append(barHeight);
                    res.append("h-");
                    res.append(width);
                    res.append("zm");
                    res.append(white + width);
                    relY = 0;
                    width = 0;
                    white = 0;
                }
                width += barWidth;
            }
        }
        if (width > 0) {
            res.append(" 0h");
            res.append(width);
            res.append("v");
            res.append(barHeight);
            res.append("h-");
            res.append(width);
            res.append("z\"/>");
        }
        return res.toString();
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        int res = symbol.elements().size() * barWidth;
        res += 20 * barWidth; // 20 x Xdimension is quiet zone width
        return res;
    }
}
