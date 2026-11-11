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
public class HeightCodedPainter implements BarcodePainter {
    
    private static class Holder {
        static final BarcodePainter INSTANCE = new HeightCodedPainter();
    }
    
    private HeightCodedPainter(){
        
    }
    
    public static BarcodePainter getInstance(){
        return Holder.INSTANCE;
    }

    public BufferedImage paintToImg(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
        int width = calcTotalWidth(barcode, barWidth, barHeight, wideRatio);
        
        BufferedImage result = new BufferedImage(width, barHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, barHeight);
        g2d.setColor(Color.BLACK);
        int x = 10 * barWidth; //10 x Xdimension is leading quiet zone
        for (int i = 0; i < barcode.length; i++) {
            for (int j = 0; j < barcode[i].length(); j++) {
                int barUnit = barcode[i].get(j) ? barHeight : barHeight/2;
                g2d.fillRect(x, barHeight - barUnit, barWidth, barUnit);
                x += 2 * barWidth;
            }
        }        
        g2d.dispose();
        return result;
    }

	@Override
	public StringBuffer paintToSVG(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
StringBuffer res = new StringBuffer();
		
		int totalWidth = calcTotalWidth(barcode, barWidth, barHeight, wideRatio);
		int pos = 10 * barWidth;
		
		res.append("<path fill=\"#ffffff\" d=\"M0 0h");
		res.append(totalWidth);
		res.append("v");
		res.append(barHeight + 4);
		res.append("H0z\"/>");

		res.append("<path fill=\"#000000\" d=\"");
        
		int relPos = 0, relY = 10;
		int height = 0;
		for (int i = 0; i < barcode.length; i++) {
			BarSet bars = barcode[i];
			for (int j = 0; j < bars.length(); j++) {
				if(bars.get(j)) {
					height = barHeight;
					
				} else {
					height = barHeight/2;
				}
				height *= -1;
				res.append("m");
				res.append(pos-relPos);
				res.append(" ");
				res.append(relY);
				res.append("h");
				res.append(barWidth);
				res.append("v");
				res.append(height);
				res.append("h-");
				res.append(barWidth);
				res.append("z");
				relPos=pos;
				relY = 0;
				pos+=2*barWidth;
			}
		}
		res.append("\"/>");
		
		return res;
	}

	@Override
	public int calcTotalWidth(BarSet[] barcode, int barWidth, int barHeight, double wideRatio) {
		int width = 0;
        for (int i = 0; i < barcode.length; i++) {
            width += barcode[i].length();
        }
        return (width*barWidth*2) - barWidth+(20*barWidth);
	}

}
