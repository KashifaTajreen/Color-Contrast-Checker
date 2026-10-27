package contrast;
import contrast.Rules.*;
import contrast.ContrastChecker.*;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
/** Demonstrates composition and dependency injection in a recoverable menu loop. */
public final class ConsoleApp {
    private final Scanner scanner;
    private final PrintStream output;
    private final ContrastChecker checker;
    private final ColorSuggester suggester;
    private final Printer printer;
    private Attempt lastFailure;
    public ConsoleApp(Scanner scanner, PrintStream output,
            ContrastChecker checker, ColorSuggester suggester, Printer printer) {
        this.scanner = Objects.requireNonNull(scanner);
        this.output = Objects.requireNonNull(output);
        this.checker = Objects.requireNonNull(checker);
        this.suggester = Objects.requireNonNull(suggester);
        this.printer = Objects.requireNonNull(printer);
    }
    public void run() {
        output.println("Color Contrast Checker");
        try {
            while (true) {
                output.println("\n1) Check a color pair\n2) Try suggested fix for last failing pair"
                        + "\n3) Show accessibility limitations\n4) Exit");
                switch (read("Choice: ")) {
                    case "1" -> checkPair();
                    case "2" -> tryFix();
                    case "3" -> printer.printLimitations();
                    case "4" -> { return; }
                    default -> output.println("Choose 1, 2, 3, or 4.");
                }
            }
        } catch (EndOfInput exception) {
            output.println("\nInput ended. Goodbye.");
        }
    }
    private void checkPair() {
        Color fg = readColor("Foreground: ");
        Color bg = readColor("Background: ");
        AccessibilityRule rule = checker.ruleFor(readLevel());
        TextSize size = readSize();
        Report result = checker.check(fg, bg);
        printer.print(result, rule, size);
        RuleResult focus = rule.evaluate(result.ratio(), size);
        if (focus.applicable() && !focus.passed()) {
            lastFailure = new Attempt(result, rule, size);
        }
    }
    private void tryFix() {
        if (lastFailure == null) {
            output.println("Check a pair that fails an applicable selected target first.");
            return;
        }
        Attempt attempt = lastFailure;
        Report original = attempt.result();
        Color fix = suggester.suggest(original.fg(), original.bg(),
                attempt.rule().thresholdFor(attempt.size()));
        if (fix == null) {
            output.println("No passing foreground was found on this HSL lightness path.");
            return;
        }
        output.println("Suggested foreground: " + fix);
        printer.print(checker.check(fix, original.bg()), attempt.rule(), attempt.size());
        lastFailure = null;
    }
    private Color readColor(String prompt) {
        while (true) {
            try {
                return ColorParser.parseAny(read(prompt));
            } catch (InvalidColorException exception) {
                output.println("Invalid color: " + exception.getMessage());
            }
        }
    }
    private Level readLevel() {
        while (true) {
            String value = read("Target level (AA/AAA): ").toUpperCase(Locale.ROOT);
            for (Level level : Level.values()) {
                if (level.name().equals(value)) {
                    return level;
                }
            }
            output.println("Enter AA or AAA.");
        }
    }
    private TextSize readSize() {
        while (true) {
            switch (read("Category (1=normal, 2=large, 3=UI component): ")) {
                case "1": return TextSize.NORMAL;
                case "2": return TextSize.LARGE;
                case "3": return TextSize.UI_COMPONENT;
                default: output.println("Enter 1, 2, or 3.");
            }
        }
    }
    private String read(String prompt) {
        output.print(prompt);
        output.flush();
        if (!scanner.hasNextLine()) {
            throw new EndOfInput();
        }
        return scanner.nextLine().trim();
    }
    private record Attempt(Report result, AccessibilityRule rule, TextSize size) { }
    private static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
