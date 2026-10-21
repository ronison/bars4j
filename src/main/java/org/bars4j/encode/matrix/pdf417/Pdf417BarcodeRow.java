package org.bars4j.encode.matrix.pdf417;

/**
 * A single PDF417 row under construction: a run of modules that grows from left to right as
 * codeword patterns are appended.
 */
final class Pdf417BarcodeRow {

    private final boolean[] modules;
    private int position;

    /**
     * Creates an empty row able to hold the given number of modules.
     *
     * @param width maximum number of modules in the row
     */
    Pdf417BarcodeRow(int width) {
        this.modules = new boolean[width];
    }

    /** Appends a run of equal modules (a bar or a space). */
    void addRun(boolean dark, int width) {
        for (int i = 0; i < width; i++) {
            modules[position++] = dark;
        }
    }

    /** Returns the row as a dense dark/light array. */
    boolean[] toModules() {
        return modules.clone();
    }
}
