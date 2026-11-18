package org.bars4j.demo;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.render.RenderSpec;

/**
 * Demonstrates how the human-readable label font size of an EAN-13 barcode is controlled by the
 * {@code fontSize} field of the {@link RenderSpec}.
 *
 * <p>Generates the sample EAN-13 with font sizes 10, 14 (default), 20 and 28, writing an SVG and a
 * PNG for each to {@code ./images} ({@code EAN13-font-<N>.svg/.png}). The SVG style carries the
 * font size, the raster label is drawn with it, and the canvas height grows with the font size
 * ({@code barHeight * 0.9 + fontSize}).
 *
 * <p>Run with: {@code mvn exec:java -Dexec.mainClass=org.bars4j.demo.Ean13FontSizeDemo}.
 */
public final class Ean13FontSizeDemo {

    private static final Logger LOG = Logger.getLogger(Ean13FontSizeDemo.class.getName());

    private static final String EAN13 = "789100031550";
    private static final int[] FONT_SIZES = {10, 14, 20, 28};

    private Ean13FontSizeDemo() {
    }

    public static void main(String[] args) {
        BarcodeGenerator generator = BarcodeGenerator.of("EAN13");
        System.out.printf("%-10s %-14s %-14s%n", "font (px)", "SVG canvas", "PNG canvas");
        for (int fontSize : FONT_SIZES) {
            // Linear formats take from the spec only foreground/background/quietZone/fontSize;
            // fontSize is the label font size, and the SVG canvas height grows with it.
            RenderSpec spec = new RenderSpec(1, 70, 2.0, 4, Color.BLACK, Color.WHITE, 10, fontSize);
            try {
                String svg = generator.with(spec).toSvg(EAN13);
                BufferedImage img = generator.with(spec).toImage(EAN13);
                saveSvg(svg, "EAN13-font-" + fontSize + ".svg");
                savePng(img, "EAN13-font-" + fontSize + ".png");
                System.out.printf("%-10d %-14s %-14s%n", fontSize,
                        svgWidth(svg) + "x" + svgHeight(svg), img.getWidth() + "x" + img.getHeight());
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to render font size " + fontSize, e);
            }
        }
        System.out.println("\nFiles written to ./images (EAN13-font-<N>.svg/.png).");
        System.out.println("Note: toHtml styles its label with the spec's font size too, but the");
        System.out.println("display size scales the snippet proportionally, which visually dilutes");
        System.out.println("the difference; use toSvg to compare the raw label sizes.");
    }

    private static String svgWidth(String svg) {
        return attribute(svg, "width");
    }

    private static String svgHeight(String svg) {
        return attribute(svg, "height");
    }

    private static String attribute(String svg, String name) {
        int from = svg.indexOf(name + "=\"") + name.length() + 2;
        return svg.substring(from, svg.indexOf('"', from));
    }

    private static void saveSvg(String data, String fileName) {
        try (PrintWriter writer = new PrintWriter("./images/" + fileName, StandardCharsets.UTF_8)) {
            writer.append(data);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save SVG: " + fileName, e);
        }
    }

    private static void savePng(BufferedImage img, String fileName) {
        try (FileOutputStream fos = new FileOutputStream("./images/" + fileName)) {
            ImageIO.write(img, "png", fos);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save PNG: " + fileName, e);
        }
    }
}
