package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Width-coded rendering strategy (bit = dark module), reproducing the
 * {@code WidthCodedPainter} for both raster and SVG output.
 *
 * <p>Colours come from the {@link RenderSpec} (foreground for dark modules, background for the
 * light modules and the canvas).
 */
public final class WidthCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);

        int width = bits.length * barWidth;
        width += 2 * quiet * barWidth; // quiet zone on each side

        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(spec.background());
        g2d.clearRect(0, 0, width, barHeight);

        int x = quiet * barWidth; // leading quiet zone
        for (boolean bit : bits) {
            g2d.setColor(bit ? spec.foreground() : spec.background());
            g2d.fillRect(x, 0, barWidth, barHeight);
            x += barWidth;
        }
        g2d.dispose();
        return result;
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int quiet = quietModules(spec);

        Graphics2D g2d = target.createGraphics();
        int x = offsetX + quiet * barWidth; // leading quiet zone
        for (boolean bit : bits) {
            g2d.setColor(bit ? spec.foreground() : spec.background());
            g2d.fillRect(x, offsetY, barWidth, barHeight);
            x += barWidth;
        }
        g2d.dispose();
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.dark((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        StringBuilder res = new StringBuilder();

        int totalWidth = calcTotalWidth((LinearSymbol) symbol, barWidth, barHeight, spec.wideRatio());

        res.append("<path fill=\"");
        res.append(ColorUtil.hex(spec.background()));
        res.append("\" d=\"M0 0h");
        res.append(totalWidth);
        res.append("v");
        res.append(barHeight + 4);
        res.append("H0z\"/>");

        res.append("<path fill=\"");
        res.append(ColorUtil.hex(spec.foreground()));
        res.append("\" d=\"M");
        res.append(quietModules(spec) * barWidth);

        int width = 0;
        int white = 0;
        int relY = 2;
        for (boolean bit : bits) {
            if (!bit) {
                white += barWidth;
            } else {
                if (white > 0 && width > 0) {
                    // The relY token completes the implicit relative moveto pair (m dx dy).
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
            res.append(" ");
            res.append(relY);
            res.append("h");
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
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio, QUIET_ZONE_MODULES);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                              int quietZoneModules) {
        int res = symbol.elements().size() * barWidth;
        res += 2 * quietZoneModules * barWidth; // quiet zone on each side
        return res;
    }
}
