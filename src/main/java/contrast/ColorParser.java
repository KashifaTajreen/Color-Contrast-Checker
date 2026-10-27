package contrast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.*;
/** A simple interface lets each parser handle one color format. */
public interface ColorParser {
    List<ColorParser> PARSERS = createParsers();
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
        return select(input, PARSERS).parse(input);
    }
    private static List<ColorParser> createParsers() {
        List<ColorParser> parsers = new ArrayList<>();
        parsers.add(new Hex());
        parsers.add(new Rgb());
        parsers.add(new Named());
        return parsers;
    }
public static class Hex implements ColorParser {
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
    private static final Map<String, Color> COLORS = createColors();
    private static Map<String, Color> createColors() {
        Map<String, Color> colors = new HashMap<>();
        colors.put("black", new Color(0, 0, 0));
        colors.put("white", new Color(255, 255, 255));
        colors.put("red", new Color(255, 0, 0));
        colors.put("green", new Color(0, 128, 0));
        colors.put("blue", new Color(0, 0, 255));
        colors.put("yellow", new Color(255, 255, 0));
        colors.put("gray", new Color(128, 128, 128));
        colors.put("orange", new Color(255, 165, 0));
        colors.put("purple", new Color(128, 0, 128));
        colors.put("navy", new Color(0, 0, 128));
        return colors;
    }
    @Override
    public boolean supports(String input) {
        return input != null && COLORS.containsKey(input.trim().toLowerCase(Locale.ROOT));
    }
    @Override
    public Color parse(String input) throws InvalidColorException {
        if (!supports(input)) {
            throw new InvalidColorException("Unknown name. Try black, white, red, green, "
                    + "blue, yellow, gray, orange, purple, or navy.");
        }
        return COLORS.get(input.trim().toLowerCase(Locale.ROOT));
    }
}
}
