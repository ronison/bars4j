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

import org.bars4j.encode.CodabarEncoder;
import org.bars4j.encode.Code11Encoder;
import org.bars4j.encode.Code128Encoder;
import org.bars4j.encode.Code39Encoder;
import org.bars4j.encode.Code39ExtEncoder;
import org.bars4j.encode.Code93Encoder;
import org.bars4j.encode.Code93ExtEncoder;
import org.bars4j.encode.EAN13Encoder;
import org.bars4j.encode.EAN5Encoder;
import org.bars4j.encode.EAN8Encoder;
import org.bars4j.encode.IATA2of5Encoder;
import org.bars4j.encode.Interleaved2of5Encoder;
import org.bars4j.encode.InvalidAtributeException;
import org.bars4j.encode.MSIPlesseyEncoder;
import org.bars4j.encode.PostNetEncoder;
import org.bars4j.encode.Standard2of5Encoder;
import org.bars4j.encode.UPCAEncoder;
import org.bars4j.encode.UPCEEncoder;
import org.bars4j.paint.BaseLineTextPainter;
import org.bars4j.paint.EAN13TextPainter;
import org.bars4j.paint.EAN8TextPainter;
import org.bars4j.paint.HeightCodedPainter;
import org.bars4j.paint.UPCATextPainter;
import org.bars4j.paint.UPCETextPainter;
import org.bars4j.paint.WideRatioCodedPainter;
import org.bars4j.paint.WidthCodedPainter;


/**
 * JBarcode factory class.
 * 
 * @author Flavio Sampaio
 * @since 0.2
 */
public class BarcodeFactory {
	
	private static BarcodeFactory instance;
	
	private BarcodeFactory(){	
	}
	
	/**
	 * Obtains a JBarcode factory instance.
	 * 
	 * @return JBarcodeFactory instance.
	 */
	public static BarcodeFactory getInstance(){
		if(instance == null){
			instance = new BarcodeFactory();
		}
		return instance;
	}
	
	/**
	 * Creates a new JBarcode instance to EAN-13 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createEAN13(){
		Barcode jbc = new Barcode(EAN13Encoder.getInstance(), WidthCodedPainter.getInstance(), EAN13TextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to EAN-8 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createEAN8(){
		Barcode jbc = new Barcode(EAN8Encoder.getInstance(), WidthCodedPainter.getInstance(), EAN8TextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to EAN-5 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createEAN5(){
		Barcode jbc = new Barcode(EAN5Encoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(false);
        jbc.setShowCheckDigit(false);
		return jbc;
	}

	/**
	 * Creates a new JBarcode instance to UPCA barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createUPCA(){
		Barcode jbc = new Barcode(UPCAEncoder.getInstance(), WidthCodedPainter.getInstance(), UPCATextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to UPCE barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createUPCE(){
		Barcode jbc = new Barcode(UPCEEncoder.getInstance(), WidthCodedPainter.getInstance(), UPCETextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Codabar barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCodabar(){
		Barcode jbc = new Barcode(CodabarEncoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(false);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code11 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode11(){
		Barcode jbc = new Barcode(Code11Encoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code39 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode39(){
		Barcode jbc = new Barcode(Code39Encoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(false);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code39 Extended barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode39Extended(){
		Barcode jbc = new Barcode(Code39ExtEncoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(false);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code93 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode93(){
		Barcode jbc = new Barcode(Code93Encoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code93 Extended barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode93Extended(){
		Barcode jbc = new Barcode(Code93ExtEncoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Code128 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createCode128(){
		Barcode jbc = new Barcode(Code128Encoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to MSIPlessey barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createMSIPlessey(){
		Barcode jbc = new Barcode(MSIPlesseyEncoder.getInstance(), WidthCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(true);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Standard2of5 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createStandard2of5(){
		Barcode jbc = new Barcode(Standard2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to IATA2of5 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createIATA2of5(){
		Barcode jbc = new Barcode(IATA2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to Interleaved2of5 barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createInterleaved2of5(){
		Barcode jbc = new Barcode(Interleaved2of5Encoder.getInstance(), WideRatioCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(17);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(true);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
	/**
	 * Creates a new JBarcode instance to PostNet barcode type. 
	 * 
	 * @return JBarcode instance.
	 */
	public Barcode createPostNet(){
		Barcode jbc = new Barcode(PostNetEncoder.getInstance(), HeightCodedPainter.getInstance(), BaseLineTextPainter.getInstance());
		jbc.setBarHeight(6);
		try {
			jbc.setXDimension(0.264583333);
		} catch (InvalidAtributeException e) {}
		jbc.setShowText(false);
        jbc.setCheckDigit(true);
        jbc.setShowCheckDigit(false);
		return jbc;
	}
	
}
