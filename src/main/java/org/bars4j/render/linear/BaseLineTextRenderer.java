package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Draws a single centred line of text under the barcode (default for most linear formats).
 */
public final class BaseLineTextRenderer implements LinearTextRenderer {

    private static final int FONT_SIZE = 14;

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        Graphics g2d = barcode.getGraphics();
        Font f = new Font("monospace", Font.PLAIN, 10 * nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();
        int h = fm.getHeight();
        int center = (barcode.getWidth() - fm.stringWidth(text)) / 2;

        g2d.setColor(Color.WHITE);
        // clear a line at the top
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        // clear a line at the bottom
        g2d.fillRect(0, barcode.getHeight() - (h * 9 / 10), barcode.getWidth(), (h * 9 / 10));

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, center, barcode.getHeight() - (h / 10));
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        StringBuilder res = new StringBuilder();
        // TEXT AREA
        res.append("<path fill=\"#fff\" d=\"M0 ");
        res.append(barHeight);
        res.append("h");
        res.append(nWidth);
        res.append("v");
        res.append(FONT_SIZE);
        res.append("H0z\"/>");
        // TEXT
        res.append("<text x=\"");
        res.append(nWidth / 2);
        res.append("\" y=\"");
        res.append(barHeight + FONT_SIZE - 2);
        res.append("\" class=\"small\" text-anchor=\"middle\">");
        res.append(text);
        res.append("</text>");
        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return barHeight + fontSize;
    }
}
