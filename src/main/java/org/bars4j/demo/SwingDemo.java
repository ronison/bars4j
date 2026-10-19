/* ================================================================
 * Bars4J : Java Barcode Library
 * ================================================================
 *
 * Project Info:  https://github.com/ronison/bars4j
 * Project Lead:  Flavio Sampaio (contato@ronison.com.br);
 *
 * (C) Copyright 2005, by Favio Sampaio
 *
 * This library is free software; you can redistribute it and/or modify it underthe terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package org.bars4j.demo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.bars4j.encode.api.BarcodeException;
import org.bars4j.format.BarcodeFormat;
import org.bars4j.format.BarcodeRegistry;
import org.bars4j.format.FormatSpec;
import org.bars4j.format.LinearBarcodeFormat;
import org.bars4j.render.RenderSpec;
import org.bars4j.symbol.SymbolKind;
import org.bars4j.util.ImageUtil;

/**
 * Desktop demo application built with Swing.
 *
 * <p>Lets the user pick any registered format (1D or 2D), edit the content and the format-specific
 * options, and see the barcode update in real time. The result can be printed or exported to PNG /
 * SVG.
 *
 * <p>Run with: {@code mvn exec:java -Dexec.mainClass=org.bars4j.demo.SwingDemo}.
 */
public class SwingDemo extends JFrame {

    private static final long serialVersionUID = 1L;

    private final BarcodeRegistry registry = BarcodeRegistry.getInstance();

    private final JComboBox<FormatEntry> formatCombo = new JComboBox<>();
    private final JTextField textField = new JTextField(28);

    private final JSpinner xdimSpinner = new JSpinner(new SpinnerNumberModel(0.2646, 0.05, 5.0, 0.05));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(17.0, 1.0, 120.0, 1.0));
    private final JSpinner ratioSpinner = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 4.0, 0.1));
    private final JCheckBox showTextBox = new JCheckBox("Draw human readable text");
    private final JCheckBox checkDigitBox = new JCheckBox("Append check digit");
    private final JCheckBox showCheckDigitBox = new JCheckBox("Include check digit in label");

    private final JSpinner moduleSpinner = new JSpinner(new SpinnerNumberModel(4, 1, 24, 1));
    private final JButton foregroundButton = new JButton("Foreground");
    private final JButton backgroundButton = new JButton("Background");

    private final CardLayout configLayout = new CardLayout();
    private final JPanel configCards = new JPanel(configLayout);
    private final JPanel linearPanel = new JPanel(new GridBagLayout());
    private final JPanel matrixPanel = new JPanel(new GridBagLayout());

    private final BarcodePreview preview = new BarcodePreview();
    private final JLabel statusLabel = new JLabel(" ");

    private Color foreground = Color.BLACK;
    private Color background = Color.WHITE;

    private FormatEntry currentFormat;
    private BufferedImage currentImage;
    private String currentSvg;
    private String currentCheckSum = "";
    private boolean updating;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        SwingUtilities.invokeLater(() -> new SwingDemo().setVisible(true));
    }

    public SwingDemo() {
        super("Bars4J - Barcode Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initGui();
        populateFormats();
        setSize(1000, 620);
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ UI

    private void initGui() {
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(preview), buildSidePanel());
        split.setResizeWeight(1.0);
        split.setDividerLocation(670);
        split.setBorder(null);
        add(split, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);

        // Live updates.
        formatCombo.addActionListener(e -> onFormatChanged());
        textField.getDocument().addDocumentListener(new DocumentListenerAdapter(this::regenerate));
        xdimSpinner.addChangeListener(e -> regenerate());
        heightSpinner.addChangeListener(e -> regenerate());
        ratioSpinner.addChangeListener(e -> regenerate());
        moduleSpinner.addChangeListener(e -> regenerate());
        showTextBox.addActionListener(e -> regenerate());
        checkDigitBox.addActionListener(e -> regenerate());
        showCheckDigitBox.addActionListener(e -> regenerate());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(10, 12, 8, 12));

        JLabel title = new JLabel("Bars4J Barcode Demo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel selector = new JPanel();
        selector.setLayout(new BoxLayout(selector, BoxLayout.X_AXIS));
        selector.add(new JLabel("Format: "));
        formatCombo.setPreferredSize(new Dimension(240, 28));
        formatCombo.setMaximumSize(new Dimension(240, 28));
        selector.add(formatCombo);

        header.add(title, BorderLayout.WEST);
        header.add(selector, BorderLayout.EAST);
        return header;
    }

    private JPanel buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setBorder(new EmptyBorder(0, 8, 8, 12));
        side.setPreferredSize(new Dimension(330, 0));

        JPanel textPanel = new JPanel(new BorderLayout(0, 4));
        textPanel.setBorder(new TitledBorder("Content"));
        textPanel.add(textField, BorderLayout.CENTER);

        buildLinearPanel();
        buildMatrixPanel();
        configCards.add(linearPanel, "LINEAR");
        configCards.add(matrixPanel, "MATRIX");

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(textPanel, BorderLayout.NORTH);
        top.add(configCards, BorderLayout.CENTER);

        side.add(top, BorderLayout.NORTH);
        side.add(buildActions(), BorderLayout.SOUTH);
        return side;
    }

    private void buildLinearPanel() {
        linearPanel.setBorder(new TitledBorder("Linear options"));
        GridBagConstraints c = gbc();
        addRow(linearPanel, c, 0, "X-dimension (mm)", xdimSpinner);
        addRow(linearPanel, c, 1, "Bar height (mm)", heightSpinner);
        addRow(linearPanel, c, 2, "Wide ratio", ratioSpinner);
        addFull(linearPanel, c, 3, showTextBox);
        addFull(linearPanel, c, 4, checkDigitBox);
        addFull(linearPanel, c, 5, showCheckDigitBox);
    }

    private void buildMatrixPanel() {
        matrixPanel.setBorder(new TitledBorder("Matrix options"));
        GridBagConstraints c = gbc();
        addRow(matrixPanel, c, 0, "Module size (px)", moduleSpinner);
        addFull(matrixPanel, c, 1, labelFor(foregroundButton, () -> foreground, col -> {
            foreground = col;
            regenerate();
        }));
        addFull(matrixPanel, c, 2, labelFor(backgroundButton, () -> background, col -> {
            background = col;
            regenerate();
        }));
    }

    private JPanel buildActions() {
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setBorder(new TitledBorder("Actions"));
        GridBagConstraints c = gbc();
        JButton reset = new JButton("Reset to defaults");
        reset.addActionListener(e -> loadFormatDefaults(currentFormat));
        JButton print = new JButton("Print\u2026");
        print.addActionListener(e -> print());
        JButton png = new JButton("Export PNG\u2026");
        png.addActionListener(e -> exportPng());
        JButton svg = new JButton("Export SVG\u2026");
        svg.addActionListener(e -> exportSvg());

        addFull(actions, c, 0, reset);
        addFull(actions, c, 1, print);
        addFull(actions, c, 2, png);
        addFull(actions, c, 3, svg);
        return actions;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
                new EmptyBorder(4, 10, 4, 10)));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 2, 3, 2);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private static void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridy = row;
        if (label != null) {
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            panel.add(new JLabel(label), c);
        }
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
    }

    private static void addFull(JPanel panel, GridBagConstraints c, int row, java.awt.Component comp) {
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(comp, c);
        c.gridwidth = 1;
    }

    private JPanel labelFor(JButton button, java.util.function.Supplier<Color> getter,
                            java.util.function.Consumer<Color> setter) {
        updateSwatch(button, getter.get());
        button.addActionListener(e -> {
            Color chosen = JColorChooser.showDialog(this, button.getText(), getter.get());
            if (chosen != null) {
                setter.accept(chosen);
                updateSwatch(button, chosen);
            }
        });
        JPanel p = new JPanel(new BorderLayout());
        p.add(button, BorderLayout.CENTER);
        return p;
    }

    private static void updateSwatch(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(luminance(color) > 0.5 ? Color.BLACK : Color.WHITE);
        button.setOpaque(true);
        button.setBorderPainted(false);
    }

    private static double luminance(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }

    // -------------------------------------------------------------- formats

    private void populateFormats() {
        List<FormatEntry> entries = new ArrayList<>();
        for (String id : registry.ids()) {
            FormatSpec spec = registry.get(id).spec();
            entries.add(new FormatEntry(spec.id(), spec.displayName(), spec.kind()));
        }
        entries.sort(Comparator.comparing(FormatEntry::displayName, String.CASE_INSENSITIVE_ORDER));

        updating = true;
        for (FormatEntry entry : entries) {
            formatCombo.addItem(entry);
        }
        formatCombo.setSelectedIndex(0);
        updating = false;
        onFormatChanged();
    }

    private void onFormatChanged() {
        if (updating) {
            return;
        }
        FormatEntry selected = (FormatEntry) formatCombo.getSelectedItem();
        if (selected != null) {
            loadFormatDefaults(selected);
        }
    }

    private void loadFormatDefaults(FormatEntry entry) {
        if (entry == null) {
            return;
        }
        currentFormat = entry;
        updating = true;
        try {
            BarcodeFormat format = registry.get(entry.id());
            FormatSpec spec = format.spec();

            if (spec.kind() == SymbolKind.LINEAR) {
                configLayout.show(configCards, "LINEAR");
                LinearBarcodeFormat.LinearOptions options =
                        ((LinearBarcodeFormat) format).options();
                xdimSpinner.setValue(round2(options.xDimensionMm()));
                heightSpinner.setValue(round1(options.barHeightMm()));
                ratioSpinner.setValue(round1(options.wideRatio()));
                showTextBox.setSelected(options.showText());
                checkDigitBox.setSelected(options.checkDigit());
                showCheckDigitBox.setSelected(options.showCheckDigit());

                showTextBox.setEnabled(spec.allowsText());
                checkDigitBox.setEnabled(spec.supportsCheckDigit());
                showCheckDigitBox.setEnabled(spec.supportsCheckDigit() && spec.allowsText());
                ratioSpinner.setEnabled(spec.allowsWideRatio());
            } else {
                configLayout.show(configCards, "MATRIX");
                moduleSpinner.setValue(4);
            }
            textField.setText(SampleContent.forFormat(entry.id()));
            textField.setCaretPosition(0);
        } finally {
            updating = false;
        }
        regenerate();
    }

    // ------------------------------------------------------------- generate

    private void regenerate() {
        if (updating || currentFormat == null) {
            return;
        }
        String id = currentFormat.id();
        String text = textField.getText();
        if (text == null || text.isEmpty()) {
            currentImage = null;
            currentSvg = null;
            preview.setMessage("Type some content to preview the barcode");
            statusLabel.setText(currentFormat.displayName() + " - waiting for content");
            return;
        }

        try {
            BarcodeFormat format = registry.get(id);
            if (currentFormat.kind() == SymbolKind.LINEAR) {
                LinearBarcodeFormat linear = ((LinearBarcodeFormat) format).withOptions(currentLinearOptions());
                RenderSpec spec = RenderSpec.linearDefaults();
                currentImage = linear.toImage(text, spec);
                currentSvg = linear.toSvg(text, spec);
                currentCheckSum = format.spec().supportsCheckDigit() ? format.checkSum(text) : "";
            } else {
                RenderSpec spec = new RenderSpec(1, 70, 2.0, ((Number) moduleSpinner.getValue()).intValue(),
                        foreground, background);
                currentImage = format.toImage(text, spec);
                currentSvg = format.toSvg(text, spec);
                currentCheckSum = "";
            }
            preview.setImage(currentImage);
            statusLabel.setText(buildStatus());
        } catch (BarcodeException | RuntimeException ex) {
            currentImage = null;
            currentSvg = null;
            currentCheckSum = "";
            preview.setMessage(message(ex));
            statusLabel.setText(currentFormat.displayName() + " - " + message(ex));
        }
    }

    private LinearBarcodeFormat.LinearOptions currentLinearOptions() {
        return new LinearBarcodeFormat.LinearOptions(
                ((Number) xdimSpinner.getValue()).doubleValue(),
                ((Number) heightSpinner.getValue()).doubleValue(),
                ((Number) ratioSpinner.getValue()).doubleValue(),
                checkDigitBox.isSelected(),
                showCheckDigitBox.isSelected(),
                showTextBox.isSelected());
    }

    private String buildStatus() {
        StringBuilder sb = new StringBuilder(currentFormat.displayName())
                .append(" (").append(currentFormat.id()).append(")");
        if (currentImage != null) {
            sb.append(" - ").append(currentImage.getWidth()).append(" x ")
                    .append(currentImage.getHeight()).append(" px");
        }
        if (!currentCheckSum.isEmpty()) {
            sb.append(" - check digit: ").append(currentCheckSum);
        }
        return sb.toString();
    }

    private String message(Throwable ex) {
        String msg = ex.getMessage();
        return msg == null || msg.isEmpty() ? ex.getClass().getSimpleName() : msg;
    }

    // ---------------------------------------------------------------- export

    private void print() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, "Nothing to print yet.", "Print",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new BarcodePrintable(currentImage));
        if (job.printDialog()) {
            try {
                job.print();
                statusLabel.setText("Sent to printer: " + currentFormat.displayName());
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Print failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportPng() {
        if (currentImage == null) {
            JOptionPane.showMessageDialog(this, "Nothing to export yet.", "Export PNG",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = chooseFile("png", currentFormat.id() + ".png");
        if (file == null) {
            return;
        }
        try (java.io.OutputStream out = new java.io.FileOutputStream(file)) {
            ImageUtil.encodeAndWrite(currentImage, ImageUtil.PNG, out, 96, 96);
            statusLabel.setText("Exported " + file.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportSvg() {
        if (currentSvg == null) {
            JOptionPane.showMessageDialog(this, "Nothing to export yet.", "Export SVG",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        File file = chooseFile("svg", currentFormat.id() + ".svg");
        if (file == null) {
            return;
        }
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.append(currentSvg);
            statusLabel.setText("Exported " + file.getName());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Export failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private File chooseFile(String extension, String suggestedName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(suggestedName));
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase() + " files", extension));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith("." + extension)) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        return file;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ------------------------------------------------------------- helpers

    /** A format entry shown in the combo box. */
    private record FormatEntry(String id, String displayName, SymbolKind kind) {
        @Override
        public String toString() {
            return displayName + "  (" + id + ")";
        }
    }

    /** Small listener adapter to react to every document change. */
    private static final class DocumentListenerAdapter implements javax.swing.event.DocumentListener {
        private final Runnable action;

        DocumentListenerAdapter(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }
    }

    /** Preview area: draws the barcode centred and scaled to fit, with mouse-wheel zoom. */
    private static final class BarcodePreview extends JPanel {
        private static final long serialVersionUID = 1L;

        private BufferedImage image;
        private String message = "No barcode";
        private double zoom = 1.0;

        BarcodePreview() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(400, 300));
            addMouseWheelListener(e -> {
                zoom *= e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1;
                zoom = Math.max(0.1, Math.min(8.0, zoom));
                repaint();
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        zoom = 1.0;
                        repaint();
                    }
                }
            });
        }

        void setImage(BufferedImage image) {
            this.image = image;
            this.message = null;
            repaint();
        }

        void setMessage(String message) {
            this.image = null;
            this.message = message;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (image == null) {
                g2.setColor(new Color(120, 120, 120));
                g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
                String msg = message == null ? "No barcode" : message;
                int w = g2.getFontMetrics().stringWidth(msg);
                g2.drawString(msg, (getWidth() - w) / 2, getHeight() / 2);
                g2.dispose();
                return;
            }

            int margin = 16;
            double fit = Math.min((getWidth() - 2.0 * margin) / image.getWidth(),
                    (getHeight() - 2.0 * margin) / image.getHeight());
            double scale = fit * zoom;
            int w = Math.max(1, (int) Math.round(image.getWidth() * scale));
            int h = Math.max(1, (int) Math.round(image.getHeight() * scale));
            int x = (getWidth() - w) / 2;
            int y = (getHeight() - h) / 2;

            // Drop shadow + frame around the rendered area.
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRect(x + 3, y + 3, w, h);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(image, x, y, w, h, null);
            g2.setColor(new Color(190, 190, 190));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(x, y, w, h);
            g2.dispose();
        }
    }

    /** Prints the barcode centred and scaled to fit the page. */
    private static final class BarcodePrintable implements Printable {
        private final BufferedImage image;

        BarcodePrintable(BufferedImage image) {
            this.image = image;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            double pw = pageFormat.getImageableWidth();
            double ph = pageFormat.getImageableHeight();
            double scale = Math.min(pw / image.getWidth(), ph / image.getHeight());
            int w = (int) Math.round(image.getWidth() * scale);
            int h = (int) Math.round(image.getHeight() * scale);
            int x = (int) (pageFormat.getImageableX() + (pw - w) / 2);
            int y = (int) (pageFormat.getImageableY() + (ph - h) / 2);
            g2.drawImage(image, x, y, w, h, null);
            g2.dispose();
            return PAGE_EXISTS;
        }
    }
}
