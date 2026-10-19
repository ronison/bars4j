package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Support for golden (characterization) tests.
 *
 * <p>Goldens are stored under {@code src/test/resources/golden}. Run the build with
 * {@code -Dbars4j.updateGolden=true} to (re)generate them.
 */
final class GoldenTestSupport {

    private static final Path GOLDEN_DIR = Path.of("src", "test", "resources", "golden");
    private static final boolean UPDATE = Boolean.getBoolean("bars4j.updateGolden");

    private GoldenTestSupport() {
    }

    static void assertSvg(String name, String actual) throws IOException {
        Path file = GOLDEN_DIR.resolve(name + ".svg");
        if (UPDATE) {
            write(file, actual);
            return;
        }
        if (!Files.exists(file)) {
            throw new AssertionError("Missing SVG golden: " + file
                    + " (run with -Dbars4j.updateGolden=true to generate)");
        }
        assertEquals(Files.readString(file), actual, "SVG mismatch for " + name);
    }

    /**
     * Fingerprints only the bar geometry (text disabled) so the result is stable across
     * platforms/JDKs, storing width, height and a SHA-256 of the ARGB pixels.
     */
    static void assertImage(String name, BufferedImage image) throws IOException {
        String actual = fingerprint(image);
        Path file = GOLDEN_DIR.resolve(name + ".image.txt");
        if (UPDATE) {
            write(file, actual);
            return;
        }
        if (!Files.exists(file)) {
            throw new AssertionError("Missing image golden: " + file
                    + " (run with -Dbars4j.updateGolden=true to generate)");
        }
        assertEquals(Files.readString(file), actual, "Image mismatch for " + name);
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String fingerprint(BufferedImage image) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(intToBytes(image.getWidth()));
            digest.update(intToBytes(image.getHeight()));
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    digest.update(intToBytes(image.getRGB(x, y)));
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return "width=" + image.getWidth() + "\nheight=" + image.getHeight() + "\nsha256=" + sb + "\n";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    static String svgOf(Barcode barcode, String code) throws Exception {
        return barcode.createBarcodeSVG(code).toString().replace("\r\n", "\n");
    }

    static BufferedImage imageOf(Barcode barcode, String code) throws Exception {
        Barcode noText = barcode;
        noText.setShowText(false);
        return noText.createBarcode(code);
    }

    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
