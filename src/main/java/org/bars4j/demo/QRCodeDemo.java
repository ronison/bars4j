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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.render.RenderSpec;
import org.bars4j.util.ImageUtil;

/**
 * Demonstrates the new unified API, including the self-contained QRCode implementation.
 *
 * <p>Run with: {@code mvn exec:java -Dexec.mainClass=org.bars4j.demo.QRCodeDemo}
 */
public class QRCodeDemo {

    public static void main(String[] args) {
        new File("./images").mkdirs();

        try {
            System.out.println("Registered formats: " + BarcodeRegistry.getInstance().ids());

            // A QRCode rendered with the default settings (ecc level M, module size 4).
            String text = "https://github.com/ronison/bars4j";
            BufferedImage image = BarcodeGenerator.of("QRCODE").toImage(text);
            saveToPNG(image, "QRCode.png");
            saveToSVG(BarcodeGenerator.of("QRCODE").toSvg(text), "QRCode.svg");
            System.out.println("QRCODE: " + image.getWidth() + "x" + image.getHeight() + "px -> images/QRCode.png");

            // A larger QRCode: bigger modules and a margin-friendly rendering.
            RenderSpec big = new RenderSpec(1, 70, 2.0, 8, Color.BLACK, Color.WHITE);
            saveToPNG(BarcodeGenerator.of("QRCODE").with(big).toImage("Bars4J QRCode demo"), "QRCode-big.png");

            // The same unified entry point works for the 1D formats too.
            String eanSvg = BarcodeGenerator.of("EAN13").toSvg("789100031550");
            saveToSVG(eanSvg, "EAN13-from-new-api.svg");
            System.out.println("EAN13 via unified API -> images/EAN13-from-new-api.svg");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveToPNG(BufferedImage img, String fileName) {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream("./images/" + fileName)) {
            ImageUtil.encodeAndWrite(img, ImageUtil.PNG, fos, 96, 96);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void saveToSVG(String data, String fileName) {
        try (PrintWriter writer = new PrintWriter("./images/" + fileName, "UTF-8")) {
            writer.append(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
