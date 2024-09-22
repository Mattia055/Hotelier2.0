package clientUtil;

import java.io.IOException;
import java.io.PrintWriter;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import lib.client.api.APIResponse;
import lib.client.api.HotelierAPI;
import lib.client.api.Status;
import lib.client.cli.Ansi;
import lib.client.cli.Option;

public class TuiHandler implements Runnable {

    protected static TuiHandler instance = null;
    protected static boolean running = true;
    protected static Option[] OptionSet;
    protected static BindingReader bindingReader;
    protected static PrintWriter writer;
    protected static Terminal terminal;
    protected static int index = 0;
    protected static KeyMap<String> keyMap = new KeyMap<>();
    protected static HotelierAPI EntryPoint;
    protected static String TerminationMessage = "Goodbye!";
    protected static String LastOpMessage = "";
    private static final String TITLE = "Hotelier Client Interface";

    private TuiHandler() {
        try {
            terminal = TerminalBuilder.builder().system(true).build();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        initializeKeyBindings();
        terminal.enterRawMode();
        bindingReader = new BindingReader(terminal.reader());
        writer = terminal.writer();
        OptionSet = Option.base();
    }

    public static TuiHandler getInstance() {
        if (instance == null) {
            instance = new TuiHandler();
        }
        return instance;
    }

    public static void setEntryPoint(HotelierAPI entry) {
        EntryPoint = entry;
    }

    private static void initializeKeyBindings() {
        keyMap.bind("UP",   "\033[A");
        keyMap.bind("DOWN", "\033[B");
        keyMap.bind("RIGHT","\033[C");
        keyMap.bind("LEFT", "\033[D");
        keyMap.bind("ENTER","\r");
        keyMap.bind("TAB",  "\t");
        keyMap.bind("SPACE"," ");
        keyMap.bind("BACKSPACE","\177");
        for (char c = 'a'; c <= 'z'; c++) {
            keyMap.bind(String.valueOf(c), String.valueOf(c));
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            keyMap.bind(String.valueOf(c), String.valueOf(c));
        }
        for (char c = '0'; c <= '9'; c++) {
            keyMap.bind(String.valueOf(c), String.valueOf(c));
        }
        String[] punctuation = { ".", ",", ";", ":", "!", "?", "-", "_", "(", ")", "[", "]", "{", "}", "'", "\"", "/", "\\", "@", "#", "$", "%", "^", "&", "*", "+", "=", "<", ">", "|", "~", "`" };
        for (String p : punctuation) {
            keyMap.bind(p, p);
        }
    }

    private static void navigateMenu(int direction) {
        index = (index + direction + OptionSet.length) % OptionSet.length;
        displayMenu();
    }

    // Method to build and display the menu using a StringBuilder
    private static void displayMenu() {
        StringBuilder menuBuffer = new StringBuilder();
        menuBuffer.append(Ansi.CLEAR)
                  .append(Ansi.CYAN).append("Welcome to ").append(TITLE).append(Ansi.RESET).append("\n\n")
                  .append("Use 'ARROWS' to navigate. Enter to select\n\n");

        for (int i = 0; i < OptionSet.length; i++) {
            if (i == index) {
                menuBuffer.append(Ansi.HIGHLIGHT).append(" ").append(OptionSet[i].description()).append(" ").append(Ansi.RESET).append("\n\n");
            } else {
                menuBuffer.append(OptionSet[i].description()).append("\n\n");
            }
        }

        menuBuffer.append("\n\n").append(LastOpMessage);
        LastOpMessage = ""; // Clear the last operation message after displaying

        String notification = ClientMain.fetchNotification();

        if (!notification.isEmpty()) {
            menuBuffer.append("\n\n").append(Ansi.YELLOW).append(notification).append(Ansi.RESET);
        }

        // Print the constructed menu
        writer.print(menuBuffer.toString());
        writer.flush();
    }

    @Override
    public void run() {
        OptionHandler.getInstance();
        // Event loop
        while (running) {
            displayMenu(); // Display menu
            String character = bindingReader.readBinding(keyMap); // Read key and get associated action
            switch (character) {
                case "UP": case "w":
                    navigateMenu(-1);
                    break;
                case "DOWN": case "s": case "TAB":
                    navigateMenu(1);
                    break;
                case "ENTER": case "SPACE":
                    handleSelection();
                    break;
            }
        }
        terminate();
    }

    private void handleSelection() {
        if (OptionSet[index] == Option.EXIT) {
            running = false;
        } else if (OptionSet[index] == Option.LOGOUT) {
            handleLogout();
        } else if (OptionSet[index] == Option.BADGE) {
            handleBadge();
        } else {
            OptionHandler.handle(OptionSet[index]).run();
        }
    }

    private void handleLogout() {
        try {
            APIResponse response = EntryPoint.UserLogout();
            if (response.getStatus() == Status.OK) {
                LastOpMessage = Ansi.GREEN + "Logged out successfully" + Ansi.RESET;
                OptionSet = Option.base();
                ClientMain.getUDPListener().stopUDPlistening();
                index = 0;
            } else {
                HandleUnauthorized();
            }
        } catch (Exception e) {
            TerminationMessage = Ansi.RED + "An error occurred while logging out" + Ansi.RESET;
            System.exit(-1);
        }
    }

    private void handleBadge() {
        try {
            APIResponse response = EntryPoint.ShowUserBadge();
            if (response.getStatus() == Status.OK) {
                LastOpMessage = Ansi.CYAN + "User Badge: " + response.getString() + Ansi.RESET;
            } else if (response.getStatus() == Status.NOT_LOGGED) {
                HandleUnauthorized();
            } else {
                LastOpMessage = Ansi.RED + "Error: " + response.getMessage() + Ansi.RESET;
            }
        } catch (Exception e) {
            TerminationMessage = Ansi.RED + "An error occurred while retrieving badge info" + Ansi.RESET;
            System.exit(-1);
        }
    }

    public static void terminate() {
        try {
            writer.print(Ansi.CLEAR + Ansi.RESET + Ansi.SHOW_CURSOR);
            writer.println("\n" + TerminationMessage + "\n");
            terminal.flush();
            terminal.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void HandleUnauthorized() {
        LastOpMessage = Ansi.RED + "Please Login Again" + Ansi.RESET;
        OptionSet = Option.base();
        index = 0;
    }

    
}
