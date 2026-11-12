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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.util.ImageUtil;

/**
 * Generates one sample image per registered format using the unified
 * {@link BarcodeGenerator} API (output is written to {@code ./images}).
 *
 * <p>Run with: {@code mvn exec:java -Dexec.mainClass=org.bars4j.demo.SimpleDemo}.
 */
public class SimpleDemo {

    private static final String[][] CASES = {
        {"EAN13", "789100031550"},
        {"EAN8", "9788515"},
        {"EAN5", "54495"},
        {"UPCA", "07567816415"},
        {"UPCE", "0756785"},
        {"CODABAR", "97885150040-85"},
        {"CODE39", "BARS4J-39"},
        {"CODE39EXT", "Bars4J-39Ext"},
        {"CODE93", "BARS4J-93"},
        {"CODE93EXT", "Bars4J-93Ext"},
        {"CODE128", "Bars4J-128"},
        {"CODE11", "9788515004041"},
        {"MSIPLESSEY", "9788515004041"},
        {"STANDARD2OF5", "978851500404"},
        {"IATA2OF5", "978851500404"},
        {"MATRIX2OF5", "123456"},
        {"DATALOGIC2OF5", "123456"},
        {"INTERLEAVED2OF5", "9876543210921"},
        {"ITF14", "1540014128876"},
        {"POSTNET", "805365961"},
        {"QRCODE", "https://github.com/ronison/bars4j"},
        {"DATAMATRIX", "https://github.com/ronison/bars4j"},
        {"PDF417", "https://github.com/ronison/bars4j"},
        {"AZTEC", "https://github.com/ronison/bars4j"},
    };

    public static void main(String[] args) {
        for (String[] c : CASES) {
            String format = c[0];
            String text = c[1];
            System.out.println(format + ":");
            try {
                BarcodeGenerator generator = BarcodeGenerator.of(format);
                BufferedImage img = generator.toImage(text);
                String svg = generator.toSvg(text);
                saveToSVG(svg, format + ".svg");
                saveToPNG(img, format + ".png");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void saveToPNG(BufferedImage img, String fileName) {
        saveToFile(img, fileName, ImageUtil.PNG);
    }

    static void saveToFile(BufferedImage img, String fileName, String format) {
        try (FileOutputStream fos = new FileOutputStream("./images/" + fileName)) {
            ImageUtil.encodeAndWrite(img, format, fos, 96, 96);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveToSVG(String data, String fileName) {
        try (PrintWriter writer = new PrintWriter("./images/" + fileName, StandardCharsets.UTF_8)) {
            writer.append(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
