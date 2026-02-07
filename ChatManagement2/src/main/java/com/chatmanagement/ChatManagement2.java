package com.chatmanagement;

import com.chatmanagement.commands.ChatManagementCommand;
import com.chatmanagement.commands.MessageCommand;
import com.chatmanagement.commands.ReplyCommand;
import com.chatmanagement.listeners.ChatListener;
import com.chatmanagement.managers.*;
import com.chatmanagement.storage.DatabaseManager;
import com.chatmanagement.storage.YAMLStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatManagement2 extends JavaPlugin {
    
    private static ChatManagement2 instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private YAMLStorage yamlStorage;
    private SpamDetectionManager spamDetectionManager;
    private MuteManager muteManager;
    private PrivateMessageManager privateMessageManager;
    private BlockedWordsManager blockedWordsManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        configManager = new ConfigManager(this);
        
        // Initialize storage based on config
        if (configManager.isDatabaseEnabled()) {
            databaseManager = new DatabaseManager(this);
            if (databaseManager.connect()) {
                getLogger().info("Database connection established successfully");
            } else {
                getLogger().warning("Failed to connect to database, falling back to YAML storage");
                yamlStorage = new YAMLStorage(this);
            }
        } else {
            yamlStorage = new YAMLStorage(this);
        }
        
        // Initialize other managers
        blockedWordsManager = new BlockedWordsManager(this);
        muteManager = new MuteManager(this);
        spamDetectionManager = new SpamDetectionManager(this);
        privateMessageManager = new PrivateMessageManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Register commands
        getCommand("chatmanagement").setExecutor(new ChatManagementCommand(this));
        getCommand("cm").setExecutor(new ChatManagementCommand(this));
        
        // Register PM commands dynamically
        for (String cmd : configManager.getPMCommands()) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(new MessageCommand(this));
            }
        }
        
        // Register reply commands
        if (getCommand("r") != null) {
            getCommand("r").setExecutor(new ReplyCommand(this));
        }
        if (getCommand("reply") != null) {
            getCommand("reply").setExecutor(new ReplyCommand(this));
        }
        
        getLogger().info("ChatManagement 2 has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save all data before shutdown
        if (muteManager != null) {
            muteManager.saveMutes();
        }
        
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("ChatManagement 2 has been disabled!");
    }
    
    public void reload() {
        // Reload config
        reloadConfig();
        configManager.reload();
        
        // Reload managers
        blockedWordsManager.reload();
        spamDetectionManager.reload();
        
        // Reload mutes from storage
        if (databaseManager != null && databaseManager.isConnected()) {
            muteManager.loadMutesFromDatabase();
        } else if (yamlStorage != null) {
            muteManager.loadMutesFromYAML();
        }
    }
    
    // Getters
    public static ChatManagement2 getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public YAMLStorage getYAMLStorage() {
        return yamlStorage;
    }
    
    public SpamDetectionManager getSpamDetectionManager() {
        return spamDetectionManager;
    }
    
    public MuteManager getMuteManager() {
        return muteManager;
    }
    
    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }
    
    public BlockedWordsManager getBlockedWordsManager() {
        return blockedWordsManager;
    }
}
