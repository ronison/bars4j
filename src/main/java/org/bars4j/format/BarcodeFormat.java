package org.bars4j.format;

import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.render.RenderSpec;

/**
 * A registered barcode format, able to render text to an image or an SVG fragment.
 */
public interface BarcodeFormat {

    String id();

    FormatSpec spec();

    BufferedImage toImage(String text, RenderSpec spec) throws BarcodeException;

    String toSvg(String text, RenderSpec spec) throws BarcodeException;

    /**
     * Returns an SVG snippet ready to be embedded inline in an HTML page (no XML prologue, with a
     * {@code viewBox}, {@code role="img"} and an {@code aria-label}).
     */
    String toHtml(String text, RenderSpec spec) throws BarcodeException;

    String checkSum(String text) throws BarcodeException;
}
