package contrast;
import java.util.Locale;
/** Immutability and encapsulation: a record owns validated RGB channels. */
public record Color(int r, int g, int b) {
    private static final int HEX_RADIX = 16;
    public Color {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255)
            throw new IllegalArgumentException("RGB channels must be between 0 and 255.");
    }
    // Record constructors cannot declare checked exceptions; input uses this factory.
    public static Color fromRGB(int r, int g, int b) throws InvalidColorException {
        for (int channel : new int[] {r, g, b}) {
            if (channel < 0 || channel > 255)
                throw new InvalidColorException("RGB channels must be between 0 and 255; received " + channel + ".");
        }
        return new Color(r, g, b);
    }
    /** Parses three or six hex digits, optionally prefixed by '#'. */
    public static Color fromHex(String input) throws InvalidColorException {
        if (input == null) {
            throw new InvalidColorException("Hex color cannot be null.");
        }
        String hex = input.trim().replaceFirst("^#", "");
        if (!hex.matches("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}")) {
            throw new InvalidColorException("Use #RGB or #RRGGBB, for example #3AF.");
        }
        if (hex.length() == 3) {
            hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1)
                    + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        return fromRGB(Integer.parseInt(hex.substring(0, 2), HEX_RADIX),
                Integer.parseInt(hex.substring(2, 4), HEX_RADIX),
                Integer.parseInt(hex.substring(4, 6), HEX_RADIX));
    }


    public String toHex() { return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b); }
    public String toRgbString() { return "rgb(" + r + ", " + g + ", " + b + ")"; }
    @Override public String toString() { return toHex() + " " + toRgbString(); }
}
