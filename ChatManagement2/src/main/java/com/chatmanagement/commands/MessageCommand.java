package com.chatmanagement.commands;

import com.chatmanagement.ChatManagement2;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageCommand implements CommandExecutor {
    
    private final ChatManagement2 plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public MessageCommand(ChatManagement2 plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Check if PM is enabled
        if (!plugin.getConfigManager().isPMEnabled()) {
            sender.sendMessage(serializer.deserialize("&cPrivate messaging is currently disabled."));
            return true;
        }
        
        // Must be a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(serializer.deserialize("&cThis command can only be used by players."));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check arguments
        if (args.length < 2) {
            sender.sendMessage(serializer.deserialize("&cUsage: /" + label + " <player> <message>"));
            return true;
        }
        
        // Get target player
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null || !target.isOnline()) {
            sender.sendMessage(serializer.deserialize("&cPlayer '" + targetName + "' is not online."));
            return true;
        }
        
        // Can't message yourself
        if (target.equals(player)) {
            sender.sendMessage(serializer.deserialize("&cYou cannot send a message to yourself."));
            return true;
        }
        
        // Build message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();
        
        // Send private message
        plugin.getPrivateMessageManager().sendPrivateMessage(player, target, message);
        
        return true;
    }
}
