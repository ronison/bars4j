package org.bars4j.render.linear;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.BarcodeSymbol;
import org.bars4j.symbol.LinearSymbol;

/**
 * ITF-14 rendering strategy.
 *
 * <p>Draws the Interleaved 2 of 5 wide/narrow bars (inherited from {@link WideRatioCodedRenderer})
 * framed by the mandatory GS1 bearer bars: a horizontal bar above and below the symbol spanning the
 * whole symbol including its quiet zones.
 */
public final class Itf14Renderer extends WideRatioCodedRenderer {

    /** Bearer-bar thickness as a fraction of the bar height. */
    private static final double BEARER_RATIO = 0.1;

    /** Vertical offset used by {@link WideRatioCodedRenderer#toSvg} for the bar baseline. */
    private static final int SVG_TOP = 2;

    @Override
    public BufferedImage toImage(BarcodeSymbol symbol, RenderSpec spec) {
        BufferedImage image = super.toImage(symbol, spec);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(spec.foreground());
        paintBearer(g2d, (LinearSymbol) symbol, spec, 0, 0);
        g2d.dispose();
        return image;
    }

    @Override
    public void renderInto(BufferedImage target, int offsetX, int offsetY, BarcodeSymbol symbol, RenderSpec spec) {
        super.renderInto(target, offsetX, offsetY, symbol, spec);
        Graphics2D g2d = target.createGraphics();
        g2d.setColor(spec.foreground());
        paintBearer(g2d, (LinearSymbol) symbol, spec, offsetX, offsetY);
        g2d.dispose();
    }

    @Override
    public String toSvg(BarcodeSymbol symbol, RenderSpec spec) {
        String bars = super.toSvg(symbol, spec);

        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int bearer = bearerWidth(barHeight);
        int fullWidth = calcTotalWidth((LinearSymbol) symbol, barWidth, barHeight, spec.wideRatio());
        int bottom = SVG_TOP + barHeight - bearer;

        StringBuilder frame = new StringBuilder("<path d=\"");
        // top bearer bar
        rect(frame, 0, SVG_TOP, fullWidth, bearer);
        // bottom bearer bar
        rect(frame, 0, bottom, fullWidth, bearer);
        frame.append("\"/>");
        return bars + frame;
    }

    private static void rect(StringBuilder sb, int x, int y, int width, int height) {
        sb.append("M").append(x).append(" ").append(y)
                .append("h").append(width)
                .append("v").append(height)
                .append("h-").append(width).append("z");
    }

    private void paintBearer(Graphics2D g2d, LinearSymbol symbol, RenderSpec spec, int offsetX, int offsetY) {
        int barWidth = spec.barWidth();
        int barHeight = spec.barHeight();
        int bearer = bearerWidth(barHeight);
        int fullWidth = calcTotalWidth(symbol, barWidth, barHeight, spec.wideRatio());
        int bottom = offsetY + barHeight - bearer;

        // GS1 ITF-14 bearer bars: a horizontal bar above and below the symbol, spanning the bars
        // plus the quiet zones (the full width of the symbol).
        g2d.fillRect(offsetX, offsetY, fullWidth, bearer);   // top
        g2d.fillRect(offsetX, bottom, fullWidth, bearer);    // bottom
    }

    private static int bearerWidth(int barHeight) {
        return Math.max(2, (int) Math.round(barHeight * BEARER_RATIO));
    }
}
