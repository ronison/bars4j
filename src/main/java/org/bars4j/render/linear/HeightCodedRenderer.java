package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Height-coded rendering strategy (bit = full-height bar, 0 = short bar), natively reproducing the
 * legacy {@code HeightCodedPainter} (e.g. PostNet) for both raster and SVG output.
 */
public final class HeightCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();

        int width = calcTotalWidth((LinearSymbol) symbol, barWidth, barHeight, spec.wideRatio());

        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.WHITE);
        int x = 0;
        for (boolean bit : bits) {
            if (!bit) {
                g2d.fillRect(x, 0, barWidth, barHeight / 2);
            }
            x += barWidth;
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
        int pos = 2;

        res.append("<path fill=\"#fff\" d=\"M0 0h");
        res.append(totalWidth);
        res.append("v");
        res.append(barHeight + 15);
        res.append("H0z\"/>");

        res.append("<path d=\"");

        int relPos = 0;
        int relY = 10;
        for (boolean bit : bits) {
            int height = bit ? barHeight : barHeight / 2;
            height *= -1;
            res.append("m");
            res.append(pos - relPos);
            res.append(" ");
            res.append(relY);
            res.append("h");
            res.append(barWidth);
            res.append("v");
            res.append(height);
            res.append("h-");
            res.append(barWidth);
            res.append("z");
            relPos = pos;
            relY = 0;
            pos += 2 * barWidth;
        }
        res.append("\"/>");
        return res.toString();
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        return (symbol.elements().size() * barWidth * 2) - barWidth + (4 * barWidth);
    }
}
