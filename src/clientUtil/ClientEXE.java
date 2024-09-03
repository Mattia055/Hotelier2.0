package clientUtil;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.AbstractTerminal;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ClientEXE {
    private static final String RESET = "\033[0m";
    private static final String HIGHLIGHT = "\033[47;30m"; // Highlight with white background and black text

    private static final int ESC = 27;
    private static final int UP_ARROW = 'A';
    private static final int DOWN_ARROW = 'B';
    private static final int ENTER = 13;

    public static void main(String[] args) {
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Ensure the terminal is in raw mode
            terminal.enterRawMode();

            List<String> options = Arrays.asList("Option 1", "Option 2", "Option 3", "Exit");
            int selectedIndex = 0;

            // Display instructions
            terminal.writer().println("Use arrow keys to navigate and Enter to select.");
            terminal.writer().println("Press Ctrl+C to exit.");
            displayMenu(terminal, options, selectedIndex);
            while (true) {
                int key = readKey(terminal); // Read raw key input
                if (key == ESC) {
                    // Read the next two characters
                    if (readKey(terminal) == '[') {
                        key = readKey(terminal);
                        switch (key) {
                            case UP_ARROW:
                                selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
                                break;
                            case DOWN_ARROW:
                                selectedIndex = (selectedIndex + 1) % options.size();
                                break;
                        }
                    }
                } else if (key == ENTER) {
                    if (selectedIndex == options.size() - 1) {
                        System.out.println("Exiting...");
                        return;
                    } else {
                        System.out.println("You selected: " + options.get(selectedIndex));
                    }
                } else if (key == 3) { // Ctrl+C
                    System.out.println("Interrupted. Exiting...");
                    return;
                }

                displayMenu(terminal, options, selectedIndex); // Update the menu display
            }
        } catch (UserInterruptException e) {
            System.out.println(RESET + "Interrupted. Exiting...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to display the menu
    private static void displayMenu(Terminal terminal, List<String> options, int selectedIndex) throws IOException {
        terminal.writer().print(RESET + "\033[H\033[J"); // Clear screen
        for (int i = 0; i < options.size(); i++) {
            if (i == selectedIndex) {
                terminal.writer().println(HIGHLIGHT + options.get(i) + RESET);
            } else {
                terminal.writer().println(options.get(i));
            }
        }
        terminal.writer().flush();
    }

    // Method to read a single key press from the terminal
    private static int readKey(Terminal terminal) throws IOException {
        return terminal.reader().read(); // Read a single character from terminal
    }
}