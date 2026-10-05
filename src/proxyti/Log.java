package proxyti;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Tiny dependency-free logger. Keeps the proxy output readable without pulling
 * in slf4j/log4j.
 */
public final class Log {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static volatile boolean debug = false;

    private Log() {
    }

    public static void setDebug(boolean value) {
        debug = value;
    }

    public static void info(String msg, Object... args) {
        print("INFO", msg, args);
    }

    public static void warn(String msg, Object... args) {
        print("WARN", msg, args);
    }

    public static void error(String msg, Object... args) {
        print("ERROR", msg, args);
    }

    public static void debug(String msg, Object... args) {
        if (debug) {
            print("DEBUG", msg, args);
        }
    }

    private static void print(String level, String msg, Object... args) {
        String rendered = args.length == 0 ? msg : String.format(msg, args);
        System.out.println("[" + LocalTime.now().format(FMT) + " " + level + "] " + rendered);
    }
}