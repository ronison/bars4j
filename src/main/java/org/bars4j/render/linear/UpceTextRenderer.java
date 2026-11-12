package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.util.Escapes;

/**
 * UPC-E specific text layout (number system + 6-digit group + check digit).
 */
public final class UpceTextRenderer implements LinearTextRenderer {

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        paintText(barcode, text, nWidth, null);
    }

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth, RenderSpec spec) {
        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        Color foreground = spec != null && spec.foreground() != null ? spec.foreground() : Color.BLACK;
        // The label is normally 8 digits; without the check digit it has 7. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException (mirrors paintTextSvg).
        String padded = text.length() >= 8 ? text : (text + "00000000").substring(0, 8);
        Graphics g2d = barcode.getGraphics();
        g2d.setFont(SlotText.font(nWidth, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        int textWidht = nWidth * 42;
        int textHeight = fm.getHeight();
        int textYPos = barcode.getHeight() - textHeight / 10;

        int x = nWidth * 13;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(background);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        // Digit group: 6 slots of 7 modules (px) between the left and right guard bars. Each digit
        // is centred in its own slot so the group spans exactly those 42px whatever the font
        // metrics of the platform are (the previous single-string drawing let the last digit drift
        // into the guard bars).
        g2d.fillRect(x, y, textWidht, textHeight);
        SlotText.draw(g2d, padded.substring(1, 7), x, textWidht, textYPos, foreground);
        // Number system digit sits in the leading quiet zone (8px slot).
        SlotText.draw(g2d, padded.substring(0, 1), 0, 8 * nWidth, textYPos, foreground);

        // Check digit sits in the trailing quiet zone (8px slot, aligned with the SVG output).
        g2d.setColor(background);
        g2d.fillRect(nWidth * 61, y, 8 * nWidth, textHeight);
        SlotText.draw(g2d, padded.substring(7), nWidth * 61, 8 * nWidth, textYPos, foreground);
        g2d.dispose();
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        return paintTextSvg(text, barHeight, nWidth, null);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth, RenderSpec spec) {
        // The label is normally 8 digits; without the check digit it has 7. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException.
        String padded = text.length() >= 8 ? text : (text + "00000000").substring(0, 8);
        String t1 = padded.substring(0, 1);
        String t2 = padded.substring(1, 7);
        String t3 = padded.substring(7, 8);
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
        res.append("h43v14H13z\"/>");

        res.append("<text x=\"0\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(Escapes.text(t1));
        res.append("</text>");
        res.append("<text x=\"13\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"43\">");
        res.append(Escapes.text(t2));
        res.append("</text>");
        res.append("<text x=\"61\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(Escapes.text(t3));
        res.append("</text>");
        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
