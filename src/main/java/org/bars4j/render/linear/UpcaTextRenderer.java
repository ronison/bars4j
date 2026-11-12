package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.util.Escapes;

/**
 * UPC-A specific text layout (number system + two 5-digit groups + check digit).
 */
public final class UpcaTextRenderer implements LinearTextRenderer {

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
        // The label is normally 12 digits; without the check digit it has 11. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException (mirrors paintTextSvg).
        String padded = text.length() >= 12 ? text : (text + "000000000000").substring(0, 12);
        Graphics g2d = barcode.getGraphics();
        g2d.setFont(SlotText.font(nWidth, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        double groupWidth = nWidth * 35;
        int textHeight = fm.getHeight();
        int textYPos = barcode.getHeight() - textHeight / 10;

        // Slot positions are relative to the bar start (offset by the leading quiet zone), so the
        // digits keep lining up with the guard bars for any quiet-zone width. The historical
        // layout assumed a quiet zone of 10 (number system at 0, left group at 20, right group at
        // 60, check digit at 106).
        int quiet = spec != null ? spec.quietZone() : 10;
        double leftGroup = (quiet + 10) * nWidth;
        double rightGroup = (quiet + 50) * nWidth;
        double checkSlot = (quiet + 96) * nWidth;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(background);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        // Left digit group: 5 slots of 7 modules between the left guard bars and the centre guard
        // bars. Each digit is centred in its own slot so the group spans exactly those 7*5 modules
        // whatever the font metrics of the platform are (the previous single-string drawing let
        // the last digit drift into the guard bars). Slot positions use the fractional module
        // width so the digits keep matching the bars when the canvas was scaled.
        g2d.fillRect(round(leftGroup), y, round(groupWidth), textHeight);
        SlotText.draw(g2d, padded.substring(1, 6), leftGroup, groupWidth, textYPos, foreground);
        // Number system digit sits in the leading quiet zone (8-module slot).
        SlotText.draw(g2d, padded.substring(0, 1), Math.max(0, quiet - 10) * nWidth, 8 * nWidth,
                textYPos, foreground);

        g2d.setColor(background);
        g2d.fillRect(round(rightGroup), y, round(groupWidth), textHeight);
        SlotText.draw(g2d, padded.substring(6, 11), rightGroup, groupWidth, textYPos, foreground);

        // Check digit sits in the trailing quiet zone (8-module slot, aligned with the SVG output
        // at x=quiet+96). When the quiet zone is too narrow to hold it the digit clips at the
        // canvas edge, mirroring the SVG text overflowing the viewBox.
        g2d.setColor(background);
        g2d.fillRect(round(checkSlot), y, round(8 * nWidth), textHeight);
        SlotText.draw(g2d, padded.substring(11), checkSlot, 8 * nWidth, textYPos, foreground);
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
        // The label is normally 12 digits; without the check digit it has 11. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException.
        String padded = text.length() >= 12 ? text : (text + "000000000000").substring(0, 12);
        String t1 = padded.substring(0, 1);
        String t2 = padded.substring(1, 6);
        String t3 = padded.substring(6, 11);
        String t4 = padded.substring(11, 12);
        int posY = (int) Math.round(barHeight * 0.9);
        StringBuilder res = new StringBuilder();

        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        res.append("<path fill=\"").append(ColorUtil.hex(background)).append("\" d=\"M0 ");
        res.append(barHeight);
        res.append("h");
        res.append(nWidth);
        res.append("v8H0z\"/>");

        res.append("<path fill=\"").append(ColorUtil.hex(background)).append("\" d=\"M20 ");
        res.append(posY);
        res.append("h36v14H20zm39 0h36v14H59z\"/>");

        res.append("<text x=\"0\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(Escapes.text(t1));
        res.append("</text>");
        res.append("<text x=\"20\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"36\">");
        res.append(Escapes.text(t2));
        res.append("</text>");
        res.append("<text x=\"59\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"36\">");
        res.append(Escapes.text(t3));
        res.append("</text><text x=\"106\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(Escapes.text(t4));
        res.append("</text>");
        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
