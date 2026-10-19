package org.bars4j.format;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.render.RenderSpec;
import org.bars4j.render.linear.LinearRenderer;
import org.bars4j.render.linear.LinearTextRenderer;
import org.bars4j.symbol.LinearSymbol;

/**
 * A linear (1D) format backed by the new native encoder/renderer pipeline.
 *
 * <p>This reproduces the legacy {@code org.bars4j.Barcode} assembly (check-digit handling, quiet
 * zone, x-dimension scaling, margins and human-readable text) byte-for-byte, while depending only on
 * the new {@code encode.linear} / {@code render.linear} packages.
 *
 * <p>The provided {@link RenderSpec} is ignored for linear formats: like the legacy facade they use
 * their own per-format defaults for x-dimension, bar height and wide ratio.
 */
public final class LinearBarcodeFormat implements BarcodeFormat {

    /** Smallest meaningful x-dimension (mirrors the legacy {@code Barcode.MIN_XDIMENSION}). */
    public static final double MIN_XDIMENSION = 0.264583333;

    private static final int FONT_SIZE = 14;

    private final LinearEncoder encoder;
    private final LinearRenderer renderer;
    private final LinearTextRenderer textRenderer;
    private final FormatSpec spec;

    private final double module;
    private final double wideToNarrow;
    private final int barHeight;
    private final int xdimension;
    private final boolean checkDigit;
    private final boolean showCheckDigit;
    private final boolean showText;

    public LinearBarcodeFormat(LinearEncoder encoder, LinearRenderer renderer, LinearTextRenderer textRenderer,
                               FormatSpec spec, double xDimension, double barHeightMm, double wideRatio,
                               boolean checkDigit, boolean showCheckDigit, boolean showText) {
        this.encoder = encoder;
        this.renderer = renderer;
        this.textRenderer = textRenderer;
        this.spec = spec;
        this.module = xDimension;
        this.xdimension = Math.max((int) (xDimension / MIN_XDIMENSION), 1);
        this.barHeight = (int) Math.round(barHeightMm / MIN_XDIMENSION);
        this.wideToNarrow = wideRatio;
        this.checkDigit = checkDigit;
        this.showCheckDigit = showCheckDigit;
        this.showText = showText;
    }

    @Override
    public String id() {
        return spec.id();
    }

    @Override
    public FormatSpec spec() {
        return spec;
    }

    /** Effective code (with check digit when applicable) and the text label to draw. */
    private String[] resolve(String text) throws BarcodeException {
        String code = text;
        String label = text;
        if (checkDigit) {
            code = text + encoder.computeCheckSum(text);
            if (showCheckDigit) {
                label = code;
            }
        }
        return new String[]{code, label};
    }

    private RenderSpec internalSpec() {
        return new RenderSpec(1, barHeight, wideToNarrow, 1, Color.BLACK, Color.WHITE);
    }

    @Override
    public BufferedImage toImage(String text, RenderSpec renderSpec) throws BarcodeException {
        String[] resolved = resolve(text);
        LinearSymbol symbol = encoder.encode(resolved[0]);
        BufferedImage img = renderer.toImage(symbol, internalSpec());

        BufferedImage result = new BufferedImage(
                (int) (img.getWidth() * (module / MIN_XDIMENSION)) + 1,
                barHeight + 8, BufferedImage.TYPE_INT_RGB);
        AffineTransform at = new AffineTransform(
                (module / MIN_XDIMENSION), 0.0, 0.0, (double) barHeight / img.getHeight(), 0.0, 4.0);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(Color.WHITE);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, result.getWidth(), result.getHeight());
        g2d.drawRenderedImage(img, at);

        if (showText) {
            textRenderer.paintText(result, resolved[1], 1);
        }
        return result;
    }

    @Override
    public String toSvg(String text, RenderSpec renderSpec) throws BarcodeException {
        String[] resolved = resolve(text);
        LinearSymbol symbol = encoder.encode(resolved[0]);

        int totalWidth = renderer.calcTotalWidth(symbol, xdimension, barHeight, wideToNarrow);
        int totalHeight = barHeight + 4;

        StringBuilder pcode = new StringBuilder(renderer.toSvg(symbol, internalSpec()));
        if (showText) {
            pcode.append(textRenderer.paintTextSvg(resolved[1], barHeight, totalWidth));
            totalHeight = textRenderer.calcTotalHeight(barHeight, FONT_SIZE);
        }

        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
                + "<!-- Created with Bars4J -->\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"");
        res.append(totalWidth);
        res.append("\" height=\"");
        res.append(totalHeight);
        res.append("\" preserveAspectRatio=\"x");
        res.append(totalWidth);
        res.append("Y");
        res.append(totalHeight);
        res.append(" meet\"><style>text{font-family: 'Courier New', monospace;font-size: 14px;}</style>");
        res.append(pcode);
        res.append("Sorry, your browser does not support inline SVG.</svg>");
        return res.toString();
    }

    @Override
    public String checkSum(String text) throws BarcodeException {
        return encoder.computeCheckSum(text);
    }
}
