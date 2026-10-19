package org.bars4j.encode.matrix.qr;

/**
 * QRCode version metadata (versions 1-10).
 *
 * <p>Holds the symbol size, alignment pattern centres and the error-correction block layout per
 * ECC level. Data source: ISO/IEC 18004.
 */
public final class QrVersion {

    /** One block group: {@code count} blocks with {@code dataCodewords} data codewords each. */
    public record EcBlock(int count, int dataCodewords) {
    }

    /** Error-correction layout for one ECC level of one version. */
    public record EcBlocks(int ecCodewordsPerBlock, EcBlock... blocks) {

        public int numBlocks() {
            int total = 0;
            for (EcBlock block : blocks) {
                total += block.count();
            }
            return total;
        }

        public int totalDataCodewords() {
            int total = 0;
            for (EcBlock block : blocks) {
                total += block.count() * block.dataCodewords();
            }
            return total;
        }
    }

    private static final QrVersion[] VERSIONS = build();

    private final int versionNumber;
    private final int size;
    private final int[] alignmentPatternCenters;
    private final EcBlocks[] ecBlocksByLevel;

    private QrVersion(int versionNumber, int[] alignmentPatternCenters, EcBlocks[] ecBlocksByLevel) {
        this.versionNumber = versionNumber;
        this.size = versionNumber * 4 + 17;
        this.alignmentPatternCenters = alignmentPatternCenters;
        this.ecBlocksByLevel = ecBlocksByLevel;
    }

    public int versionNumber() {
        return versionNumber;
    }

    public int size() {
        return size;
    }

    public int[] alignmentPatternCenters() {
        return alignmentPatternCenters.clone();
    }

    public EcBlocks ecBlocks(QrEccLevel level) {
        return ecBlocksByLevel[level.ordinal()];
    }

    /** Number of unused remainder bits at the end of the data area. */
    public int remainderBits() {
        if (versionNumber == 1) {
            return 0;
        }
        if (versionNumber <= 6) {
            return 7;
        }
        return 0; // versions 7-13
    }

    public static QrVersion getVersionForNumber(int versionNumber) {
        if (versionNumber < 1 || versionNumber > VERSIONS.length) {
            throw new IllegalArgumentException("Unsupported version: " + versionNumber);
        }
        return VERSIONS[versionNumber - 1];
    }

    public static QrVersion smallestVersionForData(int dataBits, QrEccLevel level) {
        for (QrVersion version : VERSIONS) {
            if (version.ecBlocks(level).totalDataCodewords() * 8 >= dataBits) {
                return version;
            }
        }
        throw new IllegalArgumentException("Data too large for versions 1-10: " + dataBits + " bits");
    }

    private static EcBlocks eb(int ecPerBlock, EcBlock... blocks) {
        return new EcBlocks(ecPerBlock, blocks);
    }

    private static EcBlock b(int count, int dataCodewords) {
        return new EcBlock(count, dataCodewords);
    }

    private static QrVersion[] build() {
        return new QrVersion[]{
                new QrVersion(1, new int[]{},
                        new EcBlocks[]{eb(7, b(1, 19)), eb(10, b(1, 16)), eb(13, b(1, 13)), eb(17, b(1, 9))}),
                new QrVersion(2, new int[]{6, 18},
                        new EcBlocks[]{eb(10, b(1, 34)), eb(16, b(1, 28)), eb(22, b(1, 22)), eb(28, b(1, 16))}),
                new QrVersion(3, new int[]{6, 22},
                        new EcBlocks[]{eb(15, b(1, 55)), eb(26, b(1, 44)), eb(18, b(2, 17)), eb(22, b(2, 13))}),
                new QrVersion(4, new int[]{6, 26},
                        new EcBlocks[]{eb(20, b(1, 80)), eb(18, b(2, 32)), eb(26, b(2, 24)), eb(16, b(4, 9))}),
                new QrVersion(5, new int[]{6, 30},
                        new EcBlocks[]{eb(26, b(1, 108)), eb(24, b(2, 43)), eb(18, b(2, 15), b(2, 16)),
                                eb(22, b(2, 11), b(2, 12))}),
                new QrVersion(6, new int[]{6, 34},
                        new EcBlocks[]{eb(18, b(2, 68)), eb(16, b(4, 27)), eb(24, b(4, 19)), eb(28, b(4, 15))}),
                new QrVersion(7, new int[]{6, 22, 38},
                        new EcBlocks[]{eb(20, b(2, 78)), eb(18, b(4, 31)), eb(18, b(2, 14), b(4, 15)),
                                eb(26, b(4, 13), b(1, 14))}),
                new QrVersion(8, new int[]{6, 24, 42},
                        new EcBlocks[]{eb(24, b(2, 97)), eb(22, b(2, 38), b(2, 39)), eb(22, b(4, 18), b(2, 19)),
                                eb(26, b(4, 14), b(2, 15))}),
                new QrVersion(9, new int[]{6, 26, 46},
                        new EcBlocks[]{eb(30, b(2, 116)), eb(22, b(3, 36), b(2, 37)), eb(20, b(4, 16), b(4, 17)),
                                eb(24, b(4, 12), b(4, 13))}),
                new QrVersion(10, new int[]{6, 28, 50},
                        new EcBlocks[]{eb(18, b(2, 68), b(2, 69)), eb(26, b(4, 43), b(1, 44)),
                                eb(24, b(6, 19), b(2, 20)), eb(28, b(6, 15), b(2, 16))})
        };
    }
}
