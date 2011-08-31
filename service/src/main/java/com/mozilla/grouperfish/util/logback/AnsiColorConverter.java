package com.mozilla.grouperfish.util.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

@SuppressWarnings("unused")
public class AnsiColorConverter extends ClassicConverter {

    private static final int NORMAL = 0;
    private static final int BRIGHT = 1;
    private static final int FOREGROUND_BLACK = 30;
    private static final int FOREGROUND_RED = 31;
    private static final int FOREGROUND_GREEN = 32;
    private static final int FOREGROUND_YELLOW = 33;
    private static final int FOREGROUND_BLUE = 34;
    private static final int FOREGROUND_MAGENTA = 35;
    private static final int FOREGROUND_CYAN = 36;
    private static final int FOREGROUND_WHITE = 37;

    private static final String PREFIX = "\u001b[";
    private static final String SUFFIX = "m";
    private static final char SEPARATOR = ';';
    private static final String END_COLOR = PREFIX + SUFFIX;

    private static final String ERROR_COLOR = PREFIX + BRIGHT + SEPARATOR + FOREGROUND_RED    + SUFFIX;
    private static final String WARN_COLOR  = PREFIX + NORMAL + SEPARATOR + FOREGROUND_YELLOW + SUFFIX;
    private static final String INFO_COLOR  = PREFIX + NORMAL + SEPARATOR + FOREGROUND_GREEN  + SUFFIX;
    private static final String DEBUG_COLOR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_CYAN   + SUFFIX;
    private static final String TRACE_COLOR = PREFIX + NORMAL + SEPARATOR + FOREGROUND_BLUE   + SUFFIX;

    @Override
    public String convert(final ILoggingEvent event) {
        final StringBuilder sb = new StringBuilder();
        sb.append(getColor(event.getLevel()));
        sb.append(event.getLevel());
        sb.append(END_COLOR);
        return sb.toString();
    }

    /**
     * Returns the appropriate characters to change the color for the specified
     * logging level.
     */
    private String getColor(final Level level) {
        switch (level.toInt()) {
            case Level.ERROR_INT: return ERROR_COLOR;
            case Level.WARN_INT: return WARN_COLOR;
            case Level.INFO_INT: return INFO_COLOR;
            case Level.DEBUG_INT: return DEBUG_COLOR;
            case Level.TRACE_INT: return TRACE_COLOR;
            default:
                return "";
        }
    }
}
