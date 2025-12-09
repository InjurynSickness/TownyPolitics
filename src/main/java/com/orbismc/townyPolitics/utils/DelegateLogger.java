package com.orbismc.townyPolitics.utils;

import com.orbismc.townyPolitics.TownyPolitics;

public class DelegateLogger {
    private final TownyPolitics plugin;
    private final String prefix;

    public DelegateLogger(TownyPolitics plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = "[" + prefix + "] ";
    }

    public void fine(String message) {
        plugin.getDebugLogger().fine(prefix + message);
    }

    public void info(String message) {
        plugin.getDebugLogger().info(prefix + message);
    }

    public void warning(String message) {
        plugin.getDebugLogger().warning(prefix + message);
    }

    public void severe(String message) {
        plugin.getDebugLogger().severe(prefix + message);
    }

    public boolean isEnabled() {
        return plugin.getDebugLogger().isEnabled();
    }
}