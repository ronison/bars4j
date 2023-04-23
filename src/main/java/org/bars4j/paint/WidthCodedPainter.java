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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.bars4j.encode.BarSet;




/**
 * TODO: Description.
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class WidthCodedPainter implements BarcodePainter {
    
    private static BarcodePainter instance;
    
    private Color fgColor;
    
    private Color bgColor;
    
    private WidthCodedPainter(){
        this.setFgColor(Color.BLACK);
        this.setBgColor(Color.WHITE);
    }
    
    public static BarcodePainter getInstance(){
        if(instance == null){
            instance = new WidthCodedPainter();
        }
        return instance;
    }
    
    /* (non-Javadoc)
	 * @see org.jbarcode.BarcodePainter#paint(br.ronison.util.BitSet[])
	 */
	public BufferedImage paintToImg(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		int width = 0;
        for (int i = 0; i < barcode.length; i++) {
            width += barcode[i].length();
        }
        width = width*barWidth;
        width += 20*barWidth; //20 x Xdimension is quiet zone width
        
        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setBackground(getBgColor());
        g2d.clearRect(0, 0, width, barHeight);
        
        int x = 10*barWidth; //10 x Xdimension is leading quiet zone
        for (int i = 0; i < barcode.length; i++) {
            for (int j = 0; j < barcode[i].length(); j++) {
                if(barcode[i].get(j)){
                    g2d.setColor(getFgColor());
                } else {
                    g2d.setColor(getBgColor());
                }
                g2d.fillRect(x, 0, barWidth, barHeight);
                x += barWidth;
            }
        }
		return result;
	}

	@Override
	public StringBuffer paintToSVG(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		StringBuffer res = new StringBuffer();
		
		int totalWidth = calcTotalWidth(barcode, barWidth, barHeight, wideRatio);
		
        res.append("<rect x=\"0\" y=\"0\" width=\"");
		res.append(totalWidth);
		res.append("\" height=\"");
		res.append(barHeight + 4);
		res.append("\" fill=\"white\" />\n"); //TODO: set bgcolor
        
		int pos = 10*barWidth;
		for (int i = 0; i < barcode.length; i++) {
			BarSet bars = barcode[i];
			int width = 0;
			for (int j = 0; j < bars.length(); j++) {
				if(bars.get(j)) {
					width+=barWidth;
				} else {
					if(width > 0) {
						res.append("<rect x=\"");
						res.append(pos-width);
						res.append("\"  y=\"2\" width=\"");
						res.append(width);
						res.append("\" height=\"");
						res.append(barHeight);
						res.append("\" fill=\"");
						res.append("black"); //TODO: set fgcolor
						res.append("\" />\n");
					}
					width = 0;
				}
				pos+=barWidth;
			}
			if(width > 0) {
				res.append("<rect x=\"");
				res.append(pos-width);
				res.append("\"  y=\"2\" width=\"");
				res.append(width);
				res.append("\" height=\"");
				res.append(barHeight);
				res.append("\" fill=\"");
				res.append("black"); //TODO: set fgcolor
				res.append("\" />\n");
			}
		}
		return res;
	}

	@Override
	public int calcTotalWidth(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		int res = 0;
        for (int i = 0; i < barcode.length; i++) {
            res += barcode[i].length();
        }
        res *= barWidth;
        res += 20*barWidth; //20 x Xdimension is quiet zone width
		return res;
	}

	public Color getBgColor() {
		return bgColor;
	}

	public void setBgColor(Color bgColor) {
		this.bgColor = bgColor;
	}

	public Color getFgColor() {
		return fgColor;
	}

	public void setFgColor(Color fgColor) {
		this.fgColor = fgColor;
	}

}
