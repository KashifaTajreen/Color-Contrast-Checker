package contrast;
import java.util.*;
import contrast.ContrastChecker.RuleResult;
/** Enums and abstract class with inheritance keep thresholds in concrete rules. */
public final class Rules {
 private Rules() {}
public enum Level { AA, AAA }
public enum TextSize {
    NORMAL("Normal text"), LARGE("Large text"), UI_COMPONENT("UI component");
    private final String label;
    TextSize(String label) {
        this.label = label;
    }
    public String label() {
        return label;
    }
}
public static abstract class AccessibilityRule {
    protected static final double NOT_APPLICABLE = -1.0;
    private final Level level;
    private final String name;
    protected AccessibilityRule(Level level, String name) {
        this.level = Objects.requireNonNull(level, "Level is required.");
        this.name = Objects.requireNonNull(name, "Name is required.");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be blank.");
        }
    }
    public final Level level() {
        return level;
    }
    public final String name() {
        return name;
    }
    public abstract double thresholdFor(TextSize size);
    public final RuleResult evaluate(double ratio, TextSize size) {
        Objects.requireNonNull(size, "Text size is required.");
        if (!Double.isFinite(ratio) || ratio < 1 || ratio > 21) {
            throw new IllegalArgumentException("Ratio must be between 1 and 21.");
        }
        double threshold = thresholdFor(size);
        return new RuleResult(name, size, threshold,
                threshold != NOT_APPLICABLE && ratio >= threshold);
    }
}
public static final class AARule extends AccessibilityRule {
    private static final double NORMAL_THRESHOLD = 4.5;
    private static final double OTHER_THRESHOLD = 3.0;
    public AARule() {
        super(Level.AA, "WCAG AA");
    }
    @Override
    public double thresholdFor(TextSize size) {
        return Objects.requireNonNull(size, "Text size is required.") == TextSize.NORMAL
                ? NORMAL_THRESHOLD : OTHER_THRESHOLD;
    }
}
public static final class AAARule extends AccessibilityRule {
    private static final double NORMAL_THRESHOLD = 7.0;
    private static final double LARGE_THRESHOLD = 4.5;
    public AAARule() {
        super(Level.AAA, "WCAG AAA");
    }
    @Override
    public double thresholdFor(TextSize size) {
        return switch (Objects.requireNonNull(size, "Text size is required.")) {
            case NORMAL -> NORMAL_THRESHOLD;
            case LARGE -> LARGE_THRESHOLD;
            case UI_COMPONENT -> NOT_APPLICABLE;
        };
    }
}
}
