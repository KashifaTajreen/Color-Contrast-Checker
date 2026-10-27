package contrast;
import contrast.ContrastChecker.*;
import contrast.Rules.*;
import java.io.PrintStream;
import java.util.Locale;
/** Encapsulation keeps ANSI presentation separate from model records and rules. */
public final class Printer {
    public static final boolean USE_COLOR = true;
    private static final String EDGE = "==================================================";
    private static final String BORDER = "  +-------+--------------+--------+--------+";
    public static final String LIMITATIONS = """
            1. Contrast is only one part of accessibility.
            2. Font size and weight matter.
            3. Do not rely on color alone, including for people with color blindness.
            4. Provide visible focus indicators and keyboard navigation.
            5. Give images useful alt text.
            6. Support screen readers.
            7. Consider motion and flashing content.
            8. Consider text spacing and line length.
            9. These checks alone do not establish WCAG conformance.
            """;
    private final PrintStream output;
    public Printer(PrintStream output) { this.output = java.util.Objects.requireNonNull(output); }
    public void print(Report report, AccessibilityRule target, TextSize size) {
        output.println("\n" + EDGE + "\n  COLOR CONTRAST CHECKER\n" + EDGE);
        output.println("  Foreground : " + report.fg());
        output.println("  Background : " + report.bg());
        String preview = " Sample Text ";
        if (USE_COLOR) preview = ansi(report.fg(), 38) + ansi(report.bg(), 48) + preview + "\033[0m";
        output.println("  Preview    : " + preview);
        int filled = (int) Math.round(report.ratio() / 21 * 20);
        output.printf(Locale.ROOT, "%n  Ratio   : %.2f : 1   [%s%s]%n", report.ratio(),
                "#".repeat(filled), "-".repeat(20 - filled));
        output.println("  Verdict : " + report.tier() + "\n");
        output.println(BORDER);
        output.printf("  | %-5s | %-12s | %-6s | %-6s |%n", "Level", "Text type", "Needs", "Result");
        output.println(BORDER);
        for (RuleResult row : report.results()) {
            String needs = row.applicable() ? String.format(Locale.ROOT, "%.2f:1", row.requiredRatio()) : "N/A";
            String padded = String.format("%-6s", row.verdict());
            String status = USE_COLOR ? "\033[" + (row.applicable() ? (row.passed() ? "32" : "31") : "90")
                    + "m" + padded + "\033[0m" : padded;
            output.printf("  | %-5s | %-12s | %-6s | %s |%n", row.ruleName().replace("WCAG ", ""),
                    row.size().label(), needs, status);
        }
        output.println(BORDER);
        long passed = report.results().stream().filter(RuleResult::passed).count();
        long applicable = report.results().stream().filter(RuleResult::applicable).count();
        output.println("  " + passed + " of " + applicable + " applicable checks passed\n");
        output.println("  Selected target: " + target.level() + " / " + size.label() + " -> "
                + report.selected(target, size).verdict());
        output.println("  " + report.verdict(target, size));
        output.println("  Note: contrast is only one part of accessibility. Choose 3 in the menu for the full list.");
        output.println(EDGE);
    }
    private String ansi(Color c, int kind) { return "\033[" + kind + ";2;" + c.r() + ";" + c.g() + ";" + c.b() + "m"; }
    public void printLimitations() { output.print(LIMITATIONS); }
}
