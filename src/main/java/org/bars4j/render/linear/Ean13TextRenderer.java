package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import org.bars4j.render.RenderSpec;
import org.bars4j.util.Escapes;

/**
 * EAN-13 specific text layout (guard bars left visible, three text groups).
 */
public final class Ean13TextRenderer implements LinearTextRenderer {

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        paintText(barcode, text, nWidth, null);
    }

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth, RenderSpec spec) {
        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        Color foreground = spec != null && spec.foreground() != null ? spec.foreground() : Color.BLACK;
        // The label is normally 13 digits; without the check digit it has 12. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException (mirrors paintTextSvg).
        String padded = text.length() >= 13 ? text : (text + "0000000000000").substring(0, 13);
        Graphics g2d = barcode.getGraphics();
        Font f = new Font("monospace", Font.PLAIN, 11 * nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        int textWidht = (nWidth * 42);
        int textHeight = fm.getHeight();
        int textXShift = (textWidht - fm.stringWidth("000000") + (2 * nWidth)) / 2;
        int textYPos = barcode.getHeight() - textHeight / 10;

        int x = nWidth * 13;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(background);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(foreground);
        g2d.drawString(padded.substring(0, 1), 10 * nWidth - fm.stringWidth("0"), textYPos);
        g2d.drawString(padded.substring(1, 7), x + textXShift, textYPos);

        x = nWidth * 59;
        g2d.setColor(background);
        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(foreground);
        g2d.drawString(padded.substring(7), x + textXShift, textYPos);
        g2d.dispose();
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        return paintTextSvg(text, barHeight, nWidth, null);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth, RenderSpec spec) {
        // The label is normally 13 digits; without the check digit it has 12. Pad defensively so a
        // short label cannot crash with StringIndexOutOfBoundsException.
        String padded = text.length() >= 13 ? text : (text + "0000000000000").substring(0, 13);
        String t1 = padded.substring(0, 1);
        String t2 = padded.substring(1, 7);
        String t3 = padded.substring(7, 13);
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
        res.append("h43v14H13zm46 0h43v14H59z\"/>");
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
        res.append("<text x=\"59\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"43\">");
        res.append(Escapes.text(t3));
        res.append("</text>");
        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
