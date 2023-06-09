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
package org.bars4j.demo;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.bars4j.Barcode;
import org.bars4j.BarcodeFactory;
import org.bars4j.encode.UPCAEncoder;
import org.bars4j.util.ImageUtil;





/**
 * TODO: Description.
 * 
 * @author Flavio Sampaio
 * @since 0.1
 */
public class SimpleDemo {

    /**
     * @param args
     */
    public static void main(String [] args){
        
        try{
            //Creates a JBarcode with a EAN13Encoder and a WidthCodedPainter
            Barcode b4j = BarcodeFactory.getInstance().createEAN13();
            long startTime = 0, elapsedTime = 0;
            

            System.out.println("EAN13:");
            String code = "789100031550";
            startTime = System.currentTimeMillis();
            BufferedImage img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            StringBuffer svg = b4j.createBarcodeSVG(code);
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "EAN13.svg");
            saveToPNG(img, "EAN13.png");
            
            //EAN8 Code Example
            System.out.println("EAN8:");
            b4j = BarcodeFactory.getInstance().createEAN8();
            code = "9788515";
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "EAN8.svg");
            saveToPNG(img, "EAN8.png");
            
            //EAN5 Code Example
            b4j = BarcodeFactory.getInstance().createEAN5();
            code = "54495";
            img = b4j.createBarcode(code);
            saveToPNG(img, "EAN5.png");
            
            //UPCA Code Example
            System.out.println("UPCA:");
            b4j = BarcodeFactory.getInstance().createUPCA();
            code = "07567816415";        
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "UPCA.svg");
            saveToPNG(img, "UPCA.png");
            
            //UPCE Code Example
            System.out.println("UPCE:");
            b4j = BarcodeFactory.getInstance().createUPCE();
            code = UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415");
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "UPCE.svg");
            saveToPNG(img, "UPCE.png");
            
            //Codabar Code Example
            System.out.println("Codabar:");
            b4j = BarcodeFactory.getInstance().createCodabar();
            code = "97885150040-85";
            startTime = System.currentTimeMillis();
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            img = b4j.createBarcode(code);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "Codabar.svg");
            saveToJPEG(img, "Codabar.jpg");
            
            //Code39 Code Example
            System.out.println("Code 39:");
            b4j = BarcodeFactory.getInstance().createCode39();
            code = "BARS4J-39";
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "Code39.svg");
            saveToPNG(img, "Code39.png");
            
            //Code39 Extended Code Example
            System.out.println("Code 39 Extended:");
            b4j = BarcodeFactory.getInstance().createCode39Extended();
            code = "Bars4J-39Ext";
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "Code39Ext.svg");
            saveToPNG(img, "Code39Ext.png");
            
            //Code93 Code Example
            System.out.println("Code 93:");
            b4j = BarcodeFactory.getInstance().createCode93();
            code = "BARS4J-93";
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "Code93.svg");
            saveToPNG(img, "Code93.png");
            
            //Code93Ex Code Example
            System.out.println("Code 93 Extended:");
            b4j = BarcodeFactory.getInstance().createCode93Extended();
            code = "Bars4J-93Ext";
            startTime = System.currentTimeMillis();
            img = b4j.createBarcode(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- IMG: " + elapsedTime);
            startTime = System.currentTimeMillis();
            svg = b4j.createBarcodeSVG(code);
            elapsedTime = System.currentTimeMillis() - startTime;
            System.out.println("- SVG: " + elapsedTime);
            saveToSVG(svg, "Code93Ext.svg");
            saveToPNG(img, "Code93Ext.png");
            
            //Code128 Code Example
            b4j = BarcodeFactory.getInstance().createCode128();
            code = "Bars4J-128";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Code128.svg");
            saveToPNG(img, "Code128.png");
            
            //Code11 Code Example
            b4j = BarcodeFactory.getInstance().createCode11();            
            code = "9788515004041";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Code11.svg");
            saveToPNG(img, "Code11.png");
            
            //Code MSIPlessey Code Example
            b4j = BarcodeFactory.getInstance().createMSIPlessey();
            code = "9788515004041";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "MSIPlessey.svg");
            saveToPNG(img, "MSIPlessey.png");
            
            //Standard 2 of 5 Code Example
            b4j = BarcodeFactory.getInstance().createStandard2of5();
            code = "978851500404";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Standard2of5.svg");
            saveToJPEG(img, "Standard2of5.jpg");
            
            //IATA 2 of 5 Code Example
            b4j = BarcodeFactory.getInstance().createIATA2of5();
            code = "978851500404";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "IATA2of5.svg");
            saveToPNG(img, "IATA2of5.png");
            
            //Matrix 2 of 5 Code Example
            b4j = BarcodeFactory.getInstance().createMatrix2of5();
            code = "123456";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Matrix2of5.svg");
            saveToPNG(img, "Matrix2of5.png");
            
            //Datalogic 2 of 5 Code Example
            b4j = BarcodeFactory.getInstance().createDatalogic2of5();
            code = "123456";
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Datalogic2of5.svg");
            saveToPNG(img, "Datalogic2of5.png");
            
            //Interleaved 2 of 5 Code Example
            b4j = BarcodeFactory.getInstance().createInterleaved2of5();
            code = "9876543210921";        
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "Interleaved2of5.svg");
            saveToPNG(img, "Interleaved2of5.png");
            
            //PostNet Code Example
            b4j = BarcodeFactory.getInstance().createPostNet();
            code = "805365961"; 
            img = b4j.createBarcode(code);
            svg = b4j.createBarcodeSVG(code);
            saveToSVG(svg, "PostNet.svg");
            saveToJPEG(img, "PostNet.jpg");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    static void saveToJPEG(BufferedImage img, String fileName){
        saveToFile(img, fileName, ImageUtil.JPEG);
    }
    
    static void saveToPNG(BufferedImage img, String fileName){
       saveToFile(img, fileName, ImageUtil.PNG);
    }
    
    static void saveToGIF(BufferedImage img, String fileName){
        saveToFile(img, fileName, ImageUtil.GIF);
    }     
    
    static void saveToFile(BufferedImage img, String fileName, String format){
    	try{
            FileOutputStream fos = new FileOutputStream("./images/"+fileName);
            ImageUtil.encodeAndWrite(img, format, fos, 96, 96);
            fos.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    static void saveToSVG(StringBuffer data, String fileName) {
    	
		try {
			PrintWriter writer = new PrintWriter("./images/"+fileName, "UTF-8");

	    	writer.append(data);
	    	writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

}
