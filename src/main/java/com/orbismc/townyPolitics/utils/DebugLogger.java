package com.orbismc.townyPolitics.utils;

import com.orbismc.townyPolitics.TownyPolitics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

public class DebugLogger {
    private final TownyPolitics plugin;
    private final boolean enabled;
    private final boolean logToFile;
    private final String fileName;
    private final Level logLevel;
    private File logFile;

    public DebugLogger(TownyPolitics plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("debug.enabled", false);
        this.logToFile = plugin.getConfig().getBoolean("debug.log_to_file", true);
        this.fileName = plugin.getConfig().getString("debug.file_name", "debug.log");
        String configLevel = plugin.getConfig().getString("debug.log_level", "INFO");
        this.logLevel = parseLogLevel(configLevel);

        if (enabled && logToFile) {
            setupLogFile();
        }
    }

    private Level parseLogLevel(String level) {
        try {
            return Level.parse(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid debug log level: " + level + ". Using INFO instead.");
            return Level.INFO;
        }
    }

    private void setupLogFile() {
        logFile = new File(plugin.getDataFolder(), fileName);
        try {
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                writer.println("\n\n========== TownyPolitics Debug Log Started at " +
                        dateFormat.format(new Date()) + " ==========");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create debug log file: " + e.getMessage());
        }
    }

    public void log(Level level, String message) {
        if (!enabled || level.intValue() < logLevel.intValue()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedMessage = "[" + dateFormat.format(new Date()) + "] [" +
                level.getName() + "] " + message;

        if (logToFile && logFile != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
                writer.println(formattedMessage);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to write to debug log: " + e.getMessage());
                plugin.getLogger().log(level, message); // Fallback to console
            }
        } else {
            plugin.getLogger().log(level, message);
        }
    }

    public void fine(String message) {
        log(Level.FINE, message);
    }

    public void info(String message) {
        log(Level.INFO, message);
    }

    public void warning(String message) {
        log(Level.WARNING, message);
    }

    public void severe(String message) {
        log(Level.SEVERE, message);
    }

    public boolean isEnabled() {
        return enabled;
    }
}