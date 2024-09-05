package lib.client.cli;

import java.io.IOException;
import java.io.PrintWriter;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import lib.client.api.APIResponse;
import lib.client.api.HotelierAPI;
import lib.client.api.Status;

public class CliHandler implements Runnable{

    protected static CliHandler instance = null;
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

    private CliHandler(){
        try{
        terminal = TerminalBuilder.builder().system(true).build();
        } catch(IOException e){
            e.printStackTrace();
            System.exit(-1);
        }

        //binding delle chiavi
        keyMap = new KeyMap<>();
            keyMap.bind("UP", "\033[A");
            keyMap.bind("DOWN", "\033[B");
            keyMap.bind("RIGHT", "\033[C");
            keyMap.bind("LEFT", "\033[D");
            keyMap.bind("ENTER", "\r");
            keyMap.bind("TAB", "\t");
            keyMap.bind("SPACE", " ");
            keyMap.bind("BACKSPACE", "\177");
            for(char c = 'a'; c <= 'z'; c++){keyMap.bind(String.valueOf(c), String.valueOf(c));}
            for(char c = 'A'; c <= 'Z'; c++){keyMap.bind(String.valueOf(c), String.valueOf(c));}
            for (char c = '0'; c <= '9'; c++) {keyMap.bind(String.valueOf(c), String.valueOf(c));}
            String[] punctuation = { ".", ",", ";", ":", "!", "?", "-", "_", "(", ")", "[", "]", "{", "}", "'", "\"", "/", "\\", "@", "#", "$", "%", "^", "&", "*", "+", "=", "<", ">", "|", "~", "`" };
            for (String p : punctuation) {keyMap.bind(p, p);}
    
            
        terminal.enterRawMode();
        bindingReader = new BindingReader(terminal.reader());
        writer = terminal.writer();
        writer.print(Ansi.HIDE_CURSOR);
        OptionSet = Option.base();
    }

    public static CliHandler getInstance(){
        if(instance == null){
            instance = new CliHandler();
        }
        return instance;
    }

    public static void setEntryPoint(HotelierAPI entry){
        EntryPoint = entry;
    }

    private static void navigateMenu(PrintWriter writer, Option[] options, int direction){
        index = (index + direction + options.length) % options.length;
        displayMenu(writer, options, index);
    }

    // Method to display the menu
    private static void displayMenu(PrintWriter terminal, Option[] options, int selectedIndex) {
        terminal.print(Ansi.RESET + Ansi.CLEAR); // Clear screen
        terminal.println("Use 'u' to navigate up, 'd' to navigate down, and Enter to select an option. Press 'q' to exit.\n\n");
        for (int i = 0; i < options.length; i++) {
            if (i == selectedIndex)
                terminal.println(Ansi.HIGHLIGHT + " "+ options[i].description() + " " + Ansi.RESET + "\n");
            else 
                terminal.println(options[i].description() + "\n");
        }
        terminal.println(LastOpMessage);
        LastOpMessage = "";
        terminal.flush();
    }
    @Override
    public void run(){
        OptionHandler.getInstance();
        // Event loop
        while (running) {


            displayMenu(writer, OptionSet, index); // Display menu
            String character = bindingReader.readBinding(keyMap); // Read key and get associated action
            switch(character){
                
                case "UP":case "w": navigateMenu(writer, OptionSet, -1); break;
                case "DOWN":case "s":case "TAB": navigateMenu(writer, OptionSet, 1); break;
                case "ENTER":case "SPACE":{
                    if(OptionSet[index] == Option.EXIT){
                        running = false;
                        break;
                    }

                    if(OptionSet[index] == Option.LOGOUT){
                        //chiamata API a Logout
                        try{
                            APIResponse response = EntryPoint.UserLogout();
                            if(response.getStatus() == Status.OK){
                                LastOpMessage = Ansi.GREEN + "Logged out successfully" + Ansi.RESET;
                                OptionSet = Option.base();
                                index = 0;
                            } else{
                                HandleUnauthorized();
                                break;
                            }
                        } catch(Exception e){
                            TerminationMessage = Ansi.RED + "An error occurred while logging out" + Ansi.RESET;
                            System.exit(-1);
                        }
                    } else if(OptionSet[index] == Option.BADGE){
                        try{
                            APIResponse response = EntryPoint.ShowUserBadge();
                            if(response.getStatus() == Status.OK){
                                LastOpMessage = Ansi.CYAN + "User Badge: "+ response.getString() + Ansi.RESET;
                            } else if(response.getStatus() == Status.NOT_LOGGED){
                                HandleUnauthorized();
                                break;
                            } 
                            else {
                                LastOpMessage = Ansi.RED + "Error: " + response.getMessage() + Ansi.RESET;
                            }
                        } catch(Exception e){
                            TerminationMessage = Ansi.RED + "An error occurred while logging out" + Ansi.RESET;
                            System.exit(-1);
                        }
                    }
                    
                    else OptionHandler.handle(OptionSet[index]).run();
                    
                }
                
            }
        }
        terminate();
        
        
    }

    public static void terminate(){
        try{
            writer.print(Ansi.CLEAR+Ansi.RESET+Ansi.SHOW_CURSOR);
            writer.println("\n"+TerminationMessage+"\n");
            terminal.flush();
            terminal.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    protected static void clearScreen(){
        writer.print(Ansi.CLEAR);
        writer.flush();
    }

    protected static void reset(){
        writer.print(Ansi.RESET);
        writer.flush();
    }

    protected static void HandleUnauthorized(){
        LastOpMessage = Ansi.RED + "Please Login Again" + Ansi.RESET;
                                OptionSet = Option.base();
                                index = 0;
    }

    
}
