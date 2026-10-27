package contrast;
import java.util.Arrays;
import java.util.Scanner;
/** Composition and dependency injection: wire the application in one place. */
public final class Main {
    public static void main(String[] args) {
        ContrastCalculator calculator = new ContrastCalculator.WCAG2();
        ContrastChecker checker = new ContrastChecker(calculator,
                Arrays.asList(new Rules.AARule(), new Rules.AAARule()));
        ColorSuggester suggester = new ColorSuggester(calculator);
        if (args.length > 0 && args[0].equals("--console")) {
            try (Scanner input = new Scanner(System.in)) {
                new ConsoleApp(input, System.out, checker, suggester, new Printer(System.out)).run();
            }
        } else ContrastApp.launch(checker, suggester);
    }
}
