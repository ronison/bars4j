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
        paintText(barcode, text, nWidth, null);
    }

    @Override
    public void paintText(BufferedImage barcode, String text, int nWidth, RenderSpec spec) {
        Color background = spec != null && spec.background() != null ? spec.background() : Color.WHITE;
        Color foreground = spec != null && spec.foreground() != null ? spec.foreground() : Color.BLACK;
        // The label is normally 8 digits; pad defensively so a short label cannot crash with
        // StringIndexOutOfBoundsException (mirrors paintTextSvg).
        String padded = text.length() >= 8 ? text : (text + "00000000").substring(0, 8);
        Graphics g2d = barcode.getGraphics();
        g2d.setFont(SlotText.font(nWidth, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // calc bounds
        int textWidht = nWidth * 28;
        int textHeight = fm.getHeight();
        int textYPos = barcode.getHeight() - textHeight / 10;

        int x = nWidth * 13;
        int y = barcode.getHeight() - (textHeight * 4 / 5);

        g2d.setColor(background);
        g2d.fillRect(0, 0, barcode.getWidth(), barcode.getHeight() * 1 / 20);
        g2d.fillRect(0, barcode.getHeight() - textHeight / 3, barcode.getWidth(), textHeight / 3);

        // Left digit group: 4 slots of 7 modules (px) between the left and centre guard bars. Each
        // digit is centred in its own slot so the group spans exactly those 28px whatever the font
        // metrics of the platform are (the previous single-string drawing let the last digit drift
        // into the centre guard bars).
        g2d.fillRect(x, y, textWidht, textHeight);
        SlotText.draw(g2d, padded.substring(0, 4), x, textWidht, textYPos, foreground);

        x = nWidth * 46;
        g2d.setColor(background);
        g2d.fillRect(x, y, textWidht, textHeight);
        SlotText.draw(g2d, padded.substring(4), x, textWidht, textYPos, foreground);
        g2d.dispose();
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
