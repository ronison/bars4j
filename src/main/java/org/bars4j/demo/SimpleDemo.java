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
import java.io.FileOutputStream;

import org.bars4j.Barcode;
import org.bars4j.encode.CodabarEncoder;
import org.bars4j.encode.Code128Encoder;
import org.bars4j.encode.Code39Encoder;
import org.bars4j.encode.Code93Encoder;
import org.bars4j.encode.Code93ExtEncoder;
import org.bars4j.encode.Datalogic2of5Encoder;
import org.bars4j.encode.EAN13Encoder;
import org.bars4j.encode.EAN5Encoder;
import org.bars4j.encode.EAN8Encoder;
import org.bars4j.encode.IATA2of5Encoder;
import org.bars4j.encode.Interleaved2of5Encoder;
import org.bars4j.encode.Matrix2of5Encoder;
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
            Barcode jbcode = new Barcode(EAN13Encoder.getInstance(), WidthCodedPainter.getInstance(), EAN13TextPainter.getInstance());
            
            String code = "788515004012";
            BufferedImage img = jbcode.createBarcode(code);
            saveToGIF(img, "EAN13.gif");
            
            //EAN8 Code Example
            jbcode.setEncoder(EAN8Encoder.getInstance());
            jbcode.setTextPainter(EAN8TextPainter.getInstance());
            code = "9788515";        
            img = jbcode.createBarcode(code);
            saveToPNG(img, "EAN8.png");
            
            //EAN5 Code Example
            jbcode.setEncoder(EAN5Encoder.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setCheckDigit(false);
            code = "54495";
            jbcode.toString();
            img = jbcode.createBarcode(code);
            saveToPNG(img, "EAN5.png");
            
            //UPCA Code Example
            jbcode.setEncoder(UPCAEncoder.getInstance());
            jbcode.setTextPainter(UPCATextPainter.getInstance());
            jbcode.setCheckDigit(true);
            code = "07567816415";        
            img = jbcode.createBarcode(code);
            saveToPNG(img, "UPCA.png");
            
            //UPCE Code Example
            jbcode.setEncoder(UPCEEncoder.getInstance());
            jbcode.setTextPainter(UPCETextPainter.getInstance());
            code = UPCAEncoder.getInstance().convertUPCAtoUPCE("07567816415");        
            img = jbcode.createBarcode(code);
            saveToPNG(img, "UPCE.png");
            
            //Codabar Code Example
            jbcode.setEncoder(CodabarEncoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            code = "97885150040-85";
            jbcode.setWideRatio(3.0);
            img = jbcode.createBarcode(code);
            saveToJPEG(img, "Codabar.jpg");
            
            //Code39 Code Example
            jbcode.setEncoder(Code39Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(false);
            code = "JBARCODE-39";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Code39.png");
            
            //Code93 Code Example
            jbcode.setEncoder(Code93Encoder.getInstance());
            jbcode.setPainter(WidthCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(false);
            code = "JBARCODE-93";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Code93.png");
            
            //Code93Ex Code Example
            jbcode.setEncoder(Code93ExtEncoder.getInstance());
            jbcode.setPainter(WidthCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(false);
            code = "JBARCODE-93Ext";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Code93Ext.png");
            
            //Code128 Code Example
            jbcode.setEncoder(Code128Encoder.getInstance());
            jbcode.setPainter(WidthCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(false);
            code = "JBarcode-128";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Code128.png");
            
            //Standard 2 of 5 Code Example
            jbcode.setEncoder(Standard2of5Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(true);
            jbcode.setXDimension(0.5291666);
            code = "978851500404";
            img = jbcode.createBarcode(code);
            saveToJPEG(img, "Standard2of5.jpg");
            
            //IATA 2 of 5 Code Example
            jbcode.setEncoder(IATA2of5Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(true);
            jbcode.setXDimension(0.5291666);
            code = "978851500404";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "IATA2of5.png");
            
            //Matrix 2 of 5 Code Example
            jbcode.setEncoder(Matrix2of5Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(true);
            jbcode.setXDimension(0.5291666);
            code = "123456";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Matrix2of5.png");
            
            //Datalogic 2 of 5 Code Example
            jbcode.setEncoder(Datalogic2of5Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(true);
            jbcode.setXDimension(0.5291666);
            code = "123456";
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Datalogic2of5.png");
            
            //Interleaved 2 of 5 Code Example
            jbcode.setEncoder(Interleaved2of5Encoder.getInstance());
            jbcode.setPainter(WideRatioCodedPainter.getInstance());
            jbcode.setTextPainter(BaseLineTextPainter.getInstance());
            jbcode.setShowCheckDigit(true);
            jbcode.setXDimension(0.5291666);
            code = "9876543210921";        
            img = jbcode.createBarcode(code);
            saveToPNG(img, "Interleaved2of5.png");
            
            //PostNet Code Example
            jbcode.setEncoder(PostNetEncoder.getInstance());
            jbcode.setPainter(HeightCodedPainter.getInstance());
            jbcode.setBarHeight(6);
            jbcode.setXDimension(0.5291666);
            jbcode.setShowText(false);
            code = "805365961"; 
            img = jbcode.createBarcode(code);
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

}
