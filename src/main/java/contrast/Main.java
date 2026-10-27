package contrast;
import java.util.List;
import java.util.Scanner;
/** Composition and dependency injection: wire the application in one place. */
public final class Main {
    public static void main(String[] args) {
        ContrastCalculator calculator = new ContrastCalculator.WCAG2();
        ContrastChecker checker = new ContrastChecker(calculator, List.of(new Rules.AARule(), new Rules.AAARule()));
        ColorSuggester suggester = new ColorSuggester(calculator);
        if (List.of(args).contains("--console")) {
            try (Scanner input = new Scanner(System.in)) {
                new ConsoleApp(input, System.out, checker, suggester, new Printer(System.out)).run();
            }
        } else ContrastApp.launch(checker, suggester);
    }
}
