package clientUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.function.Supplier;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Terminal;
import lib.client.api.APIResponse;
import lib.client.api.HotelierAPI;
import lib.client.api.Status;
import lib.client.cli.Ansi;
import lib.client.cli.Option;
import lib.share.struct.HotelDTO;
import lib.share.struct.Score;

public class OptionHandler {
    private static OptionHandler instance = null;
    private static Terminal terminal;
    private static PrintWriter writer;
    private static EnumMap<Option, Supplier<Runnable>> optionMap;
    private static KeyMap<String> keyMap;
    private static HotelierAPI entryPoint;

    private OptionHandler() {
        TuiHandler.getInstance();
        terminal = TuiHandler.terminal;
        writer = TuiHandler.writer;
        keyMap = TuiHandler.keyMap;
        entryPoint = TuiHandler.EntryPoint;
        initializeOptionMap();
    }

    public static OptionHandler getInstance() {
        if (instance == null) {
            instance = new OptionHandler();
        }
        return instance;
    }

    private void initializeOptionMap() {
        optionMap = new EnumMap<>(Option.class);
        optionMap.put(Option.REGISTER, Registration::new);
        optionMap.put(Option.LOGIN, Login::new);
        optionMap.put(Option.FULL_LOGOUT, FullLogout::new);
        optionMap.put(Option.SEARCH_H, SearchHotel::new);
        optionMap.put(Option.FETCH_H, FetchHotels::new);
        optionMap.put(Option.REVIEW, InsertReview::new);
    }

    public static Runnable handle(Option option) {
        return optionMap.get(option).get();
    }

    // Helper method to append menu entries
    private static void appendMenuEntry(StringBuilder buffer, String label, String value, boolean isHighlighted, boolean isButton) {
        String formattedValue = isButton ? "" : value;
        String highlightStart = isHighlighted ? Ansi.HIGHLIGHT + " " : "";
        String highlightEnd = isHighlighted ? " " + Ansi.RESET + (isButton ? "" : " :\t") : "\t\t";

        buffer.append(highlightStart)
              .append(label)
              .append(highlightEnd)
              .append(formattedValue);

        if (isHighlighted && !isButton) buffer.append("_");
        buffer.append("\n\n");
    }

    private static void appendMenuEntry(StringBuilder buffer, String label, String value, boolean isHighlighted) {
        appendMenuEntry(buffer, label, value, isHighlighted, false);
    }

    // Common method for deleting the last character
    protected static void deleteLastCharacter(StringBuilder input) {
        if (input.length() > 0) input.deleteCharAt(input.length() - 1);
    }

    // Common method for masking input
    protected static String maskInput(StringBuilder input) {
        StringBuilder maskedInput = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            maskedInput.append("*");
        }
        return maskedInput.toString();
    }

    // Method to convert stack trace to string
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    // Base class for menu operations
    private abstract static class BaseMenu implements Runnable {
        protected final Terminal terminal;
        protected final BindingReader reader;
        protected final PrintWriter writer;
        protected final StringBuilder menuBuffer;
        protected boolean returnToMenu;
        protected int index;
        protected String message;

        public BaseMenu() {
            TuiHandler.getInstance();
            terminal = OptionHandler.terminal;
            reader = TuiHandler.bindingReader;
            writer = OptionHandler.writer;
            menuBuffer = new StringBuilder();
            returnToMenu = false;
            index = 0;
            message = "";
        }

        protected void navigateMenu(int maxOptions, String key) {
            switch (key) {
                case "UP": index = (index - 1 + maxOptions) % maxOptions; break;
                case "DOWN": case "TAB": index = (index + 1) % maxOptions; break;
            }
        }

        protected void Terminate(String message) {
            TuiHandler.TerminationMessage = message;
            System.exit(-1);
        }

        protected void setMessageOnReturn(String message){
            TuiHandler.LastOpMessage = message;
        }

        protected void displayMenu() {
            String notification = ClientMain.fetchNotification();
            if (!notification.isEmpty()) {
                menuBuffer.append("\n\n").append(Ansi.YELLOW).append(notification).append(Ansi.RESET);
            }
            writer.print(Ansi.RESET + Ansi.CLEAR);
            writer.print(menuBuffer.toString());
            menuBuffer.setLength(0);
            terminal.flush();
        }

        protected void flipOptionSet() {
            TuiHandler.OptionSet = TuiHandler.OptionSet == Option.base() ? Option.logged() : Option.base();
        }
    }

    // Registration class
    public static class Registration extends BaseMenu {
        private final StringBuilder username        = new StringBuilder();
        private final StringBuilder password        = new StringBuilder();
        private final StringBuilder confirmPassword = new StringBuilder();

        @Override
        public void run() {
            while (!returnToMenu) {
                displayRegisterMenu();
                String key = reader.readBinding(keyMap);
                if (key == null) continue;

                navigateMenu(5, key);

                switch (key) {
                    case "ENTER":
                        if (index == 3) handleRegister();
                        else if (index == 4) returnToMenu = true;
                        break;
                    case "BACKSPACE": handleBackspace(); break;
                    default: handleInput(key); break;
                }
            }
        }

        private void handleRegister() {
            if (username.length() == 0 || password.length() == 0 || confirmPassword.length() == 0) {
                message = Ansi.RED + "Please fill in all fields" + Ansi.RESET;
            } else if (!password.toString().equals(confirmPassword.toString())) {
                message = Ansi.RED + "Passwords do not match. Please try again" + Ansi.RESET;
            } else {
                APIResponse response = null;
                try{
                    response = entryPoint.UserRegister(username.toString(), password.toString());
                } catch (Exception e) {
                    Terminate(Ansi.RED + "Registration failed: " + getStackTraceAsString(e) + Ansi.RESET);
                }
                setMessageOnReturn(response.getStatus() == Status.OK
                    ? Ansi.GREEN + "Registration successful!" + Ansi.RESET
                    : Ansi.RED + "Registration failed: " + response.getMessage() + Ansi.RESET
                );
                returnToMenu = true;
            }
        }

        private void handleBackspace() {
            switch (index) {
                case 0: deleteLastCharacter(username); break;
                case 1: deleteLastCharacter(password); break;
                case 2: deleteLastCharacter(confirmPassword); break;
            }
        }

        private void handleInput(String key) {
            if (key.length() == 1) {
                switch (index) {
                    case 0: username.append(key); break;
                    case 1: password.append(key); break;
                    case 2: confirmPassword.append(key); break;
                }
            }
        }

        private void displayRegisterMenu() {
            menuBuffer.append(Ansi.CLEAR)
                      .append("Registration Menu\n\n");
            appendMenuEntry(menuBuffer, " Username ", username.toString(), index == 0);
            appendMenuEntry(menuBuffer, " Password ", maskInput(password), index == 1);
            appendMenuEntry(menuBuffer, " Confirm ", maskInput(confirmPassword), index == 2);
            appendMenuEntry(menuBuffer, " REGISTER ", "", index == 3, true);
            appendMenuEntry(menuBuffer, " BACK TO MAIN MENU ", "", index == 4, true);
            menuBuffer.append("\n").append(message);
            message = "";
            displayMenu();
        }
    }

    // Login class (similar refactor to Registration)
    public static class Login extends BaseMenu {
        private final StringBuilder username = new StringBuilder();
        private final StringBuilder password = new StringBuilder();

        @Override
        public void run() {
            while (!returnToMenu) {
                displayLoginMenu();
                String key = reader.readBinding(keyMap);
                if (key == null) continue;
                navigateMenu(4, key);
                switch (key) {
                    case "ENTER":
                        if(index == 0 || index == 1) index+= 1;
                        else if (index == 2) handleLogin();
                        else if (index == 3) returnToMenu = true;
                        break;
                    case "BACKSPACE": handleBackspace(); break;
                    case "SPACE": handleInput(" "); break;
                    default: handleInput(key); break;
                }
            }
        }

        private void handleLogin() {
            if (username.length() == 0 || password.length() == 0) {
                message = Ansi.RED + "Please fill in all fields" + Ansi.RESET;
            } else {
                APIResponse response = null;
                try{
                    response = entryPoint.UserLogin(username.toString(), password.toString());
                } catch (Exception e) {
                    Terminate(Ansi.RED + "Login failed: " + getStackTraceAsString(e) + Ansi.RESET);
                }
                if(response.getStatus() == Status.OK){
                    flipOptionSet();
                    ClientMain.createUDPListener();
                    ClientMain.getUDPListener().startUDPlistening();
                }
                setMessageOnReturn(response.getStatus() == Status.OK
                    ? Ansi.GREEN + "Login successful!" + Ansi.RESET
                    : Ansi.RED + "Login failed: " + response.getMessage() + Ansi.RESET
                );
                returnToMenu = true;
            }
        }

        private void handleBackspace() {
            switch (index) {
                case 0: deleteLastCharacter(username); break;
                case 1: deleteLastCharacter(password); break;
            }
        }

        private void handleInput(String key) {
            if (key.length() == 1) {
                switch (index) {
                    case 0: username.append(key); break;
                    case 1: password.append(key); break;
                }
            }
        }

        private void displayLoginMenu() {
            menuBuffer.append(Ansi.CLEAR)
                      .append("Login Menu\n\n");
            appendMenuEntry(menuBuffer, "Username", username.toString(), index == 0);
            appendMenuEntry(menuBuffer, "Password", maskInput(password), index == 1);
            appendMenuEntry(menuBuffer, "LOGIN", "", index == 2, true);
            appendMenuEntry(menuBuffer, "BACK TO MAIN MENU", "", index == 3, true);
            menuBuffer.append("\n").append(message);
            message = "";
            displayMenu();
        }

    }
    // SearchHotel class
public static class SearchHotel extends BaseMenu {
    private final StringBuilder hotelName = new StringBuilder();
    private final StringBuilder city = new StringBuilder();
    private final StringBuilder result = new StringBuilder();

    @Override
    public void run() {
        while (!returnToMenu) {
            displaySearchHotelMenu();
            String key = reader.readBinding(keyMap);
            if (key == null) continue;

            navigateMenu(4, key);

            switch (key) {
                case "ENTER":
                    if (index == 2){
                        handleHotelSearch();
                        index = 0;
                        city.setLength(0);
                        hotelName.setLength(0);

                    }
                    else if (index == 3) returnToMenu = true;
                    break;
                case "SPACE": handleInput(" "); break;
                case "BACKSPACE": handleBackspace(); break;
                default: handleInput(key); break;
            }
        }
    }

    private void handleHotelSearch() {
        if (hotelName.length() == 0 || city.length() == 0) {
            message = Ansi.RED + "Please fill all fields" + Ansi.RESET;
        } else {
            APIResponse response = null;
            try{
                //System.out.println("Searching for hotels...");
                //Thread.sleep(5000);
                response = entryPoint.HotelSearch(city.toString(),hotelName.toString());
            } catch (Exception e) {
                Terminate(Ansi.RED + "Search failed: " + getStackTraceAsString(e) + Ansi.RESET);
            }
            if (response.getStatus() == Status.OK) {
                //message = Ansi.GREEN + "Hotels found: " + response.getData().length + Ansi.RESET;
                // Optionally display hotel details here...
                displayHotel(response.getHotel());
                return;
            } else {
                message = Ansi.RED + "Search failed: " + response.getMessage() + Ansi.RESET;
            }
        }
    }

    private void displayHotel(HotelDTO hotel){
        result  .append(Ansi.CLEAR)
                .append("Press ENTER to return to main menu\n\n")
                .append(hotel.toString());
        writer.print(result.toString());
        result.setLength(0);
        terminal.flush();
        while (true) {
            String key = reader.readBinding(keyMap);
            if (key.equals("ENTER")) break;
        }
    }

    private void handleBackspace() {
        switch (index) {
            case 0: deleteLastCharacter(hotelName); break;
            case 1: deleteLastCharacter(city); break;
        }
    }

    private void handleInput(String key) {
        if (key.length() == 1) {
            switch (index) {
                case 0: hotelName.append(key); break;
                case 1: city.append(key); break;
            }
        }
    }

    private void displaySearchHotelMenu() {
        menuBuffer.append(Ansi.CLEAR)
                  .append("Hotel Search Menu\n\n");
        appendMenuEntry(menuBuffer, "Hotel", hotelName.toString(), index == 0);
        appendMenuEntry(menuBuffer, "City",city.toString(), index == 1);
        appendMenuEntry(menuBuffer, "SEARCH", "", index == 2, true);
        appendMenuEntry(menuBuffer, "BACK TO MAIN MENU", "", index == 3, true);
        menuBuffer.append("\n").append(message);
        message = "";
        displayMenu();
    }
}

    // FullLogout class
    public static class FullLogout extends BaseMenu {
        private final StringBuilder username = new StringBuilder();
        private final StringBuilder password = new StringBuilder();

        @Override
        public void run() {
            while (!returnToMenu) {
                displayLogoutMenu();
                String key = reader.readBinding(keyMap);
                if (key == null) continue;
                navigateMenu(4, key);
                switch (key) {
                    case "ENTER":
                    if(index == 0 || index == 1) index+= 1;
                        else if (index == 2) handleLogout();
                        else if (index == 3) returnToMenu = true;
                        break;
                    case "BACKSPACE": handleBackspace(); break;
                    default: handleInput(key); break;
                }
            }
        }

        private void handleLogout() {
            if(username.length() == 0 || username.length() == 0){
                message = Ansi.RED + "Please fill in all fields" + Ansi.RESET;
            }
            APIResponse response = null;
            try{
            response = entryPoint.LogoutEverywhere(username.toString(), password.toString());
            } catch (Exception e) {
                Terminate(Ansi.RED + "Logout failed: " + getStackTraceAsString(e) + Ansi.RESET);
            }
            if(response.getStatus() == Status.OK){
                
            }
            setMessageOnReturn( response.getStatus() == Status.OK?
                                Ansi.GREEN + "Logout successful!" + Ansi.RESET:
                                Ansi.RED + "Logout failed: " + response.getMessage() + Ansi.RESET
            );
            returnToMenu = true;
        }

        private void handleBackspace() {
            switch (index) {
                case 0: deleteLastCharacter(username); break;
                case 1: deleteLastCharacter(password); break;
            }
        }

        private void handleInput(String key) {
            if (key.length() == 1) {
                switch (index) {
                    case 0: username.append(key); break;
                    case 1: password.append(key); break;
                }
            }
        }

        private void displayLogoutMenu() {
            menuBuffer.append(Ansi.CLEAR)
                      .append("Login Menu\n\n");
            appendMenuEntry(menuBuffer, "Username", username.toString(), index == 0);
            appendMenuEntry(menuBuffer, "Password", maskInput(password), index == 1);
            appendMenuEntry(menuBuffer, "LOGOUT", "", index == 2, true);
            appendMenuEntry(menuBuffer, "BACK TO MAIN MENU", "", index == 3, true);
            menuBuffer.append("\n").append(message);
            message = "";
            displayMenu();
        }
    }

    public static class FetchHotels extends BaseMenu{
        private final StringBuilder city = new StringBuilder();
        private final StringBuilder result = new StringBuilder();

        @Override
        public void run(){
            while(!returnToMenu){
                displayFetchMenu();
                String key = reader.readBinding(keyMap);
                if(key == null) continue;
                navigateMenu(3, key);
                switch(key){
                    case "ENTER":
                        if(index == 1) {
                            handleHotelsFetch();
                            city.setLength(0);
                            index = 0;
                        }
                        else if(index == 2) returnToMenu = true;
                        break;
                    case "BACKSPACE": handleBackspace(); break;
                    default: handleInput(key); break;
                }
            }

        }

        private void handleHotelsFetch(){
            int list_index = 0;
            boolean searchDone = false;
            ArrayList<HotelDTO> hotels = new ArrayList<>();
            if(city.length() == 0){
                message = Ansi.RED + "Please fill in all fields" + Ansi.RESET;
                return;
            } else {
                APIResponse response = null;
                try{
                    response = entryPoint.HotelsFetch(city.toString());
                } catch (Exception e) {
                    Terminate(Ansi.RED + "Fetch failed: " + getStackTraceAsString(e) + Ansi.RESET);
                }
                if(response.getStatus() != Status.FETCH_DONE && response.getStatus() != Status.FETCH_LEFT){
                    message = Ansi.RED + "Fetch failed: " + response.getMessage() + Ansi.RESET;
                    return;
                } else {

                    if(response.getStatus() == Status.FETCH_DONE){
                        searchDone = true;
                    }
                    for(HotelDTO hotel : response.getHotelList()){
                        hotels.add(hotel);
                    }
                    

                }

            }

            while(true){
                displayHotel(hotels.get(list_index));
                String key = reader.readBinding(keyMap);
                if(key == null) continue;
                switch(key){
                    case "ENTER": return;
                    case "LEFT": case "UP":{
                        if(list_index > 0) list_index -= 1;
                        else message = Ansi.BLUE + "First Hotel" + Ansi.RESET;
                    } break;
                    case "RIGHT": case "DOWN": case "TAB":{
                        if(list_index < hotels.size()-1) list_index += 1;
                        else{
                            if(searchDone){
                                message = Ansi.BLUE + "Hotels Finished" + Ansi.RESET;
                                searchDone = true;
                            } 
                            else{
                                searchDone = FetchAdd(hotels);
                                list_index++;
                            }
                        }
                    } break;
                }
            }

        }

        private void displayHotel(HotelDTO hotel){
            result  .append(Ansi.CLEAR)
                    .append("Press ENTER to return to main menu\n\n")
                    .append(hotel.toString())
                    .append("\n\n"+message);
            message = "";
            writer.print(result.toString());
            result.setLength(0);
            terminal.flush();


        }

        private boolean FetchAdd(ArrayList<HotelDTO> list){
            APIResponse response = null;
                try{
                    response = entryPoint.HotelsFetch();
                } catch (Exception e) {
                    Terminate(Ansi.RED + "Fetch failed: " + getStackTraceAsString(e) + Ansi.RESET);
                }
                if(response.getStatus() != Status.FETCH_DONE && response.getStatus() != Status.FETCH_LEFT){
                    message = Ansi.RED + "Fetch failed: " + response.getMessage() + Ansi.RESET;
                    Terminate(message);
                } else {
                    for(HotelDTO hotel : response.getHotelList()){
                        list.add(hotel);
                    }
                }
            return response.getStatus() == Status.FETCH_DONE;

        }

        private void handleBackspace(){
            if(index == 0) deleteLastCharacter(city);
        }

        private void handleInput(String key){
            if(key.length() == 1 && index == 0){
                city.append(key);
            }
        }

        private void displayFetchMenu() {
            menuBuffer.append(Ansi.CLEAR)
                      .append("Hotel Fetch Menu\n\n");
            appendMenuEntry(menuBuffer, "City", city.toString(), index == 0);
            appendMenuEntry(menuBuffer, "FETCH", "", index == 1, true);
            appendMenuEntry(menuBuffer, "BACK TO MAIN MENU", "", index == 2, true);
            menuBuffer.append("\n").append(message);
            message = "";
            displayMenu();
        }
    }
    
    public static class InsertReview extends BaseMenu {
        private final StringBuilder hotelName = new StringBuilder();
        private final StringBuilder city = new StringBuilder();

        private int[] scores = new int[5];
        private String[] scoreLabels = {"Global","Cleaning","Position","Price","Service"};

    
        @Override
        public void run() {
            while (!returnToMenu) {
                displaySearchMenu();
                String key = reader.readBinding(keyMap);
                if (key == null) continue;
    
                navigateMenu(4, key);
    
                switch (key) {
                    case "ENTER":
                        if (index == 2){
                            handleReviewProbe();
                            index = 0;
                            city.setLength(0);
                            hotelName.setLength(0);
                        }
                        else if (index == 3) returnToMenu = true;
                        break;
                    case "SPACE": handleInput(" "); break;
                    case "BACKSPACE": handleBackspace(); break;
                    default: handleInput(key); break;
                }
            }
        }
    
        private void handleReviewProbe() {
            if (hotelName.length() == 0 || city.length() == 0) {
                message = Ansi.RED + "Please fill all fields" + Ansi.RESET;
            } else {
                APIResponse response = null;
                try{
                    //System.out.println("Searching for hotels...");
                    //Thread.sleep(5000);
                    response = entryPoint.HotelPeek(city.toString(),hotelName.toString());
                } catch (Exception e) {
                    Terminate(Ansi.RED + "Search failed: " + getStackTraceAsString(e) + Ansi.RESET);
                }
                if (response.getStatus() == Status.OK) {
                    //message = Ansi.GREEN + "Hotels found: " + response.getData().length + Ansi.RESET;
                    // Optionally display hotel details here...
                    // insert review displayHotel(response.getHotel());
                    handleScoreInput();
                    return;
                } else {
                    message = Ansi.RED + "Search failed: " + response.getMessage() + Ansi.RESET;
                }
            }
        }
    
        private void handleBackspace() {
            switch (index) {
                case 0: deleteLastCharacter(hotelName); break;
                case 1: deleteLastCharacter(city); break;
            }
        }
    
        private void handleInput(String key) {
            if (key.length() == 1) {
                switch (index) {
                    case 0: hotelName.append(key); break;
                    case 1: city.append(key); break;
                }
            }
        }

        private String showStars(int count){
            StringBuilder stars = new StringBuilder();
            stars.append(Ansi.YELLOW);
            for(int i = 0; i < count; i++){
                stars.append("⭐ ");
            }
            stars.append(Ansi.RESET);
            return stars.toString();
        }
    
        private void displaySearchMenu() {
            menuBuffer.append(Ansi.CLEAR)
                      .append("Review Select Menu\n\n");
            appendMenuEntry(menuBuffer, "Hotel", hotelName.toString(), index == 0);
            appendMenuEntry(menuBuffer, "City",city.toString(), index == 1);
            appendMenuEntry(menuBuffer, "SELECT", "", index == 2, true);
            appendMenuEntry(menuBuffer, "BACK TO MAIN MENU", "", index == 3, true);
            menuBuffer.append("\n").append(message);
            message = "";
            displayMenu();
        }

        private void displayScoreMenu(){
            menuBuffer  .append(" INSERTING REVIEW\n\n")
                        .append(Ansi.BLUE+"Hotel: "+Ansi.RESET + hotelName.toString() +Ansi.BLUE+" City: "+Ansi.RESET+city.toString()+ "\n\n");

            for(int i = 0; i < 5; i++){
                //sottolineato
                if((index == 3 && i == 3)){
                    menuBuffer.append(Ansi.HIGHLIGHT+" "+scoreLabels[i]+" "+Ansi.RESET+"\t\t"+showStars(scores[i])+"\n\n");
                } 
                else if(index == i){
                    menuBuffer.append(Ansi.HIGHLIGHT+" "+scoreLabels[i]+" "+Ansi.RESET+"\t"+showStars(scores[i])+"\n\n");
                }
                else if(i == 0 || i == 3 || i==4){
                    menuBuffer.append(scoreLabels[i]+"\t\t"+showStars(scores[i])+"\n\n");
                } else menuBuffer.append(scoreLabels[i]+"\t"+showStars(scores[i])+"\n\n");
            }
            menuBuffer.append("\n");
            appendMenuEntry(menuBuffer, " SUBMIT ", "", index == 5, true);
            appendMenuEntry(menuBuffer, " BACK TO MAIN MENU ", "", index == 6, true);
            menuBuffer.append("\n"+message);
            displayMenu();

        }

        private void handleScoreInput(){
            for(int i = 0; i < 5; i++){ scores[i] = 0;}
            index = 0;
            while(true){
                displayScoreMenu();
                String key = reader.readBinding(keyMap);
                if(key == null) continue;
                navigateMenu(7,key);
                switch(key){
                    case "ENTER":
                        if(index == 6) return;
                        else if(index == 5){
                            //chiamata API
                            APIResponse response = null;
                            Score score = new Score(scores[0],scores[1],scores[2],scores[3],scores[4]);
                            try{
                                response = entryPoint.InsertReview(city.toString(),hotelName.toString(),score);
                            } catch (Exception e) {
                                Terminate(Ansi.RED + "Review failed: " + getStackTraceAsString(e) + Ansi.RESET);
                            }
                            if(response.getStatus() == Status.OK){
                                message = Ansi.GREEN + "Review Inserted!" + Ansi.RESET;
                            } else {
                                message = Ansi.RED + "Review failed: " + response.getMessage() + Ansi.RESET;
                            }
                            return;
                        } break;
                    case "BACKSPACE": case "LEFT":{
                        if(index >=0 && index < 5){
                            if(scores[index] > 0) scores[index] -= 1;
                            else{
                                message = Ansi.RED + "Score cannot be negative" + Ansi.RESET;
                            }
                        }
                    } break;
                    case "RIGHT": {
                        if(index >=0 && index < 5){
                            if(scores[index] < 5) scores[index] += 1;
                            else{
                                message = Ansi.RED + "Score cannot be greater than 5" + Ansi.RESET;
                            }
                        } 
                    } break;
                }
            }
        }

    }

    
    

    
    
}
