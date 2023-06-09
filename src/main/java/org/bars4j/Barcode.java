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
package org.bars4j;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.bars4j.encode.BarSet;
import org.bars4j.encode.BarcodeEncoder;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.paint.BarcodePainter;
import org.bars4j.paint.TextPainter;





/**
 * A barcode class implemented using Java 2D APIs.
 * <br>
 * Barcode coordinates three objects - <code>BarcodeEncoder</code> 
 * encode a text, <code>BarcodePaint</code> implements a stratergy to
 * paint the code, and <code>TextPainter</code> implements the barcode
 * text drawing - and several attibutes to achieve its aim of being able
 * to draw a barcode on a Java 2D graphics device.  
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class Barcode {
    
    public static final double MIN_XDIMENSION = 0.264583333;

	private int barHeight;

	private double wideToNarrow;

    private int xdimension;
    
    private int fontSize;

	private boolean showText;

	private boolean checkDigit;

	private boolean showCheckDigit;

	private BarcodeEncoder bcencoder;

	private BarcodePainter bcpainter;

    private TextPainter textpainter;
    
    private double module;

    /**
     * TODO: Description.
     * 
     * @param encoder
     * @param painter
     * @param textpainter
     * @param wnRatio
     * @param nWidth
     * @param barHeight
     */
    public Barcode(BarcodeEncoder encoder, BarcodePainter painter, TextPainter textpainter, double xdimension, int barHeight, double wideRatio){
        this.bcencoder = encoder;
        this.bcpainter = painter;
        this.textpainter = textpainter;
        this.barHeight = barHeight;
        this.xdimension = (int)(xdimension/MIN_XDIMENSION);
        this.xdimension = this.xdimension > 0 ? this.xdimension : 1;
        this.module = xdimension;
        this.wideToNarrow = wideRatio;
        this.showCheckDigit = true;
        this.checkDigit = true;
        this.showText = true;
        this.fontSize = 14;
    }

    /**
     * TODO: Description.
     * 
     * @param encoder
     * @param painter
     * @param textpainter
     */
    public Barcode(BarcodeEncoder encoder, BarcodePainter painter, TextPainter textpainter){
        this(encoder, painter, textpainter, MIN_XDIMENSION, 70, 2.0);
    }
    
    /**
     * TODO: Description.
     * 
     * @param code
     * @return
     * @throws InvalidAtributeException
     */
    public BufferedImage createBarcode(String code) throws InvalidAtributeException{
        String tmp = new String(code);
        if(isCheckDigit()){
            code = code + bcencoder.computeCheckSum(code);
            if(isShowCheckDigit()){
                tmp = code;
            }
            
        }
        BarSet [] encoded = bcencoder.encode(code);
        BufferedImage img = bcpainter.paintToImg(encoded, 1, barHeight, wideToNarrow);
        
        BufferedImage result = new BufferedImage((int)(img.getWidth()*(module/(MIN_XDIMENSION)))+1, (int)getBarHeight() + 8, BufferedImage.TYPE_INT_RGB);
        AffineTransform at = new AffineTransform((module/(MIN_XDIMENSION)), 0.0, 0.0, (double)getBarHeight()/img.getHeight(), 0.0, 4.0);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, result.getWidth(), result.getHeight());
        g2d.drawRenderedImage(img, at);
        
        if(isShowText()){
        	textpainter.paintText(result, tmp, 1);
        }
	
        return result;
    }
    
    /**
     * TODO: Description.
     * 
     * @param code
     * @return
     * @throws InvalidAtributeException
     */
    public StringBuffer createBarcodeSVG(String code) throws InvalidAtributeException{
        StringBuffer tmp = new StringBuffer(code);
        StringBuffer label = new StringBuffer(code);
        if(isCheckDigit()){
            tmp.append(bcencoder.computeCheckSum(code));
            if(isShowCheckDigit()){
                label = tmp;
            }
            
        }
        BarSet [] encoded = bcencoder.encode(tmp.toString());
        
        int totalWidth = bcpainter.calcTotalWidth(encoded, xdimension, barHeight, wideToNarrow);
        int totalHeight = barHeight+4; //2px top margin + 2px bottom margin
        
        StringBuffer pcode = bcpainter.paintToSVG(encoded, 1, barHeight, wideToNarrow);
        
        if(isShowText()){
        	pcode.append(textpainter.paintTextSVG(pcode, label.toString(), barHeight, totalWidth));
        	totalHeight = textpainter.calcTotalHeight(barHeight, fontSize);
        }
        
        StringBuffer res = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
        		+ "<!-- Created with Bars4J -->\n"
        		+ "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"");
        res.append(totalWidth);
        res.append("\" height=\"");
        res.append(totalHeight);
        res.append("\" preserveAspectRatio=\"x");
        res.append(totalWidth);
        res.append("Y");
        res.append(totalHeight);
        res.append(" meet\"><style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>");
        
        res.append(pcode);
        
        res.append("Sorry, your browser does not support inline SVG.</svg>");
       
        return res;
    }
        
    
    /**
     * TODO: Description.
     * 
     * @param text
     * @return
     * @throws InvalidAtributeException
     */
    public String calcCheckSum(String text) throws InvalidAtributeException{
        return bcencoder.computeCheckSum(text);
    }

    /**
     * Get the bar height value (in millimeter).
     * 
     * @return Returns the barHeigth.
     */
    public double getBarHeight() {
        return barHeight;
    }

    /**
     * Set the bar height value (in millimeter).
     * 
     * @param barHeigth The bar height value (in millimeter).
     */
    public void setBarHeight(double barHeight) {
        this.barHeight = (int)Math.round(barHeight/MIN_XDIMENSION);
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the bcencoder.
     */
    public BarcodeEncoder getEncoder() {
        return bcencoder;
    }

    /**
     * TODO: Description.
     * 
     * @param bcencoder The bcencoder to set.
     */
    public void setEncoder(BarcodeEncoder bcencoder) {
        this.bcencoder = bcencoder;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the bcpainter.
     */
    public BarcodePainter getPainter() {
        return bcpainter;
    }

    /**
     * TODO: Description.
     * 
     * @param bcpainter The bcpainter to set.
     */
    public void setPainter(BarcodePainter bcpainter) {
        this.bcpainter = bcpainter;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the wideToNarrow.
     */
    public double getWideRatio() {
        return wideToNarrow;
    }

    /**
     * TODO: Description.
     * 
     * @param wideToNarrow The wideToNarrow to set.
     * @throws InvalidAtributeException 
     */
    public void setWideRatio(double wideRatio) throws InvalidAtributeException {
        if(wideRatio < 1){
            throw new InvalidAtributeException("[JBarcode] Invalid wide to narrow ratio value.");
        }    
        this.wideToNarrow = wideRatio;
    }

    /**
     * Get the narrowest bar width value (in millimeter).
     * 
     * @return Returns the narrowWidth.
     */
    public double getXDimension() {
        return module;
    }

    /**
     * Set the narrowest bar width value (in millimeter).
     * 
     * @param xdimension The narrowest bar width value (in millimeter).
     */
    public void setXDimension(double xdimension) throws InvalidAtributeException {
        if(xdimension < 0){
            throw new InvalidAtributeException("[JBarcode] Invalid x-dimention value.");
        }        
        this.xdimension = (int)(xdimension/MIN_XDIMENSION);
        this.xdimension = this.xdimension > 0 ? this.xdimension : 1;
        this.module = xdimension;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the checkDigit.
     */
    public boolean isCheckDigit() {
        return checkDigit;
    }

    /**
     * TODO: Description.
     * 
     * @param checkDigit The checkDigit to set.
     */
    public void setCheckDigit(boolean checkDigit) {
        this.checkDigit = checkDigit;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the showCheckDigit.
     */
    public boolean isShowCheckDigit() {
        return showCheckDigit;
    }

    /**
     * TODO: Description.
     * 
     * @param showCheckDigit The showCheckDigit to set.
     */
    public void setShowCheckDigit(boolean showCheckDigit) {
        this.showCheckDigit = showCheckDigit;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the showText.
     */
    public boolean isShowText() {
        return showText;
    }

    /**
     * TODO: Description.
     * 
     * @param showText The showText to set.
     */
    public void setShowText(boolean showText) {
        this.showText = showText;
    }

    /**
     * TODO: Description.
     * 
     * @return Returns the textpainter.
     */
    public TextPainter getTextPainter() {
        return textpainter;
    }

    /**
     * TODO: Description.
     * 
     * @param textpainter The textpainter to set.
     */
    public void setTextPainter(TextPainter textpainter) {
        this.textpainter = textpainter;
    }
    
    public int getFontSize() {
		return fontSize;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	/**
     * TODO: Description.
     */
    public String toString(){
        return bcencoder.toString();
    }
}