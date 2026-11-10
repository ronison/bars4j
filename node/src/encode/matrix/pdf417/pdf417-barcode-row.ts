/**
 * One PDF417 row while it is being assembled: a left-to-right sequence of modules that grows as
 * patterns are appended.
 *
 * Port of {@code org.bars4j.encode.matrix.pdf417.Pdf417BarcodeRow}.
 */
export class Pdf417BarcodeRow {
  private readonly modules: boolean[];
  private length = 0;

  /**
   * Creates an empty row.
   *
   * @param width maximum number of modules the row can hold
   */
  constructor(width: number) {
    this.modules = new Array<boolean>(width).fill(false);
  }

  /** Appends a run of equal modules, either a bar or a space. */
  addRun(dark: boolean, width: number): void {
    for (let i = 0; i < width; i++) {
      this.modules[this.length++] = dark;
    }
  }

  /** Returns the row as a dense dark/light array. */
  toModules(): boolean[] {
    return this.modules.slice();
  }
}
