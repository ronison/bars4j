/* ================================================================
 * Bars4J : Java Barcode Library
 * ================================================================
 *
 * Project Info:  https://github.com/ronison/bars4j
 * Project Lead:  Flavio Sampaio (contato@ronison.com.br);
 *
 * (C) Copyright 2005, by Favio Sampaio
 *
 * This library is free software; you can redistribute it and/or modify it underthe terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.bars4j.paint;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * TODO: Description.
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class BaseLineTextPainter implements TextPainter {
    
    private static TextPainter instance; 
    
    private Color textColor;
    
    private Color bgColor;
    
    private int fontSize;
    
    private BaseLineTextPainter(){
        this.setTextColor(Color.BLACK);
        this.setBgColor(Color.WHITE);
        this.fontSize = 14;
    }
    
    public static TextPainter getInstance(){
        if(instance == null){
            instance = new BaseLineTextPainter();
        }
        return instance;
    }

    public void paintText(BufferedImage barcode, String text, int nWidth) {
        Graphics g2d = barcode.getGraphics();
        
        Font f = new Font("monospace", Font.PLAIN, 10*nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();
        int h = fm.getHeight();
        int center = (barcode.getWidth() - fm.stringWidth(text))/2;
        
        g2d.setColor(getBgColor());
        //passa uma linha limpando em cima
        g2d.fillRect(0, 0, barcode.getWidth(),  barcode.getHeight()*1/20);
        //passa uma linha limpando em baixo
        g2d.fillRect(0, barcode.getHeight() - (h*9/10), barcode.getWidth(),  (h*9/10));
        
       //coloca o texto
        g2d.setColor(getTextColor());
        //texto primeiro quadrante
        g2d.drawString(text, center, barcode.getHeight() - (h/10));
    }

	@Override
	public StringBuffer paintTextSVG(StringBuffer barcode, String text, int barHeight, int nWidth) {
		StringBuffer res = new StringBuffer();
		//TEXT AREA
		res.append("<path fill=\"#fff\" d=\"M0 ");
		res.append(barHeight);
		res.append("h");
		res.append(nWidth);
		res.append("v");
		res.append(fontSize);
		res.append("H0z\"/>");
		//TEXT
		res.append("<text x=\"");
    	res.append(nWidth/2);
    	res.append("\" y=\"");
    	res.append(barHeight + fontSize-2); //14 is font size
    	res.append("\" class=\"small\" text-anchor=\"middle\">");
    	res.append(text);
    	res.append("</text>");
    	return res;
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public Color getBgColor() {
		return bgColor;
	}

	public void setBgColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	@Override
	public int calcTotalHeight(int barHeight, int fontSize) {
		return barHeight+fontSize;
	}

}
