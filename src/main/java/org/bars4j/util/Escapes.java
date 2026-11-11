package org.bars4j.util;

/**
 * HTML/XML escaping helpers for content embedded into SVG markup (human-readable text and
 * attributes). Code 128, for instance, accepts {@code &}, {@code <} and {@code >} in its charset,
 * so the raw characters would break the surrounding markup.
 */
public final class Escapes {

    private Escapes() {
    }

    /** Escapes a string for use as element text content ({@code & < >}). */
    public static String text(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Escapes a string for use inside a double-quoted attribute value ({@code & < > "}). */
    public static String attr(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
