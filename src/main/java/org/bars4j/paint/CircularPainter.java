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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.bars4j.encode.BarSet;



public class CircularPainter implements BarcodePainter {	    
    private static BarcodePainter instance; 
    
    private CircularPainter (){
        
    }
        
    public static BarcodePainter getInstance(){
        if(instance == null){
            instance = new CircularPainter ();
        }
        return instance;
    }
    
    /* (non-Javadoc)
     * @see org.jbarcode.BarcodePainter#paint(br.ronison.util.BitSet[])
     */
    public BufferedImage paintToImg(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
        float width = 0;
        float wideWidth = (float)(barWidth*wideRatio);
        for (int i = 0; i < barcode.length; i++) {
            for(int j = 0; j < barcode[i].length(); j++){
                if(barcode[i].get(j)){
                    width += wideWidth;
                } else {
                    width += barWidth;
                }
            }
        }
        float dim = Math.max(2*width, 2*barHeight);
        
        BufferedImage result = new BufferedImage((int)dim, (int)dim, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.BLACK);
                
        float x = 0;
        boolean flag = true;
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, Math.round(dim), Math.round(dim));
        g2d.setColor(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING , RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform at = new AffineTransform();
        at.setToTranslation(dim/2, dim/2);
        g2d.transform(at);
        x=-dim/2;
        
        for (int i = 0; i < barcode.length; i++) {
            for (int j = 0; j < barcode[i].length(); j++) {
                width = (barcode[i].get(j) ? wideWidth : barWidth);
                
                //Change color
                if(flag){
                	int pos = (int)(x +width/2);
                	
                    g2d.setStroke(new BasicStroke(width));
                    g2d.drawOval(pos,pos,Math.abs(2*pos),Math.abs(2*pos));

                    g2d.setColor(Color.WHITE);
                } else {
                	g2d.setColor(Color.BLACK);
                }
                flag = !flag;
                x += width;
            }
        }
        return result;
    }

	@Override
	public StringBuffer paintToSVG(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int calcTotalWidth(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		// TODO Auto-generated method stub
		return 0;
	}

}

