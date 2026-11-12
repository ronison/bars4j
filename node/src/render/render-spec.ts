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

/** Upper bounds for the module/bar dimensions, so an untrusted request cannot request a
 * bitmap large enough to exhaust the heap (resource-exhaustion DoS). */
export const MAX_BAR_WIDTH = 100;
export const MAX_BAR_HEIGHT = 10_000;
export const MAX_MODULE_SIZE = 100;

export function renderSpec(
  barWidth: number,
  barHeight: number,
  wideRatio: number,
  moduleSize: number,
  foreground: string,
  background: string,
): RenderSpec {
  if (!Number.isInteger(barWidth) || barWidth <= 0 || barWidth > MAX_BAR_WIDTH) {
    throw new Error(`barWidth must be an integer between 1 and ${MAX_BAR_WIDTH}`);
  }
  if (!Number.isInteger(barHeight) || barHeight <= 0 || barHeight > MAX_BAR_HEIGHT) {
    throw new Error(`barHeight must be an integer between 1 and ${MAX_BAR_HEIGHT}`);
  }
  if (!Number.isInteger(moduleSize) || moduleSize <= 0 || moduleSize > MAX_MODULE_SIZE) {
    throw new Error(`moduleSize must be an integer between 1 and ${MAX_MODULE_SIZE}`);
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
