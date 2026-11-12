package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.util.Escapes;

/**
 * EAN-8 specific text layout (two 4-digit groups).
 */
public final class Ean8TextRenderer implements LinearTextRenderer {

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        paintText(barcode, text, (double) nWidth, null);
    }

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth, RenderSpec spec) {
        paintText(barcode, text, (double) nWidth, spec);
    }

    @Override
    public void paintText(BufferedImage barcode, String text, double nWidth, RenderSpec spec) {
        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        Color foreground = spec != null && spec.foreground() != null ? spec.foreground() : Color.BLACK;
        // The label is normally 8 digits; pad defensively so a short label cannot crash with
        // StringIndexOutOfBoundsException (mirrors paintTextSvg).
        String padded = text.length() >= 8 ? text : (text + "00000000").substring(0, 8);
        Graphics g2d = barcode.getGraphics();
        g2d.setFont(SlotText.font(nWidth, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        double groupWidth = nWidth * 28;
        int textHeight = fm.getHeight();
        int textYPos = barcode.getHeight() - textHeight / 10;

        // Slot positions are relative to the bar start (offset by the leading quiet zone), so the
        // digits keep lining up with the guard bars for any quiet-zone width. The historical
        // layout assumed a quiet zone of 10 (left group at 13, right group at 46).
        int quiet = spec != null ? spec.quietZone() : 10;
        double leftGroup = (quiet + 3) * nWidth;
        double rightGroup = (quiet + 36) * nWidth;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(background);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        // Left digit group: 4 slots of 7 modules between the left and centre guard bars. Each
        // digit is centred in its own slot so the group spans exactly those 7*4 modules whatever
        // the font metrics of the platform are (the previous single-string drawing let the last
        // digit drift into the centre guard bars). Slot positions use the fractional module width
        // so the digits keep matching the bars when the canvas was scaled.
        g2d.fillRect(round(leftGroup), y, round(groupWidth), textHeight);
        SlotText.draw(g2d, padded.substring(0, 4), leftGroup, groupWidth, textYPos, foreground);

        g2d.setColor(background);
        g2d.fillRect(round(rightGroup), y, round(groupWidth), textHeight);
        SlotText.draw(g2d, padded.substring(4), rightGroup, groupWidth, textYPos, foreground);
        g2d.dispose();
    }

    /** Rounds a slot geometry value to a pixel coordinate. */
    private static int round(double v) {
        return (int) Math.round(v);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        return paintTextSvg(text, barHeight, nWidth, null);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth, RenderSpec spec) {
        // The label is normally 8 digits; pad defensively so a short label cannot crash with
        // StringIndexOutOfBoundsException.
        String padded = text.length() >= 8 ? text : (text + "00000000").substring(0, 8);
        String t1 = padded.substring(0, 4);
        String t2 = padded.substring(4, 8);
        int posY = (int) Math.ceil(barHeight * 0.9);
        StringBuilder res = new StringBuilder();

        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        res.append("<path fill=\"").append(ColorUtil.hex(background)).append("\" d=\"M0 ");
        res.append(barHeight);
        res.append("h");
        res.append(nWidth);
        res.append("v8H0z\"/>");

        res.append("<path fill=\"").append(ColorUtil.hex(background)).append("\" d=\"M13 ");
        res.append(posY);
        res.append("h29v14H13zm32 0h29v14H45z\"/>");
        res.append("<text x=\"13\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"29\">");
        res.append(Escapes.text(t1));
        res.append("</text>");
        res.append("<text x=\"45\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"29\">");
        res.append(Escapes.text(t2));
        res.append("</text>");

        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
