package com.chatmanagement.commands;

import com.chatmanagement.ChatManagement2;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReplyCommand implements CommandExecutor {
    
    private final ChatManagement2 plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public ReplyCommand(ChatManagement2 plugin) {
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
        if (args.length < 1) {
            sender.sendMessage(serializer.deserialize("&cUsage: /" + label + " <message>"));
            return true;
        }
        
        // Build message
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(args[i]);
        }
        String message = messageBuilder.toString();
        
        // Send reply
        plugin.getPrivateMessageManager().sendReply(player, message);
        
        return true;
    }
}
