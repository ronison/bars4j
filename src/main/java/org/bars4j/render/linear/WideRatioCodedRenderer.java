package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * Wide-to-narrow ratio rendering strategy (bit = wide element, colour alternates starting dark),
 * natively reproducing the legacy {@code WideRatioCodedPainter} for both raster and SVG output.
 */
public final class WideRatioCodedRenderer implements LinearRenderer {

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        boolean[] bits = SymbolBits.wide((LinearSymbol) symbol);
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        double wideRatio = spec.wideRatio();

        int width = 20 * barWidth; // 20 x Xdimension is quiet zone width
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        for (boolean bit : bits) {
            width += bit ? wideWidth : barWidth;
        }

        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.BLACK);
        g2d.clearRect(0, 0, width, barHeight);

        int x = 10 * barWidth; // 10 x Xdimension is leading quiet zone
        boolean flag = true;
        for (boolean bit : bits) {
            int w = bit ? wideWidth : barWidth;
            g2d.fillRect(x, 0, w, barHeight);
            g2d.setColor(flag ? Color.WHITE : Color.BLACK);
            flag = !flag;
            x += w;
        }
        return result;
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

        res.append("<path fill=\"#fff\" d=\"M0 0h");
        res.append(totalWidth);
        res.append("v");
        res.append(barHeight + 15);
        res.append("H0z\"/>");

        res.append("<path d=\"");

        int pos = 10 * barWidth;
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
        int res = 20 * barWidth; // 20 x Xdimension is quiet zone width
        int wideWidth = (int) Math.round(barWidth * wideRatio);
        boolean[] bits = SymbolBits.wide(symbol);
        for (boolean bit : bits) {
            res += bit ? wideWidth : barWidth;
        }
        return res;
    }
}
