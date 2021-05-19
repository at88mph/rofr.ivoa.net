package edu.harvard.cda.rofr_identities_harvesting.util;

/**
 * Color codes and names are from <a href='https://en.wikipedia.org/wiki/ANSI_escape_code'>wikipedia</a>
 *
 */
public final class ConsoleANSIColorCodes {

    private ConsoleANSIColorCodes() {}


    protected static final int _RESET          = 0;
    protected static final int _BLACK          = 30;
    protected static final int _RED            = 31;
    protected static final int _GREEN          = 32;
    protected static final int _YELLOW         = 33;
    protected static final int _BLUE           = 34;
    protected static final int _MAGENTA        = 35;
    protected static final int _CYAN           = 36;
    protected static final int _WHITE          = 37;
    protected static final int _BRIGHT_BLACK   = 90;
    protected static final int _BRIGHT_RED     = 91;
    protected static final int _BRIGHT_GREEN   = 92;
    protected static final int _BRIGHT_YELLOW  = 93;
    protected static final int _BRIGHT_BLUE    = 94;
    protected static final int _BRIGHT_MAGENTA = 95;
    protected static final int _BRIGHT_CYAN    = 96;
    protected static final int _BRIGHT_WHITE   = 97;

    protected static final String escSeq(final int n) {
        return String.format("\u001B[%dm", n);
    }
    
    public static String RESET          = escSeq(_RESET);
    public static String BLACK          = escSeq(_BLACK);
    public static String RED            = escSeq(_RED);
    public static String GREEN          = escSeq(_GREEN);
    public static String YELLOW         = escSeq(_YELLOW);
    public static String BLUE           = escSeq(_BLUE);
    public static String MAGENTA        = escSeq(_MAGENTA);
    public static String CYAN           = escSeq(_CYAN);
    public static String WHITE          = escSeq(_WHITE);
    public static String BRIGHT_BLACK   = escSeq(_BRIGHT_BLACK);
    public static String BRIGHT_RED     = escSeq(_BRIGHT_RED);
    public static String BRIGHT_GREEN   = escSeq(_BRIGHT_GREEN);
    public static String BRIGHT_YELLOW  = escSeq(_BRIGHT_YELLOW);
    public static String BRIGHT_BLUE    = escSeq(_BRIGHT_BLUE);
    public static String BRIGHT_MAGENTA = escSeq(_BRIGHT_MAGENTA);
    public static String BRIGHT_CYAN    = escSeq(_BRIGHT_CYAN);
    public static String BRIGHT_WHITE   = escSeq(_BRIGHT_WHITE);
    



    // TODO: use escape codes for both FG and BG colors as found here:
    //     


    public static ConsoleANSIColorCodesBackground black() {
        return new ConsoleANSIColorCodesBackground(BLACK);
    }

    public static ConsoleANSIColorCodesBackground red() {
        return new ConsoleANSIColorCodesBackground(RED);
    }
    
    public static ConsoleANSIColorCodesBackground green() {
        return new ConsoleANSIColorCodesBackground(GREEN);
    }
    
    public static ConsoleANSIColorCodesBackground yellow() {
        return new ConsoleANSIColorCodesBackground(YELLOW);
    }
    
    public static ConsoleANSIColorCodesBackground blue() {
        return new ConsoleANSIColorCodesBackground(BLUE);
    }
    
    public static ConsoleANSIColorCodesBackground magenta() {
        return new ConsoleANSIColorCodesBackground(MAGENTA);
    }
    
    public static ConsoleANSIColorCodesBackground cyan() {
        return new ConsoleANSIColorCodesBackground(CYAN);
    }
    
    public static ConsoleANSIColorCodesBackground white() {
        return new ConsoleANSIColorCodesBackground(WHITE);
    }
    
    public static ConsoleANSIColorCodesBackground brightBlack() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_BLACK);
    }
    
    public static ConsoleANSIColorCodesBackground brightRed() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_RED);
    }
    
    public static ConsoleANSIColorCodesBackground brightGreen() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_GREEN);
    }
    
    public static ConsoleANSIColorCodesBackground brightYellow() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_YELLOW);
    }
    
    public static ConsoleANSIColorCodesBackground brightBlue() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_BLUE);
    }
    
    public static ConsoleANSIColorCodesBackground brightMagenta() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_MAGENTA);
    }
    
    public static ConsoleANSIColorCodesBackground brightCyan() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_CYAN);
    }
    
    public static ConsoleANSIColorCodesBackground brightWhite() {
        return new ConsoleANSIColorCodesBackground(BRIGHT_WHITE);
    }
}

