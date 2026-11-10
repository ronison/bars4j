/**
 * Rendering options.
 *
 * Port of {@code org.bars4j.render.RenderSpec}.
 */
export interface RenderSpec {
  /** Width of one narrow module for linear symbols, in pixels. */
  readonly barWidth: number;
  /** Bar height for linear symbols, in pixels. */
  readonly barHeight: number;
  /** Wide-to-narrow ratio for wide-ratio coded linear symbols. */
  readonly wideRatio: number;
  /** Size of one module for matrix symbols, in pixels. */
  readonly moduleSize: number;
  /** Bar/module colour. */
  readonly foreground: string;
  /** Background colour. */
  readonly background: string;
}

export function renderSpec(
  barWidth: number,
  barHeight: number,
  wideRatio: number,
  moduleSize: number,
  foreground: string,
  background: string,
): RenderSpec {
  if (barWidth <= 0 || barHeight <= 0 || moduleSize <= 0) {
    throw new Error("barWidth/barHeight/moduleSize must be positive");
  }
  if (wideRatio < 1.0) {
    throw new Error("wideRatio must be >= 1.0");
  }
  return { barWidth, barHeight, wideRatio, moduleSize, foreground, background };
}

export function linearDefaults(): RenderSpec {
  return renderSpec(1, 70, 2.0, 4, "#000000", "#ffffff");
}

export function matrixDefaults(): RenderSpec {
  return renderSpec(1, 70, 2.0, 4, "#000000", "#ffffff");
}
