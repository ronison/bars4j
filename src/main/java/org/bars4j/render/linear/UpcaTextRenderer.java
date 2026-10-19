package org.bars4j.render.linear;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * UPC-A specific text layout (number system + two 5-digit groups + check digit).
 */
public final class UpcaTextRenderer implements LinearTextRenderer {

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth) {
        Graphics g2d = barcode.getGraphics();
        Font f = new Font("monospace", Font.PLAIN, 11 * nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        int textWidht = nWidth * 35;
        int textHeight = fm.getHeight();
        int textXShift = (textWidht - fm.stringWidth("00000") + (2 * nWidth)) / 2;
        int textYPos = barcode.getHeight() - textHeight / 10;

        int x = nWidth * 20;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text.substring(0, 1), 8 * nWidth - fm.stringWidth("0"), textYPos);
        g2d.drawString(text.substring(1, 6), x + textXShift, textYPos);

        x = nWidth * 59;
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, textWidht, textHeight);
        g2d.setColor(Color.BLACK);
        g2d.drawString(text.substring(6, 11), x + textXShift, textYPos);

        // check digit
        g2d.drawString(text.substring(11), barcode.getWidth() - (8 * nWidth), textYPos);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int nWidth) {
        String t1 = text.substring(0, 1);
        String t2 = text.substring(1, 6);
        String t3 = text.substring(6, 11);
        String t4 = text.substring(11, 12);
        int posY = (int) Math.round(barHeight * 0.9);
        StringBuilder res = new StringBuilder();

        res.append("<path fill=\"#fff\" d=\"M0 ");
        res.append(barHeight);
        res.append("h");
        res.append(nWidth);
        res.append("v8H0z\"/>");

        res.append("<path fill=\"#fff\" d=\"M20 ");
        res.append(posY);
        res.append("h36v14H20zm39 0h36v14H59z\"/>");

        res.append("<text x=\"0\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(t1);
        res.append("</text>");
        res.append("<text x=\"20\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"36\">");
        res.append(t2);
        res.append("</text>");
        res.append("<text x=\"59\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"36\">");
        res.append(t3);
        res.append("</text><text x=\"106\" y=\"");
        res.append(posY + 10);
        res.append("\" class=\"small\" textLength=\"8\">");
        res.append(t4);
        res.append("</text>");
        return res.toString();
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return (int) Math.ceil((barHeight * 0.9) + fontSize);
    }
}
