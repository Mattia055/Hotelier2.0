package lib.client.cli;

public final class Ansi{
    public static final String RESET = "\u001B[0m";
    public static final String HIDE_CURSOR = "\u001B[?25l";
    public static final String SHOW_CURSOR = "\u001B[?25h";
    public static final String CLEAR = "\u001B[H\u001B[2J";
    public static final String HIGHLIGHT = "\u001B[7m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String CYAN = "\u001B[36m";
}