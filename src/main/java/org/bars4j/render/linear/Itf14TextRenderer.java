package org.bars4j.render.linear;

import java.awt.image.BufferedImage;

/**
 * Human-readable text renderer for ITF-14.
 *
 * <p>Formats the 14-digit GS1 Identification Key using the standard ITF-14 HRI mask
 * {@code "0 00 00000 00000 0"} (groups of 1, 2, 5, 5 and 1 digits) before delegating the actual
 * drawing to {@link BaseLineTextRenderer}.
 */
public final class Itf14TextRenderer implements LinearTextRenderer {

    private static final int[] GROUP_SIZES = {1, 2, 5, 5, 1};

    private final BaseLineTextRenderer delegate = new BaseLineTextRenderer();

    /** Applies the {@code "0 00 00000 00000 0"} mask to a 14-digit value. */
    public static String format(String text) {
        if (text == null || text.length() != 14) {
            return text;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return text;
            }
        }
        StringBuilder result = new StringBuilder(18);
        int pos = 0;
        for (int i = 0; i < GROUP_SIZES.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            int end = pos + GROUP_SIZES[i];
            result.append(text, pos, end);
            pos = end;
        }
        return result.toString();
    }

    @Override
    public void paintText(BufferedImage image, String text, int nWidth) {
        delegate.paintText(image, format(text), nWidth);
    }

    @Override
    public String paintTextSvg(String text, int barHeight, int totalWidth) {
        return delegate.paintTextSvg(format(text), barHeight, totalWidth);
    }

    @Override
    public int calcTotalHeight(int barHeight, int fontSize) {
        return delegate.calcTotalHeight(barHeight, fontSize);
    }
}
