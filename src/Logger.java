package log;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    
    private static final String LOG_FILE = System.getProperty("user.home")+"/myhttpserver/log/serverhttp.log";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss");

    public static synchronized void log(String clientIp, String method, String url, String protocol, int statusCode, long responseSize, String referer, String userAgent) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logEntry = String.format("%s - - [%s] \"%s %s %s\" %d %d \"%s\" \"%s\"%n",
                clientIp, timestamp, method, url, protocol, statusCode, responseSize, referer, userAgent);

        writeLog(logEntry);
    }

    public static synchronized void logEvent(String eventType, String message) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String logEntry = String.format("[EVENT] [%s] [%s] %s%n", timestamp, eventType, message);
        writeLog(logEntry);
    }

    private static void writeLog(String logEntry) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    public static void logStartup() {
        logEvent("STARTUP", "Server started successfully.");
    }

    public static void logShutdown() {
        logEvent("SHUTDOWN", "Server shut down successfully.");
    }

    public static void logInternalError(String errorMessage) {
        logEvent("ERROR", "Internal server error: " + errorMessage);
    }
}
