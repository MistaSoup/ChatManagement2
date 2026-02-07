package com.chatmanagement.managers;

import com.chatmanagement.ChatManagement2;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MuteManager {
    
    private final ChatManagement2 plugin;
    private final Map<UUID, MuteData> mutedPlayers;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public MuteManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.mutedPlayers = new ConcurrentHashMap<>();
        
        // Load mutes from storage
        loadMutes();
        
        // Start unmute checker task (every second)
        startUnmuteChecker();
    }
    
    /**
     * Mute a player for a specified duration
     */
    public void mutePlayer(UUID uuid, int durationSeconds) {
        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        MuteData muteData = new MuteData(endTime, durationSeconds);
        mutedPlayers.put(uuid, muteData);
        
        // Save to storage
        saveMute(uuid, muteData);
        
        // Notify player if online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && plugin.getConfigManager().shouldNotifyMute()) {
            String message = plugin.getConfigManager().getMuteNotification()
                    .replace("{duration}", String.valueOf(durationSeconds));
            player.sendMessage(serializer.deserialize(message));
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Muted player " + uuid + " for " + durationSeconds + " seconds");
        }
    }
    
    /**
     * Unmute a player
     */
    public void unmutePlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
        
        // Remove from storage
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            plugin.getDatabaseManager().removeMute(uuid);
        } else if (plugin.getYAMLStorage() != null) {
            plugin.getYAMLStorage().removeMute(uuid);
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Unmuted player " + uuid);
        }
    }
    
    /**
     * Check if a player is muted
     */
    public boolean isMuted(UUID uuid) {
        MuteData muteData = mutedPlayers.get(uuid);
        if (muteData == null) {
            return false;
        }
        
        // Check if mute has expired
        if (System.currentTimeMillis() >= muteData.endTime) {
            unmutePlayer(uuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining mute time in seconds
     */
    public int getRemainingTime(UUID uuid) {
        MuteData muteData = mutedPlayers.get(uuid);
        if (muteData == null) {
            return 0;
        }
        
        long remaining = muteData.endTime - System.currentTimeMillis();
        return (int) Math.max(0, remaining / 1000);
    }
    
    /**
     * Handle player disconnect - pause mute timer
     */
    public void handleDisconnect(UUID uuid) {
        MuteData muteData = mutedPlayers.get(uuid);
        if (muteData != null && !muteData.isPaused) {
            long remaining = muteData.endTime - System.currentTimeMillis();
            if (remaining > 0) {
                muteData.pausedTimeRemaining = remaining;
                muteData.isPaused = true;
                
                // Save paused state
                saveMute(uuid, muteData);
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Paused mute for " + uuid + " with " + (remaining / 1000) + " seconds remaining");
                }
            }
        }
    }
    
    /**
     * Handle player reconnect - resume mute timer
     */
    public void handleReconnect(UUID uuid) {
        MuteData muteData = mutedPlayers.get(uuid);
        if (muteData != null && muteData.isPaused) {
            muteData.endTime = System.currentTimeMillis() + muteData.pausedTimeRemaining;
            muteData.isPaused = false;
            muteData.pausedTimeRemaining = 0;
            
            // Save resumed state
            saveMute(uuid, muteData);
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Resumed mute for " + uuid);
            }
        }
    }
    
    /**
     * Load mutes from storage
     */
    private void loadMutes() {
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            loadMutesFromDatabase();
        } else if (plugin.getYAMLStorage() != null) {
            loadMutesFromYAML();
        }
    }
    
    /**
     * Load mutes from database
     */
    public void loadMutesFromDatabase() {
        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().isConnected()) {
            return;
        }
        
        Map<UUID, MuteData> mutes = plugin.getDatabaseManager().loadMutes();
        mutedPlayers.putAll(mutes);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Loaded " + mutes.size() + " mutes from database");
        }
    }
    
    /**
     * Load mutes from YAML
     */
    public void loadMutesFromYAML() {
        if (plugin.getYAMLStorage() == null) {
            return;
        }
        
        Map<UUID, MuteData> mutes = plugin.getYAMLStorage().loadMutes();
        mutedPlayers.putAll(mutes);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Loaded " + mutes.size() + " mutes from YAML");
        }
    }
    
    /**
     * Save a single mute to storage
     */
    private void saveMute(UUID uuid, MuteData muteData) {
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            plugin.getDatabaseManager().saveMute(uuid, muteData);
        } else if (plugin.getYAMLStorage() != null) {
            plugin.getYAMLStorage().saveMute(uuid, muteData);
        }
    }
    
    /**
     * Save all mutes to storage
     */
    public void saveMutes() {
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
            for (Map.Entry<UUID, MuteData> entry : mutedPlayers.entrySet()) {
                plugin.getDatabaseManager().saveMute(entry.getKey(), entry.getValue());
            }
        } else if (plugin.getYAMLStorage() != null) {
            plugin.getYAMLStorage().saveMutes(mutedPlayers);
        }
    }
    
    /**
     * Start task to check for expired mutes
     * Uses Folia's async scheduler for compatibility
     */
    private void startUnmuteChecker() {
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            long now = System.currentTimeMillis();
            mutedPlayers.entrySet().removeIf(entry -> {
                MuteData muteData = entry.getValue();
                if (!muteData.isPaused && now >= muteData.endTime) {
                    UUID uuid = entry.getKey();
                    
                    // Remove from storage
                    if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isConnected()) {
                        plugin.getDatabaseManager().removeMute(uuid);
                    } else if (plugin.getYAMLStorage() != null) {
                        plugin.getYAMLStorage().removeMute(uuid);
                    }
                    
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Auto-unmuted player " + uuid);
                    }
                    
                    return true;
                }
                return false;
            });
        }, 50L, 50L, java.util.concurrent.TimeUnit.MILLISECONDS); // Run every second
    }
    
    /**
     * Data class for mute information
     */
    public static class MuteData {
        public long endTime;
        public int originalDuration;
        public boolean isPaused;
        public long pausedTimeRemaining;
        
        public MuteData(long endTime, int originalDuration) {
            this.endTime = endTime;
            this.originalDuration = originalDuration;
            this.isPaused = false;
            this.pausedTimeRemaining = 0;
        }
        
        public MuteData(long endTime, int originalDuration, boolean isPaused, long pausedTimeRemaining) {
            this.endTime = endTime;
            this.originalDuration = originalDuration;
            this.isPaused = isPaused;
            this.pausedTimeRemaining = pausedTimeRemaining;
        }
    }
}
