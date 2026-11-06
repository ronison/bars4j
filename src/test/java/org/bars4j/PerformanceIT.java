package org.bars4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bars4j.api.BarcodeGenerator;
import org.bars4j.demo.SampleContent;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.symbol.SymbolKind;
import org.junit.jupiter.api.Test;

/**
 * Automated performance gate for the barcode generation component.
 *
 * <p>Runs in {@code mvn verify} (failsafe, {@code *IT}) and is deliberately kept out of the fast
 * unit-test phase. It asserts on very conservative budgets (roughly a fifth of what a developer
 * machine delivers) so it catches catastrophic regressions — e.g. reintroducing per-pixel
 * {@code setRGB} in the matrix renderer, which used to make 2D generation 5-30x slower — without
 * being flaky on slower CI hardware.
 *
 * <p>The exact numbers are printed so a change in throughput/latency is visible in the build log.
 * Tune the constants together with the numbers when the implementation legitimately changes.
 */
class PerformanceIT {

    /** Minimum image throughput (ops/s) accepted for the heavier 2D formats. */
    private static final Map<String, Double> MIN_OPS_PER_SEC_2D = Map.of(
            "QRCODE", 1_200.0,
            "DATAMATRIX", 12_000.0,
            "PDF417", 8_000.0,
            "AZTEC", 6_000.0);

    private static final double MIN_OPS_PER_SEC_DEFAULT = 1_000.0;
    private static final double MIN_OPS_PER_SEC_1D = 2_000.0;
    private static final long MAX_P99_NANOS_1D = 5_000_000L;   // 5 ms
    private static final long MAX_P99_NANOS_2D = 10_000_000L;  // 10 ms

    private static final int WARMUP_1D = 500;
    private static final int WARMUP_2D = 150;
    private static final int MEASURE_1D = 2_000;
    private static final int MEASURE_2D = 500;

    @Test
    void generatesAllFormatsWithinBudgets() throws Exception {
        List<String> ids = new ArrayList<>(BarcodeRegistry.getInstance().ids());
        ids.sort(String::compareTo);

        Map<String, double[]> results = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        for (String id : ids) {
            BarcodeGenerator generator = BarcodeGenerator.of(id);
            String text = SampleContent.forFormat(id);
            boolean matrix = generator.format().kind() == SymbolKind.MATRIX;
            int warm = matrix ? WARMUP_2D : WARMUP_1D;
            int n = matrix ? MEASURE_2D : MEASURE_1D;

            for (int i = 0; i < warm; i++) {
                assertNotNull(generator.toImage(text));
            }

            long[] latencies = new long[n];
            for (int i = 0; i < n; i++) {
                long start = System.nanoTime();
                BufferedImage image = generator.toImage(text);
                latencies[i] = System.nanoTime() - start;
                assertTrue(image.getWidth() > 0 && image.getHeight() > 0,
                        id + " produced an empty image");
            }

            double opsPerSec = n / (sum(latencies) / 1e9);
            double p50Micros = percentile(latencies, 0.50) / 1e3;
            double p99Micros = percentile(latencies, 0.99) / 1e3;

            results.put(id, new double[]{opsPerSec, p50Micros, p99Micros});

            double minOps = matrix ? MIN_OPS_PER_SEC_2D.getOrDefault(id, MIN_OPS_PER_SEC_DEFAULT)
                    : MIN_OPS_PER_SEC_1D;
            long maxP99 = matrix ? MAX_P99_NANOS_2D : MAX_P99_NANOS_1D;

            if (opsPerSec < minOps) {
                failures.add(String.format(Locale.ROOT,
                        "%s: %.0f ops/s < %.0f ops/s budget", id, opsPerSec, minOps));
            }
            if (percentile(latencies, 0.99) > maxP99) {
                failures.add(String.format(Locale.ROOT,
                        "%s: p99 %.0f us > %.0f us budget", id, p99Micros, maxP99 / 1e3));
            }
        }

        report(results);
        assertTrue(failures.isEmpty(),
                "performance budget violations:\n  " + String.join("\n  ", failures));
    }

    private static void report(Map<String, double[]> results) {
        System.out.printf(Locale.ROOT, "%nPERF  %-16s %10s %9s %9s%n",
                "format", "ops/s", "p50us", "p99us");
        results.forEach((id, r) -> System.out.printf(Locale.ROOT,
                "PERF  %-16s %10.0f %9.1f %9.1f%n", id, r[0], r[1], r[2]));
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return total;
    }

    private static long percentile(long[] values, double percentile) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }
}
