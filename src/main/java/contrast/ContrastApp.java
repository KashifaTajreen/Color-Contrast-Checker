package contrast;
import contrast.ContrastChecker.*;
import contrast.Rules.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Locale;
/** Composition and listeners: Swing presents model results without owning calculation or rules. */
public final class ContrastApp {
    private final ContrastChecker checker;
    private final ColorSuggester suggester;
    private final JFrame frame = new JFrame("Color Contrast Checker");
    private final JPanel content = new JPanel();
    private final JTextField foreground = new JTextField("#333333", 22);
    private final JTextField background = new JTextField("#FFFFFF", 22);
    private final JLabel fgError = new JLabel(" "), bgError = new JLabel(" ");
    private final JLabel fgSwatch = swatch(), bgSwatch = swatch(), fixSwatch = swatch();
    private final JComboBox<String> category = new JComboBox<>(new String[] {"Normal text", "Large text", "Button or icon"});
    private final JComboBox<String> strictness = new JComboBox<>(new String[] {"Standard (AA)", "Strict (AAA)"});
    private final JLabel categoryHint = new JLabel(), levelHint = new JLabel();
    private final JPanel preview = new JPanel();
    private final JLabel small = new JLabel("The quick brown fox"), large = new JLabel("The quick brown fox");
    private final JLabel ratio = new JLabel(), verdict = new JLabel();
    private final JTextArea sentence = new JTextArea(2, 42);
    private final JPanel fixPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JLabel fixText = new JLabel();
    private final JButton useFix = new JButton("Use this color");
    private final DefaultTableModel rows = new DefaultTableModel(new String[] {"Level", "Text type", "Needs", "Result"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final Border normalBorder = foreground.getBorder();
    private final java.awt.Color green = new java.awt.Color(0, 100, 30), red = new java.awt.Color(170, 0, 0);
    private SwingWorker<Color, Void> worker;
    private Color suggested;
    private long revision;
    private ContrastApp(ContrastChecker checker, ColorSuggester suggester) {
        this.checker = checker; this.suggester = suggester;
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel title = new JLabel("Color Contrast Checker"); title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
        add(title); add(new JLabel("Check whether text is easy to read on its background."));
        inputRow("Text color", foreground, fgSwatch, fgError, 'T');
        inputRow("Background color", background, bgSwatch, bgError, 'B');
        JTextArea hint = new JTextArea("Type a color like #FFC0CB, rgb(255, 192, 203), or a name like navy.");
        hint.setEditable(false); hint.setLineWrap(true); hint.setWrapStyleWord(true); hint.setOpaque(false); add(hint);
        JButton swap = new JButton("Swap colors"); swap.setMnemonic('S');
        swap.addActionListener(e -> { String text = foreground.getText(); foreground.setText(background.getText()); background.setText(text); });
        add(swap);
        choice("What are you checking?", category, categoryHint, 'W');
        choice("How strict?", strictness, levelHint, 'H');
        category.setToolTipText("Choose the kind of text or control you want people to read.");
        strictness.setToolTipText("AA is the standard target; AAA asks for stronger text contrast.");
        preview.setLayout(new BoxLayout(preview, BoxLayout.Y_AXIS));
        preview.setBorder(BorderFactory.createTitledBorder("Preview"));
        small.setFont(small.getFont().deriveFont(16f)); large.setFont(large.getFont().deriveFont(25f));
        preview.add(small); preview.add(large); add(preview);
        ratio.setFont(ratio.getFont().deriveFont(Font.BOLD, 28f)); add(ratio);
        verdict.setOpaque(true); verdict.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8)); add(verdict);
        sentence.setEditable(false); sentence.setLineWrap(true); sentence.setWrapStyleWord(true); sentence.setOpaque(false); add(sentence);
        JTable table = new JTable(rows); table.setRowHeight(25); table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus, int row, int col) {
                Component cell = super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                cell.setForeground(col == 3 ? statusColor(String.valueOf(value)) : t.getForeground()); return cell;
            }
        });
        JScrollPane tableScroll = new JScrollPane(table); tableScroll.setPreferredSize(new Dimension(570, 180)); add(tableScroll);
        fixPanel.setBorder(BorderFactory.createTitledBorder("Suggested fix"));
        fixPanel.add(fixSwatch); fixPanel.add(fixText); fixPanel.add(useFix); add(fixPanel);
        useFix.setMnemonic('U'); useFix.addActionListener(e -> { if (suggested != null) foreground.setText(suggested.toHex()); });
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.add(new JLabel("Contrast is only one part of accessibility."));
        JButton more = new JButton("What else matters?"); more.setMnemonic('M');
        more.addActionListener(e -> JOptionPane.showMessageDialog(frame, Printer.LIMITATIONS, "What else matters?", JOptionPane.INFORMATION_MESSAGE));
        footer.add(more); add(footer);
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh(); }
            public void removeUpdate(DocumentEvent e) { refresh(); }
            public void changedUpdate(DocumentEvent e) { refresh(); }
        };
        foreground.getDocument().addDocumentListener(listener); background.getDocument().addDocumentListener(listener);
        category.addActionListener(e -> refresh()); strictness.addActionListener(e -> refresh());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.setContentPane(new JScrollPane(content));
        frame.setSize(640, 720); frame.setLocationRelativeTo(null); refresh(); frame.setVisible(true);
    }
    public static void launch(ContrastChecker checker, ColorSuggester suggester) {
        if (GraphicsEnvironment.isHeadless()) { System.err.println("No display is available. Run with --console."); return; }
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) { }
            // Set defaults before creating controls, including table headings and dialogs.
            for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) {
                Object value = UIManager.get(key);
                if (value instanceof Font font) UIManager.put(key, font.deriveFont(Math.max(14f, font.getSize2D())));
            }
            new ContrastApp(checker, suggester);
        });
    }
    private void add(JComponent c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); content.add(c); content.add(Box.createVerticalStrut(5)); }
    private static JLabel swatch() {
        JLabel label = new JLabel("   "); label.setOpaque(true);
        label.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY)); return label;
    }
    private void inputRow(String text, JTextField field, JLabel swatch, JLabel error, char mnemonic) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel(text); label.setLabelFor(field); label.setDisplayedMnemonic(mnemonic);
        JButton pick = new JButton("Pick..."); pick.setMnemonic(text.equals("Text color") ? 'P' : 'K');
        pick.addActionListener(e -> {
            java.awt.Color chosen = JColorChooser.showDialog(frame, text, swatch.getBackground());
            if (chosen != null) field.setText(new Color(chosen.getRed(), chosen.getGreen(), chosen.getBlue()).toHex());
        });
        row.add(label); row.add(field); row.add(swatch); row.add(pick); add(row);
        error.setForeground(red); add(error);
    }
    private void choice(String text, JComboBox<String> box, JLabel hint, char mnemonic) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT)); JLabel label = new JLabel(text);
        label.setLabelFor(box); label.setDisplayedMnemonic(mnemonic); row.add(label); row.add(box); add(row); add(hint);
    }
    private Color parse(JTextField field, JLabel error, JLabel swatch) {
        try {
            if (field.getText().isBlank()) throw new InvalidColorException("Enter a color");
            Color color = ColorParser.parseAny(field.getText());
            error.setText(" "); field.setBorder(normalBorder); swatch.setBackground(paint(color)); return color;
        } catch (InvalidColorException ex) {
            error.setText("<html>" + ex.getMessage() + "</html>");
            field.setBorder(BorderFactory.createLineBorder(red)); return null;
        }
    }
    private void refresh() {
        long current = ++revision;
        if (worker != null) worker.cancel(true);
        categoryHint.setText(switch (category.getSelectedIndex()) {
            case 1 -> "Large text means 18pt or bigger, or 14pt or bigger in bold.";
            case 2 -> "Checks the visible parts of a button or icon.";
            default -> "Normal text includes paragraphs and smaller labels.";
        });
        levelHint.setText(strictness.getSelectedIndex() == 0 ? "Standard (AA) is the usual contrast target." : "Strict (AAA) asks for stronger text contrast.");
        Color fg = parse(foreground, fgError, fgSwatch), bg = parse(background, bgError, bgSwatch);
        if (fg == null || bg == null) { useFix.setEnabled(false); return; }
        Report report = checker.check(fg, bg);
        AccessibilityRule rule = checker.ruleFor(strictness.getSelectedIndex() == 0 ? Level.AA : Level.AAA);
        TextSize size = TextSize.values()[category.getSelectedIndex()]; RuleResult focus = report.selected(rule, size);
        preview.setBackground(paint(bg)); small.setForeground(paint(fg)); large.setForeground(paint(fg));
        ratio.setText(String.format(Locale.ROOT, "%.2f : 1", report.ratio()));
        verdict.setText(focus.verdict()); verdict.setForeground(statusColor(focus.verdict()));
        verdict.setBackground(focus.applicable() ? (focus.passed() ? new java.awt.Color(220, 245, 225) : new java.awt.Color(255, 225, 225)) : new java.awt.Color(235, 235, 235));
        sentence.setText(report.verdict(rule, size)); rows.setRowCount(0);
        for (RuleResult row : report.results()) rows.addRow(new Object[] {row.ruleName().replace("WCAG ", ""), row.size().label(),
                row.applicable() ? String.format(Locale.ROOT, "%.2f:1", row.requiredRatio()) : "N/A", row.verdict()});
        boolean failed = focus.applicable() && !focus.passed(); fixPanel.setVisible(failed); suggested = null; useFix.setEnabled(false);
        if (failed) {
            fixText.setText("Finding a text color..."); fixSwatch.setVisible(false);
            // Search away from the event thread so typing and window controls stay responsive.
            worker = new SwingWorker<>() {
                protected Color doInBackground() { return suggester.suggest(fg, bg, focus.requiredRatio()); }
                protected void done() {
                    if (isCancelled() || current != revision) return;
                    try {
                        suggested = get();
                        fixText.setText(suggested == null ? "No fix is possible on this lightness path." : suggested.toHex());
                        fixSwatch.setVisible(suggested != null); useFix.setEnabled(suggested != null);
                        if (suggested != null) fixSwatch.setBackground(paint(suggested));
                    } catch (Exception ex) { fixText.setText("Could not find a suggested color."); }
                }
            }; worker.execute();
        }
        content.revalidate(); content.repaint();
    }
    private java.awt.Color statusColor(String status) { return status.equals("PASS") ? green : status.equals("FAIL") ? red : java.awt.Color.DARK_GRAY; }
    private static java.awt.Color paint(Color c) { return new java.awt.Color(c.r(), c.g(), c.b()); }
}
