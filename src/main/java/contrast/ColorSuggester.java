package contrast;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
/** Uses loops and a list to search for a readable foreground color. */
public final class ColorSuggester {
    private static final double CHANNEL_MAX = 255.0;
    private static final int SEARCH_STEPS = 65_536;
    private static final int HUE_SECTORS = 6;
    private final ContrastCalculator calculator;
    public ColorSuggester(ContrastCalculator calculator) {
        this.calculator = Objects.requireNonNull(calculator, "Calculator is required.");
    }
    public Color suggest(Color fg, Color bg, double target) {
        validate(fg, bg, target);
        if (calculator.calculate(fg, bg) >= target) {
            return fg;
        }
        Hsl original = toHsl(fg);
        Color best = null;
        double smallestChange = Double.MAX_VALUE;
        for (Color color : suggestBothDirections(fg, bg, target)) {
            double change = Math.abs(toHsl(color).lightness - original.lightness);
            if (change < smallestChange) {
                best = color;
                smallestChange = change;
            }
        }
        return best;
    }
    public List<Color> suggestBothDirections(Color fg, Color bg, double target) {
        validate(fg, bg, target);
        Hsl hsl = toHsl(fg);
        List<Color> colors = new ArrayList<>();
        Color darker = search(hsl, bg, target, 0);
        Color lighter = search(hsl, bg, target, 1);
        if (darker != null) colors.add(darker);
        if (lighter != null && !colors.contains(lighter)) colors.add(lighter);
        return colors;
    }
    private Color search(Hsl hsl, Color bg, double target, double endpoint) {
        // Hold hue and saturation fixed; move outward in small lightness steps.
        // Check rounded RGB, because that is the color the user will actually use.
        Color previous = null;
        for (int step = 0; step <= SEARCH_STEPS; step++) {
            double fraction = (double) step / SEARCH_STEPS;
            double lightness = hsl.lightness + (endpoint - hsl.lightness) * fraction;
            Color candidate = fromHsl(hsl.hue, hsl.saturation, lightness);
            if (!candidate.equals(previous) && calculator.calculate(candidate, bg) >= target) {
                return candidate;
            }
            previous = candidate;
        }
        return null;
    }
    private Hsl toHsl(Color color) {
        double r = color.r() / CHANNEL_MAX, g = color.g() / CHANNEL_MAX;
        double b = color.b() / CHANNEL_MAX;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double delta = max - min, lightness = (max + min) / 2;
        if (delta == 0) {
            return new Hsl(0, 0, lightness);
        }
        double saturation = delta / (1 - Math.abs(2 * lightness - 1));
        double sector;
        if (max == r) {
            sector = ((g - b) / delta) % HUE_SECTORS;
        } else if (max == g) {
            sector = (b - r) / delta + 2;
        } else {
            sector = (r - g) / delta + 4;
        }
        return new Hsl((sector + HUE_SECTORS) % HUE_SECTORS, saturation, lightness);
    }
    private Color fromHsl(double hue, double saturation, double lightness) {
        double chroma = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double secondary = chroma * (1 - Math.abs(hue % 2 - 1));
        double offset = lightness - chroma / 2;
        double[] channels;
        switch ((int) hue) {
            case 0: channels = new double[] {chroma, secondary, 0}; break;
            case 1: channels = new double[] {secondary, chroma, 0}; break;
            case 2: channels = new double[] {0, chroma, secondary}; break;
            case 3: channels = new double[] {0, secondary, chroma}; break;
            case 4: channels = new double[] {secondary, 0, chroma}; break;
            default: channels = new double[] {chroma, 0, secondary};
        }
        return rgb(channel(channels[0] + offset), channel(channels[1] + offset),
                channel(channels[2] + offset));
    }
    private int channel(double value) {
        return (int) Math.round(Math.max(0, Math.min(1, value)) * CHANNEL_MAX);
    }
    private Color rgb(int r, int g, int b) {
        try {
            return Color.fromRGB(r, g, b);
        } catch (InvalidColorException exception) {
            throw new IllegalStateException("HSL conversion produced invalid RGB.", exception);
        }
    }
    private void validate(Color fg, Color bg, double target) {
        Objects.requireNonNull(fg, "Foreground is required.");
        Objects.requireNonNull(bg, "Background is required.");
        if (!Double.isFinite(target) || target < 1 || target > 21) {
            throw new IllegalArgumentException("Target ratio must be between 1 and 21.");
        }
    }
    private static final class Hsl {
        final double hue;
        final double saturation;
        final double lightness;
        Hsl(double hue, double saturation, double lightness) {
            this.hue = hue;
            this.saturation = saturation;
            this.lightness = lightness;
        }
    }
}
