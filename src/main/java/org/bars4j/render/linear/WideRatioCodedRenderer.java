package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Wide-to-narrow ratio rendering strategy (bit = wide element, colour alternates starting dark),
 * reproducing the historical {@code WideRatioCodedPainter} strategy for both raster and SVG output.
 */
public class WideRatioCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.wide((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();
        int quiet = quietModules(spec);

        int width = 2 * quiet * barWidth; // quiet zone on each side
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        for (boolean bit : bits) {
            width += bit ? wideWidth : barWidth;
        }

        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(spec.background());
        g2d.setColor(spec.foreground());
        g2d.clearRect(0, 0, width, barHeight);

        int x = quiet * barWidth; // leading quiet zone
        boolean flag = true;
        for (boolean bit : bits) {
            int w = bit ? wideWidth : barWidth;
            g2d.fillRect(x, 0, w, barHeight);
            g2d.setColor(flag ? spec.background() : spec.foreground());
            flag = !flag;
            x += w;
        }
        g2d.dispose();
        return result;
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.wide((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int wideWidth = (int) Math.round(barWidth * spec.wideRatio());
        int quiet = quietModules(spec);

        Graphics2D g2d = target.createGraphics();
        g2d.setColor(spec.foreground());
        int x = offsetX + quiet * barWidth; // leading quiet zone
        boolean flag = true;
        for (boolean bit : bits) {
            int w = bit ? wideWidth : barWidth;
            g2d.fillRect(x, offsetY, w, barHeight);
            g2d.setColor(flag ? spec.background() : spec.foreground());
            flag = !flag;
            x += w;
        }
        g2d.dispose();
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.wide((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();
        StringBuilder res = new StringBuilder();

        int totalWidth = calcTotalWidth((LinearSymbol) symbol, barWidth, barHeight, wideRatio);
        int wideWidth = (int) Math.round(barWidth * wideRatio);

        res.append("<path fill=\"");
        res.append(ColorUtil.hex(spec.background()));
        res.append("\" d=\"M0 0h");
        res.append(totalWidth);
        res.append("v");
        res.append(barHeight + 4);
        res.append("H0z\"/>");

        res.append("<path fill=\"");
        res.append(ColorUtil.hex(spec.foreground()));
        res.append("\" d=\"");

        int pos = quietModules(spec) * barWidth;
        int relPos = 0;
        int relY = 2;
        int width = 0;
        boolean cFlag = true;
        for (boolean bit : bits) {
            width = bit ? wideWidth : barWidth;
            if (cFlag) {
                res.append("m");
                res.append(pos - relPos);
                res.append(" ");
                res.append(relY);
                res.append("h");
                res.append(width);
                res.append("v");
                res.append(barHeight);
                res.append("h-");
                res.append(width);
                res.append("z");
                relPos = pos;
                relY = 0;
            }
            cFlag = !cFlag;
            pos += width;
        }
        res.append("\"/>");
        return res.toString();
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio) {
        return calcTotalWidth(symbol, barWidth, barHeight, wideRatio, QUIET_ZONE_MODULES);
    }

    @Override
    public int calcTotalWidth(LinearSymbol symbol, int barWidth, int barHeight, double wideRatio,
                              int quietZoneModules) {
        int res = 2 * quietZoneModules * barWidth; // quiet zone on each side
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        boolean[] bits = SymbolBits.wide(symbol);
        for (boolean bit : bits) {
            res += bit ? wideWidth : barWidth;
        }
        return res;
    }
}
