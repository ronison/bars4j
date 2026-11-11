/**
 * SVG support for {@link RenderSpec} colours.
 *
 * Port of {@code org.bars4j.render.linear.ColorUtil}.
 */

/** Returns the colour as a {@code #rrggbb} lowercase hexadecimal string. */
export function hexColour(colour: string): string {
  const value = colour.trim();
  if (!value.startsWith("#")) {
    throw new Error(`Unsupported colour: ${colour}`);
  }
  let rgb = value.slice(1);
  if (rgb.length === 3) {
    rgb = rgb[0] + rgb[0] + rgb[1] + rgb[1] + rgb[2] + rgb[2];
  }
  if (rgb.length === 8) {
    // #aarrggbb: drop the alpha component (the library only renders opaque colours).
    rgb = rgb.slice(2);
  }
  if (rgb.length !== 6) {
    throw new Error(`Unsupported colour: ${colour}`);
  }
  return `#${rgb.toLowerCase()}`;
}
