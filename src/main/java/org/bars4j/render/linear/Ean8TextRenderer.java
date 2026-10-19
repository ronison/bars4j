package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * EAN-8 specific text layout (two 4-digit groups).
 */
public final class Ean8TextRenderer implements LinearTextRenderer {

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        Graphics g2d = barcode.getGraphics();
        Font f = new Font("monospace", Font.PLAIN, 11 * nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        int textWidht = nWidth * 28;
        int textHeight = fm.getHeight();
        int textXShift = (textWidht - fm.stringWidth("0000") + (2 * nWidth)) / 2;
        int textYPos = barcode.getHeight() - textHeight / 10;

        int x = nWidth * 13;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text.substring(0, 4), x + textXShift, textYPos);

        x = nWidth * 45;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text.substring(4), x + textXShift, textYPos);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        String t1 = text.substring(0, 4);
        String t2 = text.substring(4, 8);
        int posY = (int) Math.ceil(barHeight * 0.9);
        StringBuilder res = new StringBuilder();

        res.append("<path fill=\"#fff\" d=\"M0 ");
        res.append(barHeight);
        res.append("h");
        res.append(nWidth);
        res.append("v8H0z\"/>");

        res.append("<path fill=\"#fff\" d=\"M13 ");
        res.append(posY);
        res.append("h29v14H13zm32 0h29v14H45z\"/>");
        res.append("<text x=\"13\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"29\">");
        res.append(t1);
        res.append("</text>");
        res.append("<text x=\"45\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"29\">");
        res.append(t2);
        res.append("</text>");

        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
