package org.bars4j.format;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.encode.api.InvalidSymbolException;
import org.bars4j.encode.api.LinearEncoder;
import org.bars4j.render.RenderSpec;
import org.bars4j.render.linear.LinearRenderer;
import org.bars4j.render.linear.LinearTextRenderer;
import org.bars4j.symbol.LinearSymbol;

/**
 * A linear (1D) format backed by the native encoder/renderer pipeline.
 *
 * <p>The provided {@link RenderSpec} supplies the bar/module and background colours, the quiet
 * zone width and the human-readable label font size for linear formats ({@code null} components
 * fall back to black on white, a quiet zone of 10 modules and a 14px label). Like the historical
 * facade they use their own per-format defaults for x-dimension, bar height and wide ratio, which
 * the spec does not override.
 */
public final class LinearBarcodeFormat implements BarcodeFormat {

    /** Smallest meaningful x-dimension. */
    public static final double MIN_XDIMENSION = 0.264583333;

    /**
     * Default upper bound for the user-supplied text length. Variable-length symbologies (Code 128,
     * Code 39, MSI, 2-of-5, ...) accept arbitrary input, so without a cap an untrusted request can
     * drive a multi-megabyte string into the encoder and exhaust memory/CPU (DoS).
     */
    public static final int DEFAULT_MAX_TEXT_LENGTH = 4_000;

    /** Upper bound applied by {@link #resolve} when {@link #withMaxTextLength} is not used. */
    private static int defaultMaxTextLength = DEFAULT_MAX_TEXT_LENGTH;


    /**
     * User-configurable options of a linear format.
     *
     * @param xDimensionMm  narrow bar width (x-dimension) in millimetres
     * @param barHeightMm   bar height in millimetres
     * @param wideRatio     wide-to-narrow ratio (only used by wide-ratio coded formats)
     * @param checkDigit    whether to append the computed check digit before encoding
     * @param showCheckDigit whether to include the check digit in the human readable label
     * @param showText      whether to draw the human readable label
     */
    public record LinearOptions(double xDimensionMm, double barHeightMm, double wideRatio,
                                boolean checkDigit, boolean showCheckDigit, boolean showText) {
    }

    private final LinearEncoder encoder;
    private final LinearRenderer renderer;
    private final LinearTextRenderer textRenderer;
    private final FormatSpec spec;

    private final double module;
    private final double barHeightMm;
    private final double wideToNarrow;
    private final int barHeight;
    private final int xdimension;
    private final boolean checkDigit;
    private final boolean showCheckDigit;
    private final boolean showText;

    /** Extra vertical space (px) reserved below the bars so the human-readable text does not overlap. */
    private final int textGap;

    public LinearBarcodeFormat(LinearEncoder encoder, LinearRenderer renderer, LinearTextRenderer textRenderer,
                               FormatSpec spec, double xDimension, double barHeightMm, double wideRatio,
                               boolean checkDigit, boolean showCheckDigit, boolean showText) {
        this(encoder, renderer, textRenderer, spec, xDimension, barHeightMm, wideRatio,
                checkDigit, showCheckDigit, showText, 0);
    }

    public LinearBarcodeFormat(LinearEncoder encoder, LinearRenderer renderer, LinearTextRenderer textRenderer,
                               FormatSpec spec, double xDimension, double barHeightMm, double wideRatio,
                               boolean checkDigit, boolean showCheckDigit, boolean showText, int textGap) {
        this.encoder = encoder;
        this.renderer = renderer;
        this.textRenderer = textRenderer;
        this.spec = spec;
        this.module = xDimension;
        this.xdimension = Math.max((int) (xDimension / MIN_XDIMENSION), 1);
        this.barHeightMm = barHeightMm;
        this.barHeight = (int) Math.round(barHeightMm / MIN_XDIMENSION);
        this.wideToNarrow = wideRatio;
        this.checkDigit = checkDigit;
        this.showCheckDigit = showCheckDigit;
        this.showText = showText;
        this.textGap = textGap;
    }

    /** Returns the current configuration of this format. */
    public LinearOptions options() {
        return new LinearOptions(module, barHeightMm, wideToNarrow, checkDigit, showCheckDigit, showText);
    }

    /** Returns a copy of this format with the given options applied. */
    public LinearBarcodeFormat withOptions(LinearOptions options) {
        return new LinearBarcodeFormat(encoder, renderer, textRenderer, spec,
                options.xDimensionMm(), options.barHeightMm(), options.wideRatio(),
                options.checkDigit(), options.showCheckDigit(), options.showText(), textGap);
    }

    /** The effective text-length cap for this format. */
    public static int maxTextLength() {
        return defaultMaxTextLength;
    }

    /**
     * Overrides the global text-length cap (e.g. lowered for an internet-facing service, or raised
     * for a trusted batch job). Values below 1 are ignored.
     *
     * @param maxTextLength new maximum user-supplied text length
     */
    public static void setMaxTextLength(int maxTextLength) {
        if (maxTextLength >= 1) {
            defaultMaxTextLength = maxTextLength;
        }
    }

    /** Restores the default text-length cap. */
    public static void resetMaxTextLength() {
        defaultMaxTextLength = DEFAULT_MAX_TEXT_LENGTH;
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
        if (text == null || text.isEmpty()) {
            throw new InvalidSymbolException("[" + spec.id() + "] Invalid text length (0).");
        }
        if (text.length() > defaultMaxTextLength) {
            throw new InvalidSymbolException("[" + spec.id() + "] Text length (" + text.length()
                    + ") exceeds the maximum of " + defaultMaxTextLength + ".");
        }
        String code = text;
        String label = text;
        if (checkDigit) {
            if (text.length() == encoder.completeLength()) {
                // The caller already supplied the complete code (payload + check digit): keep it
                // verbatim. The encoder validates the check digit and rejects a wrong one.
                code = text;
            } else {
                code = text + encoder.computeCheckSum(text);
            }
            if (showCheckDigit) {
                label = code;
            }
        }
        return new String[]{code, label};
    }

    private RenderSpec internalSpec() {
        return internalSpec(null);
    }

    /**
     * Builds the internal render spec from this format's geometry, taking the colours, quiet zone
     * and label font size from the user-provided spec when given ({@code null} components fall
     * back to black on white, a quiet zone of 10 and a 14px label).
     */
    private RenderSpec internalSpec(RenderSpec userSpec) {
        Color foreground = userSpec != null && userSpec.foreground() != null
                ? userSpec.foreground() : Color.BLACK;
        Color background = userSpec != null && userSpec.background() != null
                ? userSpec.background() : Color.WHITE;
        int quietZone = userSpec != null ? userSpec.quietZone() : RenderSpec.DEFAULT_QUIET_ZONE;
        int fontSize = userSpec != null ? userSpec.fontSize() : RenderSpec.DEFAULT_FONT_SIZE;
        return new RenderSpec(1, barHeight, wideToNarrow, 1, foreground, background, quietZone, fontSize);
    }

    @Override
    public BufferedImage toImage(String text, RenderSpec renderSpec) throws BarcodeException {
        String[] resolved = resolve(text);
        LinearSymbol symbol = encoder.encode(resolved[0]);
        RenderSpec internal = internalSpec(renderSpec);
        double scale = module / MIN_XDIMENSION;

        int unscaledWidth = renderer.calcTotalWidth(symbol, internal.barWidth(), internal.barHeight(),
                internal.wideRatio(), internal.quietZone());
        BufferedImage result = new BufferedImage(
                (int) (unscaledWidth * scale) + 1, barHeight + 8 + (showText ? textGap : 0),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setBackground(internal.background());
        g2d.setColor(internal.background());
        g2d.fillRect(0, 0, result.getWidth(), result.getHeight());
        g2d.dispose();

        if (Math.abs(scale - 1.0) < 1e-9) {
            // No horizontal scaling required: draw straight into the destination, avoiding an
            // intermediate full-size image and the drawRenderedImage transform pipeline.
            renderer.renderInto(result, 0, 4, symbol, internal);
        } else {
            BufferedImage img = renderer.toImage(symbol, internal);
            g2d = result.createGraphics();
            AffineTransform at = new AffineTransform(
                    scale, 0.0, 0.0, (double) barHeight / img.getHeight(), 0.0, 4.0);
            g2d.drawRenderedImage(img, at);
            g2d.dispose();
        }

        if (showText) {
            textRenderer.paintText(result, resolved[1], 1, internal);
        }
        return result;
    }

    @Override
    public String toSvg(String text, RenderSpec renderSpec) throws BarcodeException {
        String[] resolved = resolve(text);
        LinearSymbol symbol = encoder.encode(resolved[0]);

        // The SVG paths are drawn in barWidth=1 units; compute the canvas width with the same
        // unit so the document is not over-sized for x-dimensions above the minimum.
        int totalWidth = renderer.calcTotalWidth(symbol, 1, barHeight, wideToNarrow,
                internalSpec(renderSpec).quietZone());
        int totalHeight = barHeight + 4;

        StringBuilder pcode = new StringBuilder(renderer.toSvg(symbol, internalSpec(renderSpec)));
        if (showText) {
            int textTop = barHeight + textGap;
            pcode.append(textRenderer.paintTextSvg(resolved[1], textTop, totalWidth, internalSpec(renderSpec)));
            totalHeight = textRenderer.calcTotalHeight(textTop, internalSpec(renderSpec).fontSize());
        }

        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n"
                + "<!-- Created with Bars4J -->\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" width=\"");
        res.append(totalWidth);
        res.append("\" height=\"");
        res.append(totalHeight);
        // A previous version emitted the canvas size as the preserveAspectRatio alignment
        // ("x226Y78 meet"), which is not a valid SVG value; xMidYMid is the spec-correct
        // centred alignment.
        res.append("\" preserveAspectRatio=\"xMidYMid meet\"><style>text{font-family: 'Courier New', monospace;font-size: ")
                .append(internalSpec(renderSpec).fontSize()).append("px;}</style>");
        res.append(pcode);
        res.append("Sorry, your browser does not support inline SVG.</svg>");
        return res.toString();
    }

    @Override
    public String toHtml(String text, RenderSpec renderSpec) throws BarcodeException {
        return toHtml(text, renderSpec, -1, -1);
    }

    /**
     * Renders the embeddable HTML snippet with an explicit display size
     * ({@code width}/{@code height} of -1 keeps the natural size).
     */
    public String toHtml(String text, RenderSpec renderSpec, int displayWidth, int displayHeight)
            throws BarcodeException {
        String[] resolved = resolve(text);
        LinearSymbol symbol = encoder.encode(resolved[0]);

        // Same geometry as toSvg: the paths are drawn in barWidth=1 units and the human-readable
        // text sits below the bars.
        int totalWidth = renderer.calcTotalWidth(symbol, 1, barHeight, wideToNarrow,
                internalSpec(renderSpec).quietZone());
        int totalHeight = barHeight + 4;

        StringBuilder pcode = new StringBuilder(renderer.toSvg(symbol, internalSpec(renderSpec)));
        if (showText) {
            int textTop = barHeight + textGap;
            pcode.append(textRenderer.paintTextSvg(resolved[1], textTop, totalWidth, internalSpec(renderSpec)));
            totalHeight = textRenderer.calcTotalHeight(textTop, internalSpec(renderSpec).fontSize());
        }

        return HtmlSnippet.assemble(totalWidth, totalHeight, displayWidth, displayHeight,
                spec.displayName() + " " + resolved[1], pcode.toString());
    }

    @Override
    public String checkSum(String text) throws BarcodeException {
        return encoder.computeCheckSum(text);
    }
}
