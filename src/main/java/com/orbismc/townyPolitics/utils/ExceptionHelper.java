package com.orbismc.townyPolitics.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ExceptionHelper {
    private final DelegateLogger logger;

    public ExceptionHelper(DelegateLogger logger) {
        this.logger = logger;
    }

    public void handleStorageException(String operation, Exception e) {
        logger.severe("Storage operation failed: " + operation);
        logger.severe("Error: " + e.getMessage());

        if (logger.isEnabled()) {
            e.printStackTrace();
        }
    }

    public void handleCommandException(CommandSender sender, String command, Exception e) {
        logger.severe("Command execution failed: " + command);
        logger.severe("Error: " + e.getMessage());

        sender.sendMessage(ChatColor.RED + "An error occurred while executing this command.");

        if (logger.isEnabled()) {
            e.printStackTrace();
        }
    }

    public void handleEventException(String event, Exception e) {
        logger.severe("Event handling failed: " + event);
        logger.severe("Error: " + e.getMessage());

        if (logger.isEnabled()) {
            e.printStackTrace();
        }
    }
}