package com.chatmanagement.managers;

import com.chatmanagement.ChatManagement2;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpamDetectionManager {
    
    private final ChatManagement2 plugin;
    private final Map<UUID, LinkedList<MessageData>> messageHistory;
    private final Map<UUID, LinkedList<Long>> messageTimes;
    private final Map<UUID, Integer> spamKickCount;
    private final Map<UUID, Long> lastKickTime;
    
    public SpamDetectionManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.messageHistory = new ConcurrentHashMap<>();
        this.messageTimes = new ConcurrentHashMap<>();
        this.spamKickCount = new ConcurrentHashMap<>();
        this.lastKickTime = new ConcurrentHashMap<>();
    }
    
    public void reload() {
        // Clear history on reload to prevent issues with changed settings
        messageHistory.clear();
        messageTimes.clear();
    }
    
    /**
     * Check if a message is spam based on duplicate detection
     * Returns true if the message should be blocked
     */
    public boolean isDuplicateSpam(Player player, String message) {
        UUID uuid = player.getUniqueId();
        
        // Bypass permission check
        if (player.hasPermission(plugin.getConfigManager().getBypassPermission())) {
            return false;
        }
        
        // Normalize message for comparison
        String normalized = normalizeMessage(message);
        
        // Check minimum length
        int minLength = plugin.getConfigManager().getMinMessageLength();
        if (normalized.length() < minLength) {
            return false; // Too short to be considered spam
        }
        
        // Get or create message history
        LinkedList<MessageData> history = messageHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        // Clean old messages based on cooldown
        long cooldown = plugin.getConfigManager().getDuplicateCooldown() * 1000L;
        long now = System.currentTimeMillis();
        history.removeIf(data -> now - data.timestamp > cooldown);
        
        // Check for duplicates
        int maxRepeats = plugin.getConfigManager().getMaxRepeats();
        int similarCount = 0;
        
        for (MessageData data : history) {
            if (isSimilar(normalized, data.normalizedMessage)) {
                similarCount++;
                if (similarCount >= maxRepeats) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("Blocked duplicate message from " + player.getName() + ": " + message);
                    }
                    return true;
                }
            }
        }
        
        // Add message to history
        history.add(new MessageData(normalized, now));
        
        // Limit history size
        int maxSize = plugin.getConfigManager().getMessageHistorySize();
        while (history.size() > maxSize) {
            history.removeFirst();
        }
        
        return false;
    }
    
    /**
     * Check if player is spam flooding (too many messages too quickly)
     * Returns true if player should be kicked
     */
    public boolean isRapidSpam(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Bypass permission check
        if (player.hasPermission(plugin.getConfigManager().getBypassPermission())) {
            return false;
        }
        
        if (!plugin.getConfigManager().isAntiSpamKickEnabled()) {
            return false;
        }
        
        // Get or create message times list
        LinkedList<Long> times = messageTimes.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        long now = System.currentTimeMillis();
        long timeWindow = plugin.getConfigManager().getSpamTimeWindow() * 1000L;
        
        // Add current message time
        times.add(now);
        
        // Remove old messages outside time window
        times.removeIf(time -> now - time > timeWindow);
        
        // Check if threshold exceeded
        int threshold = plugin.getConfigManager().getSpamMessageThreshold();
        if (times.size() >= threshold) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Rapid spam detected from " + player.getName() + ": " + times.size() + " messages in " + plugin.getConfigManager().getSpamTimeWindow() + " seconds");
            }
            
            // Clear times to prevent multiple kicks
            times.clear();
            
            // Track kick for auto-mute
            trackSpamKick(uuid);
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Track spam kicks for auto-mute system
     */
    private void trackSpamKick(UUID uuid) {
        if (!plugin.getConfigManager().isAutoMuteEnabled()) {
            return;
        }
        
        long now = System.currentTimeMillis();
        long kickWindow = plugin.getConfigManager().getMuteKickWindow() * 60 * 1000L;
        
        // Check if last kick was outside the window
        Long lastKick = lastKickTime.get(uuid);
        if (lastKick != null && now - lastKick > kickWindow) {
            // Reset counter if outside window
            spamKickCount.put(uuid, 1);
        } else {
            // Increment counter
            int count = spamKickCount.getOrDefault(uuid, 0) + 1;
            spamKickCount.put(uuid, count);
            
            // Check if should auto-mute
            if (count >= plugin.getConfigManager().getMuteKickThreshold()) {
                plugin.getMuteManager().mutePlayer(uuid, plugin.getConfigManager().getMuteDuration());
                spamKickCount.remove(uuid); // Reset after muting
                
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Auto-muted player " + uuid + " for repeated spam kicks");
                }
            }
        }
        
        lastKickTime.put(uuid, now);
    }
    
    /**
     * Normalize message for comparison
     */
    private String normalizeMessage(String message) {
        return message.toLowerCase()
                .replaceAll("\\s+", " ") // Normalize whitespace
                .replaceAll("[^a-z0-9\\s]", "") // Remove special characters
                .trim();
    }
    
    /**
     * Check if two messages are similar using improved Levenshtein distance
     */
    private boolean isSimilar(String msg1, String msg2) {
        // Quick exact match check
        if (msg1.equals(msg2)) {
            return true;
        }
        
        // If length difference is too large, not similar
        int lengthDiff = Math.abs(msg1.length() - msg2.length());
        int maxLength = Math.max(msg1.length(), msg2.length());
        
        // If one message is more than 30% different in length, probably not spam
        if (lengthDiff > maxLength * 0.3) {
            return false;
        }
        
        // Calculate Levenshtein distance
        int distance = levenshteinDistance(msg1, msg2);
        
        // Calculate similarity percentage
        int similarity = (int) (((double) (maxLength - distance) / maxLength) * 100);
        
        int threshold = plugin.getConfigManager().getSimilarityThreshold();
        
        if (plugin.getConfigManager().isVerboseEnabled() && similarity >= threshold - 10) {
            plugin.getLogger().info("Similarity check: '" + msg1 + "' vs '" + msg2 + "' = " + similarity + "%");
        }
        
        return similarity >= threshold;
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        
        int[][] dp = new int[len1 + 1][len2 + 1];
        
        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[len1][len2];
    }
    
    /**
     * Clear player data when they disconnect
     */
    public void clearPlayerData(UUID uuid) {
        messageHistory.remove(uuid);
        messageTimes.remove(uuid);
        // Don't clear spam kick count - it should persist for auto-mute
    }
    
    /**
     * Data class for storing message information
     */
    private static class MessageData {
        String normalizedMessage;
        long timestamp;
        
        MessageData(String message, long timestamp) {
            this.normalizedMessage = message;
            this.timestamp = timestamp;
        }
    }
}
