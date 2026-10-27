package contrast;
import contrast.Rules.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/** Demonstrates dependency injection, composition, streams, and lambdas. */
public final class ContrastChecker {
    private final ContrastCalculator calculator;
    private final List<AccessibilityRule> rules;
    public ContrastChecker(ContrastCalculator calculator, List<AccessibilityRule> rules) {
        this.calculator = Objects.requireNonNull(calculator, "Calculator is required.");
        this.rules = List.copyOf(Objects.requireNonNull(rules, "Rules are required."));
        if (this.rules.isEmpty()) {
            throw new IllegalArgumentException("At least one rule is required.");
        }
    }
    public Report check(Color fg, Color bg) {
        Objects.requireNonNull(fg, "Foreground is required.");
        Objects.requireNonNull(bg, "Background is required.");
        double ratio = calculator.calculate(fg, bg);
        List<RuleResult> results = rules.stream()
                .flatMap(rule -> Arrays.stream(TextSize.values())
                        .map(size -> rule.evaluate(ratio, size))).toList();
        return new Report(fg, bg, ratio, results);
    }
    public AccessibilityRule ruleFor(Level level) {
        Objects.requireNonNull(level, "Level is required.");
        return rules.stream().filter(rule -> rule.level() == level).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No rule for " + level));
    }
public record RuleResult(String ruleName, TextSize size, double requiredRatio, boolean passed) {
    public RuleResult {
        Objects.requireNonNull(ruleName, "Rule name is required.");
        Objects.requireNonNull(size, "Text size is required.");
        if (ruleName.isBlank() || !Double.isFinite(requiredRatio)
                || (requiredRatio != -1 && (requiredRatio < 1 || requiredRatio > 21))) {
            throw new IllegalArgumentException("Invalid rule name or threshold.");
        }
        if (requiredRatio == -1 && passed) {
            throw new IllegalArgumentException("An inapplicable rule cannot pass.");
        }
    }
    public boolean applicable() {
        return requiredRatio != -1;
    }
    public String verdict() {
        return applicable() ? (passed ? "PASS" : "FAIL") : "N/A";
    }
}
public record Report(Color fg, Color bg, double ratio, List<RuleResult> results) {
    public Report {
        Objects.requireNonNull(fg, "Foreground is required.");
        Objects.requireNonNull(bg, "Background is required.");
        if (!Double.isFinite(ratio) || ratio < 1 || ratio > 21) {
            throw new IllegalArgumentException("Contrast ratio must be between 1 and 21.");
        }
        results = List.copyOf(Objects.requireNonNull(results, "Results are required."));
        if (results.isEmpty()) {
            throw new IllegalArgumentException("At least one rule result is required.");
        }
    }
    public String verdict() {
        long applicable = results.stream().filter(RuleResult::applicable).count();
        long passed = results.stream().filter(RuleResult::applicable)
                .filter(RuleResult::passed).count();
        return passed + " of " + applicable + " applicable contrast checks pass.";
    }
    public RuleResult selected(AccessibilityRule rule, TextSize size) { return rule.evaluate(ratio, size); }
    public String verdict(AccessibilityRule rule, TextSize size) {
        RuleResult focus = selected(rule, size);
        String level = rule.level() == Level.AA ? "Standard (AA)" : "Strict (AAA)";
        if (!focus.applicable()) return "N/A: no Strict (AAA) target is defined for buttons or icons. Choose Standard (AA).";
        return (focus.passed() ? "Good: this combination passes " : "Needs improvement: this combination fails ")
                + "the " + level + " level for " + size.label() + ".";
    }
    public String tier() {
        if (ratio >= 7) return "EXCELLENT - passes every level";
        if (ratio >= 4.5) return "GOOD - passes AA for normal text";
        if (ratio >= 3) return "LIMITED - only OK for large text and UI elements";
        return "POOR - fails all text requirements";
    }
}
}