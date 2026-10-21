package org.bars4j.encode.matrix.pdf417;

/**
 * One PDF417 row while it is being assembled: a left-to-right sequence of modules that grows as
 * patterns are appended.
 */
final class Pdf417BarcodeRow {

    private final boolean[] modules;
    private int length;

    /**
     * Creates an empty row.
     *
     * @param width maximum number of modules the row can hold
     */
    Pdf417BarcodeRow(int width) {
        this.modules = new boolean[width];
    }

    /** Appends a run of equal modules, either a bar or a space. */
    void addRun(boolean dark, int width) {
        for (int i = 0; i < width; i++) {
            modules[length++] = dark;
        }
    }

    /** Returns the row as a dense dark/light array. */
    boolean[] toModules() {
        return modules.clone();
    }
}
