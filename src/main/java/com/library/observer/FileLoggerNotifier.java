package com.library.observer;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * File logger observer (for audit trail)
 * Follows Observer Pattern from refactoring.guru
 * @author Library Team
 * @version 1.0
 */
public class FileLoggerNotifier implements Observer {
    private String logFilePath;

    public FileLoggerNotifier(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    @Override
    public void update(NotificationEvent event) {
        try (FileWriter writer = new FileWriter(logFilePath, true)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logEntry = String.format("[%s] %s - %s - %s%n",
                    timestamp,
                    event.getEventType(),
                    event.getUser() != null ? event.getUser().getUserId() : "SYSTEM",
                    event.getMessage()
            );
            writer.write(logEntry);
            System.out.println("Event logged to file: " + logFilePath);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}