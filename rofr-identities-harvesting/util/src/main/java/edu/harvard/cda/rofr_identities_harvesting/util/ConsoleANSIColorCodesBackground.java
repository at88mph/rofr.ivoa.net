package edu.harvard.cda.rofr_identities_harvesting.util;

public class ConsoleANSIColorCodesBackground {

    public static String BLACK          = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BLACK          +10);
    public static String RED            = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._RED            +10);
    public static String GREEN          = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._GREEN          +10);
    public static String YELLOW         = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._YELLOW         +10);
    public static String BLUE           = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BLUE           +10);
    public static String MAGENTA        = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._MAGENTA        +10);
    public static String CYAN           = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._CYAN           +10);
    public static String WHITE          = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._WHITE          +10);
    public static String BRIGHT_BLACK   = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_BLACK   +10);
    public static String BRIGHT_RED     = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_RED     +10);
    public static String BRIGHT_GREEN   = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_GREEN   +10);
    public static String BRIGHT_YELLOW  = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_YELLOW  +10);
    public static String BRIGHT_BLUE    = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_BLUE    +10);
    public static String BRIGHT_MAGENTA = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_MAGENTA +10);
    public static String BRIGHT_CYAN    = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_CYAN    +10);
    public static String BRIGHT_WHITE   = ConsoleANSIColorCodes.escSeq(ConsoleANSIColorCodes._BRIGHT_WHITE   +10);

    

    protected final String foreground;
    protected ConsoleANSIColorCodesBackground(final String foreground) {
        this.foreground = foreground;
    }

    public String onBlack() {
        return _bg(BLACK);
    }

    public String onRed() {
        return _bg(RED);
    }

    public String onGreen() {
        return _bg(GREEN);
    }

    public String onYellow() {
        return _bg(YELLOW);
    }

    public String onBlue() {
        return _bg(BLUE);
    }

    public String onMagenta() {
        return _bg(MAGENTA);
    }

    public String onCyan() {
        return _bg(CYAN);
    }

    public String onWhite() {
        return _bg(WHITE);
    }

    public String onBrightBlack() {
        return _bg(BRIGHT_BLACK);
    }

    public String onBrightRed() {
        return _bg(BRIGHT_RED);
    }

    public String onBrightGreen() {
        return _bg(BRIGHT_GREEN);
    }

    public String onBrightYellow() {
        return _bg(BRIGHT_YELLOW);
    }

    public String onBrightBlue() {
        return _bg(BRIGHT_BLUE);
    }

    public String onBrightMagenta() {
        return _bg(BRIGHT_MAGENTA);
    }

    public String onBrightCyan() {
        return _bg(BRIGHT_CYAN);
    }

    public String onBrightWhite() {
        return _bg(BRIGHT_WHITE);
    }
    
    

    private final String _bg(final String bg) {
        return String.format("%s%s", foreground, bg);
    }

}
