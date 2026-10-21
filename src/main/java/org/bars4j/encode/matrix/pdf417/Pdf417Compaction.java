package org.bars4j.encode.matrix.pdf417;

/**
 * High-level compaction schemes defined by ISO/IEC 15438.
 *
 * <p>{@link #AUTO} chooses a scheme (or a mix of schemes) for every stretch of input; the other
 * values force a single scheme for the whole message.
 */
public enum Pdf417Compaction {

    /** Let the encoder choose the most suitable compaction for each part of the input. */
    AUTO,

    /** Force Text compaction for the whole message. */
    TEXT,

    /** Force Byte compaction for the whole message. */
    BYTE,

    /** Force Numeric compaction for the whole message (digits only). */
    NUMERIC
}
