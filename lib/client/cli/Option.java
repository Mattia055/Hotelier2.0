package lib.client.cli;

public enum Option{

    // devo poi impostare tutti i metodi che devono essere void
    REGISTER("Register"),
    LOGIN("Login"),
    SEARCH_H("Search Hotel"),
    FETCH_H("Fetch Hotels"),
    FULL_LOGOUT("Full Logout"),
    LOGOUT("Logout"),
    REVIEW("Write Review"),
    BADGE("Show Badge"),
    EXIT("Exit");

    private final String description;

    private static final Option[] Base = new Option[]{
        REGISTER,LOGIN,SEARCH_H,FETCH_H,FULL_LOGOUT,EXIT
    };

    private static final Option[] Logged = new Option[]{
        SEARCH_H,FETCH_H,LOGOUT,REVIEW,BADGE,EXIT
    };


    Option(String description){
        this.description = description;
    }

    public String description() {
        return description;
    }

    public static Option[] base() {
        return Base;
    }
    public static Option[] logged() {
        return Logged;
    }
}
