package contrast;
import java.util.*;
import java.util.regex.*;
/** Sealed interface, polymorphism and Strategy: each nested parser owns one syntax. */
public sealed interface ColorParser permits ColorParser.Hex, ColorParser.Rgb, ColorParser.Named {
    boolean supports(String input);
    Color parse(String input) throws InvalidColorException;
    String HELP = "Supported formats: #RGB, #RRGGBB (also without #), "
            + "rgb(255, 87, 51), 255,87,51, or a name such as navy.";
    static ColorParser select(String input, List<ColorParser> parsers) throws InvalidColorException {
        if (input == null || input.isBlank())
            throw new InvalidColorException("Color cannot be null or empty. " + HELP);
        for (ColorParser parser : parsers) if (parser.supports(input)) return parser;
        throw new InvalidColorException("Unrecognized color. " + HELP);
    }
    static Color parseAny(String input) throws InvalidColorException {
        return select(input, List.of(new Hex(), new Rgb(), new Named())).parse(input);
    }
public static non-sealed class Hex implements ColorParser {
    @Override
    public boolean supports(String input) {
        if (input == null) {
            return false;
        }
        String value = input.trim();
        return value.startsWith("#") || value.matches("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}");
    }
    @Override
    public Color parse(String input) throws InvalidColorException {
        return Color.fromHex(input);
    }
}
public static final class Rgb implements ColorParser {
    private static final String CHANNELS =
            "\\s*([+-]?\\d+)\\s*,\\s*([+-]?\\d+)\\s*,\\s*([+-]?\\d+)\\s*";
    private static final Pattern WRAPPED = Pattern.compile(
            "rgb\\s*\\(" + CHANNELS + "\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BARE = Pattern.compile(CHANNELS);
    @Override
    public boolean supports(String input) {
        if (input == null) return false;
        String value = input.trim();
        return value.regionMatches(true, 0, "rgb", 0, 3) || BARE.matcher(value).matches();
    }
    @Override
    public Color parse(String input) throws InvalidColorException {
        if (input == null || input.isBlank()) {
            throw new InvalidColorException("RGB color cannot be null or empty.");
        }
        Matcher matcher = WRAPPED.matcher(input.trim());
        if (!matcher.matches()) {
            matcher = BARE.matcher(input.trim());
            if (!matcher.matches()) {
                throw new InvalidColorException("Use rgb(255, 87, 51) or 255,87,51.");
            }
        }
        return parseChannels(matcher);
    }
    private Color parseChannels(Matcher matcher) throws InvalidColorException {
        try {
            return Color.fromRGB(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        } catch (NumberFormatException exception) {
            throw new InvalidColorException("RGB channels must be integers from 0 to 255.");
        }
    }
}
public static final class Named implements ColorParser {
    private final Map<String, Color> colors;
    public Named() {
        colors = createColors();
    }
    @Override
    public boolean supports(String input) {
        return input != null && colors.containsKey(input.trim().toLowerCase(Locale.ROOT));
    }
    @Override
    public Color parse(String input) throws InvalidColorException {
        if (!supports(input)) {
            throw new InvalidColorException("Unknown name. Try black, white, red, green, "
                    + "blue, yellow, gray, orange, purple, or navy.");
        }
        return colors.get(input.trim().toLowerCase(Locale.ROOT));
    }
    private Map<String, Color> createColors() {
        return Map.ofEntries(
                Map.entry("black", constant("#000000")),
                Map.entry("white", constant("#FFFFFF")),
                Map.entry("red", constant("#FF0000")),
                Map.entry("green", constant("#008000")),
                Map.entry("blue", constant("#0000FF")),
                Map.entry("yellow", constant("#FFFF00")),
                Map.entry("gray", constant("#808080")),
                Map.entry("orange", constant("#FFA500")),
                Map.entry("purple", constant("#800080")),
                Map.entry("navy", constant("#000080")));
    }
    private Color constant(String hex) {
        try {
            return Color.fromHex(hex);
        } catch (InvalidColorException exception) {
            throw new IllegalStateException("Invalid built-in color: " + hex, exception);
        }
    }
}
}
