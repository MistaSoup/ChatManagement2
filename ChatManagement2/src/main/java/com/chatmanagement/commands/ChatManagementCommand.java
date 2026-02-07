package com.chatmanagement.commands;

import com.chatmanagement.ChatManagement2;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ChatManagementCommand implements CommandExecutor {
    
    private final ChatManagement2 plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public ChatManagementCommand(ChatManagement2 plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check permission
        if (!sender.hasPermission(plugin.getConfigManager().getReloadPermission())) {
            sender.sendMessage(serializer.deserialize(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }
        
        // Check arguments
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            case "version":
            case "ver":
                sender.sendMessage(serializer.deserialize("&aChatManagement 2 &7v1.0.0"));
                sender.sendMessage(serializer.deserialize("&7Created for advanced chat moderation"));
                break;
            default:
                sender.sendMessage(serializer.deserialize("&cUnknown subcommand. Use /cm help for help."));
                break;
        }
        
        return true;
    }
    
    /**
     * Handle reload command
     */
    private void handleReload(CommandSender sender) {
        sender.sendMessage(serializer.deserialize("&eReloading ChatManagement 2..."));
        
        try {
            plugin.reload();
            String message = plugin.getConfigManager().getMessage("reload-success");
            if (!message.isEmpty()) {
                sender.sendMessage(serializer.deserialize(message));
            } else {
                sender.sendMessage(serializer.deserialize("&aChatManagement 2 reloaded successfully!"));
            }
        } catch (Exception e) {
            sender.sendMessage(serializer.deserialize("&cError reloading plugin: " + e.getMessage()));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(serializer.deserialize("&8&m------------------&r &aChatManagement 2 &8&m------------------"));
        sender.sendMessage(serializer.deserialize("&a/cm reload &7- Reload the configuration"));
        sender.sendMessage(serializer.deserialize("&a/cm help &7- Show this help message"));
        sender.sendMessage(serializer.deserialize("&a/cm version &7- Show plugin version"));
        sender.sendMessage(serializer.deserialize(""));
        sender.sendMessage(serializer.deserialize("&7Private Message Commands:"));
        sender.sendMessage(serializer.deserialize("&a/msg <player> <message> &7- Send a private message"));
        sender.sendMessage(serializer.deserialize("&a/r <message> &7- Reply to last message"));
        sender.sendMessage(serializer.deserialize("&8&m------------------------------------------------"));
    }
}
