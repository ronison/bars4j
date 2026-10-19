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

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import javax.imageio.ImageIO;

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
        if (!ImageIO.write(image, normalized, out)) {
            throw new IOException("No image writer available for format: " + format);
        }
    }
}
