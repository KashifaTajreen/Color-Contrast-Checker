# Color Contrast Checker

A small Java 21 application that checks text and background colors with the WCAG 2 contrast formula. It accepts `#RGB`, `#RRGGBB`, bare hex, `rgb(...)`, bare RGB channels, and ten common color names.

## Run

```powershell
./run.ps1             # graphical window
./run.ps1 -Console    # console menu
```

The window updates while you type, previews the colors, shows all AA/AAA checks, and suggests a foreground color when the selected target fails. The console presents the same calculations in a structured report. ANSI color can be disabled by changing `Printer.USE_COLOR` to `false`.

## Structure and OOP concepts

| File | Responsibility and concepts |
|---|---|
| `Color.java` | Immutable record, encapsulation, factories |
| `InvalidColorException.java` | Custom checked exception |
| `ColorParser.java` | Interface, `ArrayList`, `HashMap`, loops |
| `ContrastCalculator.java` | Interface, WCAG2 strategy, polymorphism |
| `Rules.java` | Enums, abstract class, inheritance |
| `ContrastChecker.java` | Dependency injection, records, lists and loops |
| `ColorSuggester.java` | Arrays, `ArrayList`, loops and HSL search |
| `Printer.java` | Console presentation |
| `ConsoleApp.java` | Recoverable menu loop |
| `ContrastApp.java` | Swing presentation and live listeners |
| `Main.java` | Composition root and launcher |

The formula and rule thresholds live outside both user interfaces. `Main` opens the Swing window by default and selects the console only for `--console`.
