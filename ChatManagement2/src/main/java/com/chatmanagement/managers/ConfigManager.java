package com.chatmanagement.managers;

import com.chatmanagement.ChatManagement2;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {
    
    private final ChatManagement2 plugin;
    private FileConfiguration config;
    
    public ConfigManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }
    
    public void reload() {
        this.config = plugin.getConfig();
    }
    
    // General Settings
    public int getMessageHistorySize() {
        return config.getInt("settings.message-history-size", 10);
    }
    
    public int getSimilarityThreshold() {
        return config.getInt("settings.similarity-threshold", 80);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }
    
    public boolean isVerboseEnabled() {
        return config.getBoolean("settings.verbose", false);
    }
    
    // Database Settings
    public boolean isDatabaseEnabled() {
        return config.getBoolean("database.enabled", false);
    }
    
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }
    
    public String getDatabaseHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    public int getDatabasePort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    public String getDatabaseName() {
        return config.getString("database.mysql.database", "chatmanagement");
    }
    
    public String getDatabaseUsername() {
        return config.getString("database.mysql.username", "root");
    }
    
    public String getDatabasePassword() {
        return config.getString("database.mysql.password", "");
    }
    
    // Duplicate Message Detection
    public int getMaxRepeats() {
        return config.getInt("duplicate-messages.max-repeats", 2);
    }
    
    public int getDuplicateCooldown() {
        return config.getInt("duplicate-messages.cooldown-seconds", 30);
    }
    
    public int getMinMessageLength() {
        return config.getInt("duplicate-messages.min-message-length", 3);
    }
    
    // Blocked Words
    public boolean isBlockedWordsEnabled() {
        return config.getBoolean("blocked-words.enabled", true);
    }
    
    public List<String> getBlockedWords() {
        return config.getStringList("blocked-words.word-list");
    }
    
    public boolean shouldBlockPartialMatches() {
        return config.getBoolean("blocked-words.block-partial-matches", false);
    }
    
    public int getMinWordLength() {
        return config.getInt("blocked-words.min-word-length", 4);
    }
    
    // Anti-Spam Kick
    public boolean isAntiSpamKickEnabled() {
        return config.getBoolean("anti-spam-kick.enabled", true);
    }
    
    public int getSpamMessageThreshold() {
        return config.getInt("anti-spam-kick.message-threshold", 7);
    }
    
    public int getSpamTimeWindow() {
        return config.getInt("anti-spam-kick.time-window-seconds", 5);
    }
    
    public String getSpamKickMessage() {
        return config.getString("anti-spam-kick.kick-message", "&cYou have been kicked for spamming!");
    }
    
    public boolean shouldNotifySpamKick() {
        return config.getBoolean("anti-spam-kick.notify-player", true);
    }
    
    // Auto-Mute
    public boolean isAutoMuteEnabled() {
        return config.getBoolean("auto-mute.enabled", true);
    }
    
    public int getMuteKickThreshold() {
        return config.getInt("auto-mute.kick-threshold", 3);
    }
    
    public int getMuteKickWindow() {
        return config.getInt("auto-mute.kick-window-minutes", 10);
    }
    
    public int getMuteDuration() {
        return config.getInt("auto-mute.mute-duration-seconds", 300);
    }
    
    public String getMuteMessage() {
        return config.getString("auto-mute.mute-message", "&cYou are muted for spamming. Time remaining: &e{time} &cseconds.");
    }
    
    public boolean canMutedReceivePM() {
        return config.getBoolean("auto-mute.allow-receive-pm", true);
    }
    
    public boolean shouldNotifyMute() {
        return config.getBoolean("auto-mute.notify-player", true);
    }
    
    public String getMuteNotification() {
        return config.getString("auto-mute.mute-notification", "&cYou have been muted for {duration} seconds for repeated spamming.");
    }
    
    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }
    
    public boolean shouldNotifyBlocked() {
        return config.getBoolean("messages.notify-blocked-message", false);
    }
    
    public String getBlockedMessageNotification() {
        return config.getString("messages.blocked-message-notification", "&cYour message was blocked.");
    }
    
    // Private Messaging
    public boolean isPMEnabled() {
        return config.getBoolean("private-messaging.enabled", true);
    }
    
    public String getPMColor() {
        return config.getString("private-messaging.message-color", "&d");
    }
    
    public List<String> getPMCommands() {
        return config.getStringList("private-messaging.commands");
    }
    
    public String getPMSentFormat() {
        return config.getString("private-messaging.sent-format", "&7[&dYou &7-> &d{receiver}&7] &r{message}");
    }
    
    public String getPMReceivedFormat() {
        return config.getString("private-messaging.received-format", "&7[&d{sender} &7-> &dYou&7] &r{message}");
    }
    
    // Chat Colors
    public boolean isChatColorsEnabled() {
        return config.getBoolean("chat-colors.enabled", true);
    }
    
    public String getColorPrefix() {
        return config.getString("chat-colors.color-prefix", ">");
    }
    
    public String getPrefixColor() {
        return config.getString("chat-colors.prefix-color", "&a");
    }
    
    // Permissions
    public String getBypassPermission() {
        return "chatmanagement.bypass";
    }
    
    public String getReloadPermission() {
        return "chatmanagement.reload";
    }
}
