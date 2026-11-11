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
package org.bars4j.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/**
 * Utility class to encode images, backed by the standard ImageIO API.
 *
 * @author Flavio Sampaio
 * @since 0.1
 */
public class ImageUtil {

    public static final String JPEG = "jpeg";
    public static final String PNG = "png";
    public static final String GIF = "gif";

    public static final int DEFAULT_DPI = 96;

    /**
     * Creates an image encoded in the specified format.
     *
     * @param image  image to be encoded.
     * @param format image format (PNG, JPEG or GIF).
     * @return byte array with the encoded image.
     * @throws IOException if the format is not supported.
     */
    public static byte[] encode(BufferedImage image, String format) throws IOException {
        return encode(image, format, DEFAULT_DPI, DEFAULT_DPI);
    }

    /**
     * Creates an image encoded in the specified format and dpi.
     *
     * @param image image to be encoded.
     * @param format image format (PNG, JPEG or GIF).
     * @param xDpi horizontal dpi definition.
     * @param yDpi vertical dpi definition.
     * @return byte array with the encoded image.
     * @throws IOException if the format is not supported.
     */
    public static byte[] encode(BufferedImage image, String format, int xDpi, int yDpi) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encodeAndWrite(image, format, out, xDpi, yDpi);
        return out.toByteArray();
    }

    public static void encodeAndWrite(BufferedImage image, String format, OutputStream out) throws IOException {
        encodeAndWrite(image, format, out, DEFAULT_DPI, DEFAULT_DPI);
    }

    public static void encodeAndWrite(BufferedImage image, String format, OutputStream out, int xDpi, int yDpi)
            throws IOException {
        String normalized = format == null ? "" : format.toLowerCase(Locale.ROOT);
        if (!PNG.equals(normalized) && !JPEG.equals(normalized) && !GIF.equals(normalized)) {
            throw new IOException("Unsupported image type: " + format);
        }
        if (xDpi <= 0 || yDpi <= 0) {
            throw new IOException("DPI values must be positive: " + xDpi + "x" + yDpi);
        }
        if (JPEG.equals(normalized)) {
            // JPEG has no alpha channel: composite over white first, otherwise transparent
            // pixels are rendered black by the writer.
            image = overWhite(image);
        }
        ImageWriter writer = imageWriter(normalized);
        try {
            ImageWriteParam params = writer.getDefaultWriteParam();
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image), params);
            if (metadata != null) {
                if (PNG.equals(normalized)) {
                    setPngDensity(metadata, xDpi, yDpi);
                } else if (JPEG.equals(normalized)) {
                    setJpegDensity(metadata, xDpi, yDpi);
                }
                // GIF: no standard DPI metadata chunk; the density hint is silently ignored.
            }
            try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(image, null, metadata), params);
            }
        } finally {
            writer.dispose();
        }
    }

    /**
     * Stores the pixel density in the PNG {@code pHYs} chunk. The JDK PNG writer interprets
     * {@code HorizontalPixelSize} as dots per millimetre (contrary to the standard-tree
     * documentation, which says millimetres per pixel), so the value must be {@code dpi/25.4}
     * to emit a correct {@code pHYs} chunk.
     */
    private static void setPngDensity(IIOMetadata metadata, int xDpi, int yDpi) throws IOException {
        IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
        IIOMetadataNode dimension = new IIOMetadataNode("Dimension");
        IIOMetadataNode horizontal = new IIOMetadataNode("HorizontalPixelSize");
        horizontal.setAttribute("value", Double.toString(xDpi / 25.4));
        IIOMetadataNode vertical = new IIOMetadataNode("VerticalPixelSize");
        vertical.setAttribute("value", Double.toString(yDpi / 25.4));
        dimension.appendChild(horizontal);
        dimension.appendChild(vertical);
        root.appendChild(dimension);
        metadata.mergeTree("javax_imageio_1.0", root);
    }

    /** Stores the pixel density in the JPEG JFIF {@code Xdensity}/{@code Ydensity} fields. */
    private static void setJpegDensity(IIOMetadata metadata, int xDpi, int yDpi) throws IOException {
        IIOMetadataNode jfif = (IIOMetadataNode) metadata.getAsTree("javax_imageio_jpeg_image_1.0");
        IIOMetadataNode markerSequence = (IIOMetadataNode) jfif.getElementsByTagName("JPEGvariety").item(0);
        IIOMetadataNode app0Jfif = (IIOMetadataNode) markerSequence.getElementsByTagName("app0JFIF").item(0);
        app0Jfif.setAttribute("Xdensity", Integer.toString(xDpi));
        app0Jfif.setAttribute("Ydensity", Integer.toString(yDpi));
        // The JFIF attribute is "resUnits" (1 = dots per inch); "densityUnits" is discarded.
        app0Jfif.setAttribute("resUnits", "1");
        metadata.setFromTree("javax_imageio_jpeg_image_1.0", jfif);
    }

    /** Flattens the image over a white background, dropping its alpha channel. */
    private static BufferedImage overWhite(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, result.getWidth(), result.getHeight());
        g2d.drawRenderedImage(image, null);
        g2d.dispose();
        return result;
    }

    private static ImageWriter imageWriter(String format) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IOException("No image writer available for format: " + format);
        }
        return writers.next();
    }
}
