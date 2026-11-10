/**
 * A single element of a linear symbol.
 *
 * Port of {@code org.bars4j.symbol.BarElement}.
 */
export interface BarElement {
  /** Whether the element is a bar (dark) or a space (light). */
  readonly dark: boolean;
  /** Whether the element uses the wide width (wide-to-narrow codes). */
  readonly wide: boolean;
  /**
   * Element height as a percentage of the full bar height (100 for normal codes, 50 for the short
   * bars of PostNet).
   */
  readonly heightPercent: number;
}

export function barElement(dark: boolean, wide: boolean, heightPercent: number): BarElement {
  if (heightPercent <= 0) {
    throw new Error("heightPercent must be positive: " + heightPercent);
  }
  return { dark, wide, heightPercent };
}

export function moduleElement(dark: boolean): BarElement {
  return barElement(dark, false, 100);
}
