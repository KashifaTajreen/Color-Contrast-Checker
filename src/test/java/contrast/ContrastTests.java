package contrast;
import contrast.ColorParser.*;
import contrast.Rules.*;
import contrast.ContrastChecker.*;
import contrast.ContrastCalculator.WCAG2;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;
/** Nested groups retain the original eight test suites and their cases. */
class ContrastTests {
    @Nested
    class AAARuleTest {
    private final AAARule rule = new AAARule();

    @Test
    void normalTextRequiresSeven() {
        assertTrue(rule.evaluate(7.0, TextSize.NORMAL).passed());
        assertFalse(rule.evaluate(6.99, TextSize.NORMAL).passed());
    }

    @Test
    void largeTextRequiresFourPointFive() {
        assertTrue(rule.evaluate(4.5, TextSize.LARGE).passed());
        assertFalse(rule.evaluate(4.49, TextSize.LARGE).passed());
    }

    @Test
    void uiComponentsAreNotApplicable() {
        RuleResult result = rule.evaluate(21, TextSize.UI_COMPONENT);
        assertEquals(-1.0, result.requiredRatio());
        assertFalse(result.applicable());
        assertFalse(result.passed());
        assertEquals("N/A", result.verdict());
    }
}

    @Nested
    class AARuleTest {
    private final AARule rule = new AARule();

    @Test
    void normalTextUsesAnInclusiveBoundary() {
        assertTrue(rule.evaluate(4.5, TextSize.NORMAL).passed());
        assertFalse(rule.evaluate(4.49, TextSize.NORMAL).passed());
        assertFalse(rule.evaluate(4.4999, TextSize.NORMAL).passed());
    }

    @Test
    void largeTextAndComponentsRequireThree() {
        for (TextSize size : List.of(TextSize.LARGE, TextSize.UI_COMPONENT)) {
            assertEquals(3.0, rule.thresholdFor(size));
            assertTrue(rule.evaluate(3.0, size).passed());
            assertFalse(rule.evaluate(2.99, size).passed());
        }
    }

    @Test
    void checkerEvaluatesEverySizeAndReturnsImmutableResults() throws Exception {
        ContrastChecker checker = new ContrastChecker(new WCAG2(), List.of(rule));
        Report result = checker.check(Color.fromHex("#000"), Color.fromHex("#FFF"));
        assertEquals(3, result.results().size());
        assertTrue(result.results().stream().allMatch(RuleResult::passed));
        assertThrows(UnsupportedOperationException.class, () -> result.results().clear());
    }

    @Test
    void invalidEvaluationInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> rule.evaluate(Double.NaN, TextSize.NORMAL));
        assertThrows(NullPointerException.class, () -> rule.thresholdFor(null));
    }
}

    @Nested
    class ColorParserFactoryTest {
    

    @Test
    void selectsAppropriateStrategies() throws Exception {
        assertInstanceOf(Hex.class, ColorParser.select("#ABC", List.of(new Hex(), new Rgb(), new Named())));
        assertInstanceOf(Rgb.class, ColorParser.select("1,2,3", List.of(new Hex(), new Rgb(), new Named())));
        assertInstanceOf(Named.class, ColorParser.select("navy", List.of(new Hex(), new Rgb(), new Named())));
        assertEquals("#000080", ColorParser.parseAny("navy").toHex());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bluee", "hsl(0,100%,50%)"})
    void unknownInputThrowsHelpfulException(String input) {
        InvalidColorException exception = assertThrows(
                InvalidColorException.class, () -> ColorParser.parseAny(input));
        assertTrue(exception.getMessage().contains("Supported formats"));
    }

    @Test
    void firstSupportingStrategyWins() throws Exception {
        ColorParser custom = new Hex() {
            @Override
            public boolean supports(String input) {
                return "navy".equals(input);
            }

            @Override
            public Color parse(String input) throws InvalidColorException {
                return Color.fromRGB(1, 2, 3);
            }
        };
        List<ColorParser> extended = List.of(custom, new Named());
        assertSame(custom, ColorParser.select("navy", extended));
        assertEquals("#010203", ColorParser.select("navy", extended).parse("navy").toHex());
    }

    @Test
    void malformedRecognizedFormatsThrowCheckedExceptions() {
        assertThrows(InvalidColorException.class, () -> ColorParser.parseAny("#GG1234"));
        assertThrows(InvalidColorException.class, () -> ColorParser.parseAny("rgb(300,0,0)"));
    }
}

    @Nested
    class ColorSuggesterTest {
    private final WCAG2 calculator = new WCAG2();
    private final ColorSuggester suggester = new ColorSuggester(calculator);

    @Test
    void darkensFailingGrayToNearestPassingSample() throws Exception {
        Color foreground = Color.fromHex("#777777"), background = Color.fromHex("#FFF");
        Color suggestion = suggester.suggest(foreground, background, 4.5).orElseThrow();
        assertEquals("#767676", suggestion.toHex());
        assertTrue(calculator.calculate(suggestion, background) >= 4.5);
    }

    @Test
    void lightensForegroundAgainstBlack() throws Exception {
        Color background = Color.fromHex("#000");
        Color suggestion = suggester.suggest(Color.fromHex("#222"), background, 7).orElseThrow();
        assertTrue(calculator.calculate(suggestion, background) >= 7);
        assertTrue(suggestion.r() > 34);
    }

    @Test
    void bothDirectionsCanProducePassingAlternatives() throws Exception {
        Color gray = Color.fromHex("#777");
        List<Color> alternatives = suggester.suggestBothDirections(gray, gray, 3);
        assertEquals(2, alternatives.size());
        assertTrue(alternatives.stream().allMatch(c -> calculator.calculate(c, gray) >= 3));
        assertTrue(alternatives.stream().anyMatch(c -> c.r() < 119));
        assertTrue(alternatives.stream().anyMatch(c -> c.r() > 119));
    }

    @Test
    void coloredForegroundKeepsItsHslHueAlongThePath() throws Exception {
        Color white = Color.fromHex("#FFF");
        Color suggestion = suggester.suggest(Color.fromHex("#F00"), white, 4.5).orElseThrow();
        assertTrue(calculator.calculate(suggestion, white) >= 4.5);
        assertTrue(suggestion.r() > 0);
        assertEquals(0, suggestion.g());
        assertEquals(0, suggestion.b());
    }

    @Test
    void impossibleTargetReturnsEmpty() throws Exception {
        Color gray = Color.fromHex("#777");
        assertTrue(suggester.suggest(gray, gray, 21).isEmpty());
    }

    @Test
    void alreadyPassingForegroundIsRetained() throws Exception {
        Color black = Color.fromHex("#000");
        assertEquals(black, suggester.suggest(black, Color.fromHex("#FFF"), 7).orElseThrow());
    }

    @Test
    void validatesTargetsAndNullColors() throws Exception {
        Color gray = Color.fromHex("#777");
        assertThrows(IllegalArgumentException.class, () -> suggester.suggest(gray, gray, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> suggester.suggest(gray, gray, 22));
        assertThrows(NullPointerException.class, () -> suggester.suggest(null, gray, 4.5));
    }
}

    @Nested
    class HexTest {
    private final Hex parser = new Hex();

    @Test
    void expandsShortHexAndIgnoresCase() throws Exception {
        assertEquals("#AABBCC", parser.parse(" #aBc ").toHex());
        assertEquals("#AABBCC", parser.parse("abc").toHex());
        assertEquals("#AABBCC", parser.parse("aAbBcC").toHex());
    }

    @Test
    void equalColorsHaveEqualHashes() throws Exception {
        Color first = parser.parse("#123456"), second = Color.fromRGB(18, 52, 86);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals("#123456 rgb(18, 52, 86)", first.toString());
        assertNotEquals(first, parser.parse("#654321"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "#GG1234", "#12", "##ABC", "#12345678"})
    void invalidHexThrowsCheckedException(String input) {
        assertThrows(InvalidColorException.class, () -> parser.parse(input));
    }

    @Test
    void outOfRangeChannelsAreRejected() {
        assertThrows(InvalidColorException.class, () -> Color.fromRGB(-1, 0, 0));
        assertThrows(InvalidColorException.class, () -> Color.fromRGB(0, 256, 0));
        assertThrows(IllegalArgumentException.class, () -> new Color(0, 0, 300));
    }

    @Test
    void supportsHandlesNullAndEmpty() {
        assertFalse(parser.supports(null));
        assertFalse(parser.supports(""));
        assertTrue(parser.supports("#GG1234"));
    }
}

    @Nested
    class NamedTest {
    private final Named parser = new Named();

    @ParameterizedTest
    @CsvSource({"black,#000000", "white,#FFFFFF", "red,#FF0000", "green,#008000",
            "blue,#0000FF", "yellow,#FFFF00", "gray,#808080", "orange,#FFA500",
            "purple,#800080", "navy,#000080"})
    void resolvesRequiredNames(String name, String expected) throws Exception {
        assertEquals(expected, parser.parse(name).toHex());
    }

    @Test
    void lookupIsCaseInsensitive() throws Exception {
        assertTrue(parser.supports(" NaVy "));
        assertEquals("#000080", parser.parse(" NaVy ").toHex());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "bluee", "not-a-color"})
    void rejectsUnknownNames(String input) {
        assertFalse(parser.supports(input));
        assertThrows(InvalidColorException.class, () -> parser.parse(input));
    }
}

    @Nested
    class RgbTest {
    private final Rgb parser = new Rgb();

    @ParameterizedTest
    @ValueSource(strings = {"rgb(255, 87, 51)", "255,87,51", " RGB ( 255 , 87 , 51 ) "})
    void parsesSupportedForms(String input) throws Exception {
        assertEquals("#FF5733", parser.parse(input).toHex());
        assertTrue(parser.supports(input));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "rgb(300,0,0)", "rgb(-1,0,0)", "rgb(1,2)", "1,2,3,4",
            "rgb(1.5,2,3)", "rgb(999999999999999,0,0)", "rgb(1,2,3", "1,2,3)",
            "(1,2,3)", "#FFFFFF"})
    void rejectsInvalidRgb(String input) {
        assertThrows(InvalidColorException.class, () -> parser.parse(input));
    }

    @Test
    void supportsHandlesNullAndEmpty() {
        assertFalse(parser.supports(null));
        assertFalse(parser.supports(""));
    }
}

    @Nested
    class WCAG2Test {
    private final WCAG2 calculator = new WCAG2();

    @Test
    void blackOnWhiteHasMaximumContrast() throws Exception {
        assertEquals(21.0, calculator.calculate(
                Color.fromHex("#000000"), Color.fromHex("#FFFFFF")), 0.000001);
    }

    @Test
    void identicalColorsHaveMinimumContrast() throws Exception {
        Color white = Color.fromHex("#FFFFFF");
        assertEquals(1.0, calculator.calculate(white, white), 0.000001);
    }

    @Test
    void gray777FailsAaNormal() throws Exception {
        double ratio = calculator.calculate(Color.fromHex("#777777"), Color.fromHex("#FFF"));
        assertEquals(4.48, ratio, 0.01);
        assertFalse(new AARule().evaluate(ratio, TextSize.NORMAL).passed());
    }

    @Test
    void gray767PassesAaNormal() throws Exception {
        double ratio = calculator.calculate(Color.fromHex("#767676"), Color.fromHex("#FFF"));
        assertEquals(4.54, ratio, 0.01);
        assertTrue(new AARule().evaluate(ratio, TextSize.NORMAL).passed());
    }

    @Test
    void orderDoesNotChangeContrast() throws Exception {
        Color first = Color.fromHex("#123456"), second = Color.fromHex("#ABCDEF");
        assertEquals(calculator.calculate(first, second),
                calculator.calculate(second, first), 0.000001);
    }

    @Test
    void luminanceEndpointsAreZeroAndOne() throws Exception {
        assertEquals(0, calculator.relativeLuminance(Color.fromHex("#000")), 0.000001);
        assertEquals(1, calculator.relativeLuminance(Color.fromHex("#FFF")), 0.000001);
    }

    @Test
    void nullColorsAreRejected() throws Exception {
        Color white = Color.fromHex("#FFF");
        assertThrows(NullPointerException.class, () -> calculator.calculate(null, white));
        assertThrows(NullPointerException.class, () -> calculator.calculate(white, null));
    }
}

}
