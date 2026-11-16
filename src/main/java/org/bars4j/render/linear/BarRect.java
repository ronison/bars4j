package org.bars4j.render.linear;

/**
 * One drawn rectangle of a linear symbol: position, size and ink.
 *
 * <p>The painting strategies convert a {@link org.bars4j.symbol.LinearSymbol} into a list of
 * rectangles first (this record) and hand the list to the shared raster/SVG painters, so the
 * geometry logic lives in one place per strategy instead of being repeated per output format.
 *
 * @param x      left edge, in pixels, relative to the drawing origin
 * @param y      top edge, in pixels, relative to the drawing origin
 * @param width  width in pixels
 * @param height height in pixels
 * @param dark   whether the rectangle is drawn in the foreground (bar) or background colour
 */
record BarRect(int x, int y, int width, int height, boolean dark) {
}
