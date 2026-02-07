package com.chatmanagement.storage;

import com.chatmanagement.ChatManagement2;
import com.chatmanagement.managers.MuteManager.MuteData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YAMLStorage {
    
    private final ChatManagement2 plugin;
    private File mutesFile;
    private FileConfiguration mutesConfig;
    
    public YAMLStorage(ChatManagement2 plugin) {
        this.plugin = plugin;
        setupFiles();
    }
    
    /**
     * Setup storage files
     */
    private void setupFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        mutesFile = new File(dataFolder, "mutes.yml");
        if (!mutesFile.exists()) {
            try {
                mutesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create mutes.yml: " + e.getMessage());
            }
        }
        
        mutesConfig = YamlConfiguration.loadConfiguration(mutesFile);
    }
    
    /**
     * Save a single mute
     */
    public void saveMute(UUID uuid, MuteData muteData) {
        String path = uuid.toString();
        mutesConfig.set(path + ".end_time", muteData.endTime);
        mutesConfig.set(path + ".original_duration", muteData.originalDuration);
        mutesConfig.set(path + ".is_paused", muteData.isPaused);
        mutesConfig.set(path + ".paused_time_remaining", muteData.pausedTimeRemaining);
        
        saveMutesFile();
    }
    
    /**
     * Save all mutes
     */
    public void saveMutes(Map<UUID, MuteData> mutes) {
        // Clear existing data
        for (String key : mutesConfig.getKeys(false)) {
            mutesConfig.set(key, null);
        }
        
        // Save all mutes
        for (Map.Entry<UUID, MuteData> entry : mutes.entrySet()) {
            String path = entry.getKey().toString();
            MuteData muteData = entry.getValue();
            
            mutesConfig.set(path + ".end_time", muteData.endTime);
            mutesConfig.set(path + ".original_duration", muteData.originalDuration);
            mutesConfig.set(path + ".is_paused", muteData.isPaused);
            mutesConfig.set(path + ".paused_time_remaining", muteData.pausedTimeRemaining);
        }
        
        saveMutesFile();
    }
    
    /**
     * Remove a mute
     */
    public void removeMute(UUID uuid) {
        mutesConfig.set(uuid.toString(), null);
        saveMutesFile();
    }
    
    /**
     * Load all mutes
     */
    public Map<UUID, MuteData> loadMutes() {
        Map<UUID, MuteData> mutes = new HashMap<>();
        
        for (String key : mutesConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long endTime = mutesConfig.getLong(key + ".end_time");
                int originalDuration = mutesConfig.getInt(key + ".original_duration");
                boolean isPaused = mutesConfig.getBoolean(key + ".is_paused");
                long pausedTimeRemaining = mutesConfig.getLong(key + ".paused_time_remaining");
                
                MuteData muteData = new MuteData(endTime, originalDuration, isPaused, pausedTimeRemaining);
                mutes.put(uuid, muteData);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in mutes.yml: " + key);
            }
        }
        
        return mutes;
    }
    
    /**
     * Save mutes file to disk
     */
    private void saveMutesFile() {
        try {
            mutesConfig.save(mutesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save mutes.yml: " + e.getMessage());
        }
    }
    
    /**
     * Reload mutes from disk
     */
    public void reload() {
        mutesConfig = YamlConfiguration.loadConfiguration(mutesFile);
    }
}
