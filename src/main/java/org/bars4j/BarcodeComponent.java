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
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import org.bars4j.encode.BarcodeEncoder;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.paint.BarcodePainter;
import org.bars4j.paint.TextPainter;





/**
 * TODO: Description.
 * 
 * @author Flávio Sampaio
 * @since 0.1
 */
public class BarcodeComponent extends JComponent implements PropertyChangeListener{

    private static final long serialVersionUID = 1L;
    
    private Barcode barcode;

	private BufferedImage img;
    private String text;
    private String checkSum;
    
    public BarcodeComponent(Barcode barcode){
        this(barcode, "");
    }
    
    public BarcodeComponent(Barcode barcode, String text){
        this.barcode = barcode;
        addPropertyChangeListener(this);
        if(text != null){
            try {
                setText(text);
            } catch (InvalidAtributeException exc) {}
        }
        this.setBackground(Color.white);
    }
    
    public void paint(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if(img != null){
            int x = (getWidth() - img.getWidth())/2;
            int y = (getHeight() -img.getHeight())/2;            
            g.drawImage(img, x, y, this);
        }
        super.paint(g);
    }



    /**
     * @return Returns the text.
     */
    public String getText() {
        return text;
    }

    /**
     * @param text The text to set.
     * @throws InvalidAtributeException 
     */
    public void setText(String text) throws InvalidAtributeException {
        if(text.equals(getText())){
            return;
        }
        String old = this.text;
        this.text = text;
        this.checkSum = barcode.calcCheckSum(text);
        firePropertyChange("Text", old, text);
        invalidate();
        repaint();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#getBarHeigth()
     */
    public double getBarHeight() {
        return barcode.getBarHeight();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#getEncoder()
     */
    public BarcodeEncoder getEncoder() {
        return barcode.getEncoder();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#getNarrowWidth()
     */
    public double getXDimension() {
        return barcode.getXDimension();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#getPainter()
     */
    public BarcodePainter getPainter() {
        return barcode.getPainter();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#getWideToNarrow()
     */
    public double getWideRatio() {
        return barcode.getWideRatio();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#setBarHeigth(int)
     */
    public void setBarHeight(double barHeight) {
        if(getBarHeight() == barHeight){
            return;
        }
        double old = getBarHeight();
        barcode.setBarHeight(barHeight);
        firePropertyChange("BarHeigth", old, barHeight);
        invalidate();
        repaint();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#setEncoder(org.barcode.encode.BarcodeEncoder)
     */
    public void setEncoder(BarcodeEncoder bcencoder) {
        if(getEncoder().equals(bcencoder)){
            return;
        }
        Object old = getEncoder();
        barcode.setEncoder(bcencoder);
        firePropertyChange("Encoder", old, bcencoder);
        invalidate();
        repaint();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#setNarrowWidth(int)
     */
    public void setXDimension(double xdim) throws InvalidAtributeException {
        if(getXDimension() == xdim){
            return;
        }
        double old = getXDimension();
        barcode.setXDimension(xdim);
        firePropertyChange("xDimension", old, xdim);
        invalidate();
        repaint();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#setPainter(org.barcode.paint.BarcodePainter)
     */
    public void setPainter(BarcodePainter bcpainter) {
        if(getPainter().equals(bcpainter)){
            return;
        }
        Object old = getPainter();
        barcode.setPainter(bcpainter);
        firePropertyChange("Painter", old, bcpainter);
        invalidate();
        repaint();
    }



    /* (non-Javadoc)
     * @see org.barcode.barcode#setWideToNarrow(int)
     */
    public void setWideRatio(double wideRatio) throws InvalidAtributeException {
        if(getWideRatio() == wideRatio){
            return;
        }
        double old = getWideRatio();
        barcode.setWideRatio(wideRatio);
        firePropertyChange("wideRatio", old, wideRatio);
        invalidate();
        repaint();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        try {
            if(text != null && !"".equals(text) && barcode != null){
                img = barcode.createBarcode(text);
            }
        } catch (InvalidAtributeException exc) {
        	//throw new RuntimeException(exc);
        }
    }

    /**
     * @return Returns the checkSum.
     */
    public String getCheckSum() {
        return checkSum;
    }

    /**
     * @param checkSum The checkSum to set.
     */
    public void setCheckSum(String checkSum) {
        firePropertyChange("checkSum", this.checkSum, this.checkSum);
        invalidate();
        repaint();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#getTextPainter()
     */
    public TextPainter getTextPainter() {
        return barcode.getTextPainter();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#isCheckDigit()
     */
    public boolean isCheckDigit() {
        return barcode.isCheckDigit();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#isShowCheckDigit()
     */
    public boolean isShowCheckDigit() {
        return barcode.isShowCheckDigit();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#isShowText()
     */
    public boolean isShowText() {
        return barcode.isShowText();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#setCheckDigit(boolean)
     */
    public void setCheckDigit(boolean checkDigit) {
        if(checkDigit == isCheckDigit()){
            return;
        }
        boolean old = isCheckDigit();
        barcode.setCheckDigit(checkDigit);
        firePropertyChange("checkDigit", old, checkDigit);
        invalidate();
        repaint();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#setShowCheckDigit(boolean)
     */
    public void setShowCheckDigit(boolean showCheckDigit) {
        if(showCheckDigit == isShowCheckDigit()){
            return;
        }
        boolean old = isShowCheckDigit();
        barcode.setShowCheckDigit(showCheckDigit);
        firePropertyChange("showCheckDigit", old, showCheckDigit);
        invalidate();
        repaint();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#setShowText(boolean)
     */
    public void setShowText(boolean showText) {
        if(showText == isShowText()){
            return;
        }
        boolean old = isShowText();
        barcode.setShowText(showText);
        firePropertyChange("showText", old, showText);
        invalidate();
        repaint();
    }

    /* (non-Javadoc)
     * @see org.barcode.barcode#setTextPainter(org.barcode.paint.TextPainter)
     */
    public void setTextPainter(TextPainter textpainter) {
        if(getTextPainter().equals(textpainter)){
            return;
        }
        Object old = getTextPainter();
        barcode.setTextPainter(textpainter);
        firePropertyChange("textPainter", old, textpainter);
        invalidate();
        repaint();
        barcode.setTextPainter(textpainter);
    }
    
    public Barcode getBarcode() {
		return barcode;
	}

	public void setBarcode(Barcode barcode) {
		this.barcode = barcode;
		Object old = getBarcode();
        firePropertyChange("Barcode", old, barcode);
        invalidate();
        repaint();
	}
    
}