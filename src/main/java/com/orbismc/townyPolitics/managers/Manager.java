package com.orbismc.townyPolitics.managers;

/**
 * Base interface for all managers in the plugin
 */
public interface Manager {
    /**
     * Load data from storage
     */
    void loadData();

    /**
     * Save all data to storage
     */
    void saveAllData();
}