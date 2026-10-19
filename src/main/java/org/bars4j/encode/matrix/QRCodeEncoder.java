package org.bars4j.encode.matrix;

import java.nio.charset.Charset;

import org.bars4j.encode.api.MatrixEncoder;
import org.bars4j.encode.api.UnsupportedTextException;
import org.bars4j.encode.matrix.qr.QrBitStream;
import org.bars4j.encode.matrix.qr.QrDataBlocks;
import org.bars4j.encode.matrix.qr.QrEccLevel;
import org.bars4j.encode.matrix.qr.QrMatrixBuilder;
import org.bars4j.encode.matrix.qr.QrSegment;
import org.bars4j.encode.matrix.qr.QrSegmentEncoder;
import org.bars4j.encode.matrix.qr.QrVersion;
import org.bars4j.symbol.MatrixSymbol;

/**
 * Self-contained QRCode encoder (versions 1-10, ECC levels L/M/Q/H, numeric/alphanumeric/byte
 * modes). No runtime dependency on any external QR library.
 */
public final class QRCodeEncoder implements MatrixEncoder {

    private static final int MAX_VERSION = 10;
    private static final int QUIET_ZONE = 4;

    private final QrEccLevel eccLevel;
    private final Charset charset;
    private final Integer forcedVersion;

    public QRCodeEncoder() {
        this(QrEccLevel.M, QrSegmentEncoder.defaultCharset(), null);
    }

    public QRCodeEncoder(QrEccLevel eccLevel) {
        this(eccLevel, QrSegmentEncoder.defaultCharset(), null);
    }

    public QRCodeEncoder(QrEccLevel eccLevel, Charset charset, Integer forcedVersion) {
        this.eccLevel = eccLevel;
        this.charset = charset;
        this.forcedVersion = forcedVersion;
    }

    public QrEccLevel eccLevel() {
        return eccLevel;
    }

    @Override
    public MatrixSymbol encode(String text) throws UnsupportedTextException {
        QrSegment segment = QrSegmentEncoder.encode(text, charset);
        QrVersion version = chooseVersion(segment);
        QrVersion.EcBlocks ecBlocks = version.ecBlocks(eccLevel);

        QrBitStream stream = new QrBitStream();
        segment.appendTo(stream, version.versionNumber());
        stream.padToCapacity(ecBlocks.totalDataCodewords());

        int[] dataCodewords = stream.toCodewords();
        int[] codewords = QrDataBlocks.interleave(dataCodewords, ecBlocks);
        boolean[][] modules = QrMatrixBuilder.build(version, eccLevel, codewords);
        return new MatrixSymbol(modules, QUIET_ZONE);
    }

    @Override
    public String computeCheckSum(String text) {
        // QRCode error correction is computed internally, there is no external check digit.
        return "";
    }

    private QrVersion chooseVersion(QrSegment segment) throws UnsupportedTextException {
        if (forcedVersion != null) {
            QrVersion version = QrVersion.getVersionForNumber(forcedVersion);
            if (requiredBits(segment, version.versionNumber()) > version.ecBlocks(eccLevel).totalDataCodewords() * 8) {
                throw new UnsupportedTextException(
                        "Data does not fit in forced version " + forcedVersion + " at level " + eccLevel);
            }
            return version;
        }
        for (int number = 1; number <= MAX_VERSION; number++) {
            QrVersion version = QrVersion.getVersionForNumber(number);
            if (requiredBits(segment, number) <= version.ecBlocks(eccLevel).totalDataCodewords() * 8) {
                return version;
            }
        }
        throw new UnsupportedTextException("Data too large for QRCode versions 1-" + MAX_VERSION);
    }

    private static int requiredBits(QrSegment segment, int version) {
        return 4 + segment.mode().charCountBitsForVersion(version) + segment.dataBitLength();
    }
}
