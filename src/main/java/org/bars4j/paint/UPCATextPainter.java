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
 * @author Flávio Sampaio
 * @since 0.1
 */
public class UPCATextPainter implements TextPainter {
    
    private static TextPainter instance; 
    
    private UPCATextPainter(){
        
    }
    
    public static TextPainter getInstance(){
        if(instance == null){
            instance = new UPCATextPainter();
        }
        return instance;
    }

    public void paintText(BufferedImage barcode, String text, int nWidth) {
        Graphics g2d = barcode.getGraphics();
        Font f = new Font("monospace", Font.PLAIN, 11*nWidth);
        g2d.setFont(f);
        FontMetrics fm = g2d.getFontMetrics();
        
        //calc bounds
        int textWidht = nWidth*35;
        int textHeight = fm.getHeight();
        int textXShift = (textWidht - fm.stringWidth("00000") + (2*nWidth))/2;
        int textYPos = barcode.getHeight() - textHeight/10;
        
        int x = nWidth*20;
        int y = barcode.getHeight() - (textHeight*4/5);
        
        //passa uma linha limpando em cima
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, barcode.getWidth(),  barcode.getHeight()*1/20);
        //passa uma linha limpando em baixo
        g2d.fillRect(0, barcode.getHeight() - textHeight/3, barcode.getWidth(),  textHeight/3);
        
        //limpa o primeiro quadrante
        g2d.fillRect(x, y, textWidht,  textHeight);
        //coloca o texto
        g2d.setColor(Color.BLACK);
        //primeiro caractere
        g2d.drawString(text.substring(0, 1), 8*nWidth-fm.stringWidth("0"), textYPos);
        //texto primeiro quadrante
        g2d.drawString(text.substring(1, 6), x+textXShift, textYPos);
        
        //limpa o segundo quadrante
        x = nWidth*59;  
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y, textWidht,  textHeight);
        //texto segundo quadrante
        g2d.setColor(Color.black);
        g2d.drawString(text.substring(6, 11), x+textXShift, textYPos);
        
        //digito verificador
        g2d.drawString(text.substring(11), barcode.getWidth() - (8*nWidth), textYPos);
    }

	@Override
	public StringBuffer paintTextSVG(StringBuffer barcode, String text, int barHeight, int nWidth) {
		String t1 = text.substring(0, 1);
    	String t2 = text.substring(1, 6);
    	String t3 = text.substring(6, 11);
    	String t4 = text.substring(11, 12);
    	int posY = (int) Math.round(barHeight * 0.9);
    	StringBuffer res = new StringBuffer();
    	
    	res.append("<path fill=\"#fff\" d=\"M0 ");
    	res.append(barHeight);
    	res.append("h");
    	res.append(nWidth);
    	res.append("v8H0z\"/>");
    	
    	res.append("<path fill=\"#fff\" d=\"M20 ");
    	res.append(posY);
    	res.append("h36v14H20zm39 0h36v14H59z\"/>");
    	
    	res.append("<text x=\"0\" y=\"");
    	res.append(posY+10);
    	res.append("\" class=\"small\" textLength=\"8\">");
    	res.append(t1);
    	res.append("</text>");
    	res.append("<text x=\"20\" y=\"");
    	res.append(posY+10);
    	res.append("\" class=\"small\" textLength=\"36\">");
    	res.append(t2);
    	res.append("</text>");
    	res.append("<text x=\"59\" y=\"");
    	res.append(posY+10);
    	res.append("\" class=\"small\" textLength=\"36\">");
    	res.append(t3);
    	res.append("</text><text x=\"106\" y=\"");
    	res.append(posY+10);
    	res.append("\" class=\"small\" textLength=\"8\">");
    	res.append(t4);
    	res.append("</text>");
    	return res;
	}

	@Override
	public int calcTotalHeight(int barHeight, int fontSize) {
		return (int) Math.ceil((barHeight * 0.9) + fontSize);
	}

}
