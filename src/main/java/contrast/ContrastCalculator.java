package contrast;
import java.util.Objects;
/** Interface, polymorphism and Strategy keep calculation interchangeable. */
public interface ContrastCalculator {
 double calculate(Color fg, Color bg);
public static final class WCAG2 implements ContrastCalculator {
    private static final double CHANNEL_MAX = 255.0;
    private static final double LINEAR_BREAKPOINT = 0.03928;
    private static final double LINEAR_DIVISOR = 12.92;
    private static final double GAMMA_OFFSET = 0.055;
    private static final double GAMMA_SCALE = 1.055;
    private static final double GAMMA_EXPONENT = 2.4;
    private static final double RED_WEIGHT = 0.2126;
    private static final double GREEN_WEIGHT = 0.7152;
    private static final double BLUE_WEIGHT = 0.0722;
    private static final double LUMINANCE_OFFSET = 0.05;

    /** Calculates contrast from the lighter and darker relative luminances. */
    @Override
    public double calculate(Color fg, Color bg) {
        double foreground = relativeLuminance(fg);
        double background = relativeLuminance(bg);
        // The offset keeps black's zero luminance safe in the denominator.
        return (Math.max(foreground, background) + LUMINANCE_OFFSET)
                / (Math.min(foreground, background) + LUMINANCE_OFFSET);
    }

    /** Returns perceived brightness on a scale from zero to one. */
    public double relativeLuminance(Color color) {
        Objects.requireNonNull(color, "Color is required.");
        // Human vision is most sensitive to green and least sensitive to blue.
        return RED_WEIGHT * linearize(color.r()) + GREEN_WEIGHT * linearize(color.g())
                + BLUE_WEIGHT * linearize(color.b());
    }

    private double linearize(int channel) {
        // Normalize the integer channel to a number between zero and one.
        double normalized = channel / CHANNEL_MAX;
        // Remove sRGB gamma before combining the channels as brightness.
        return normalized <= LINEAR_BREAKPOINT ? normalized / LINEAR_DIVISOR
                : Math.pow((normalized + GAMMA_OFFSET) / GAMMA_SCALE, GAMMA_EXPONENT);
    }
}

}