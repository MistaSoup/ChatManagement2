package com.chatmanagement.listeners;

import com.chatmanagement.ChatManagement2;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class ChatListener implements Listener {
    
    private final ChatManagement2 plugin;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public ChatListener(ChatManagement2 plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        
        // Convert Component to plain text
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Bypass permission check
        if (player.hasPermission(plugin.getConfigManager().getBypassPermission())) {
            // Still apply color prefix if enabled
            if (plugin.getConfigManager().isChatColorsEnabled()) {
                String colored = applyColorPrefix(message);
                if (!colored.equals(message)) {
                    event.message(serializer.deserialize(colored));
                }
            }
            return;
        }
        
        // Check if player is muted
        if (plugin.getMuteManager().isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            int remaining = plugin.getMuteManager().getRemainingTime(player.getUniqueId());
            String muteMsg = plugin.getConfigManager().getMuteMessage()
                    .replace("{time}", String.valueOf(remaining));
            player.sendMessage(serializer.deserialize(muteMsg));
            return;
        }
        
        // Check for rapid spam (kick check)
        if (plugin.getSpamDetectionManager().isRapidSpam(player)) {
            event.setCancelled(true);
            
            // Kick player
            String kickMsg = plugin.getConfigManager().getSpamKickMessage();
            player.kick(serializer.deserialize(kickMsg));
            return;
        }
        
        // Check for blocked words
        if (plugin.getBlockedWordsManager().containsBlockedWord(message)) {
            event.setCancelled(true);
            
            if (plugin.getConfigManager().shouldNotifyBlocked()) {
                player.sendMessage(serializer.deserialize(plugin.getConfigManager().getBlockedMessageNotification()));
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Blocked message from " + player.getName() + " (blocked word): " + message);
            }
            return;
        }
        
        // Check for duplicate spam
        if (plugin.getSpamDetectionManager().isDuplicateSpam(player, message)) {
            event.setCancelled(true);
            
            if (plugin.getConfigManager().shouldNotifyBlocked()) {
                player.sendMessage(serializer.deserialize(plugin.getConfigManager().getBlockedMessageNotification()));
            }
            
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Blocked message from " + player.getName() + " (duplicate): " + message);
            }
            return;
        }
        
        // Apply color prefix if enabled
        if (plugin.getConfigManager().isChatColorsEnabled()) {
            String colored = applyColorPrefix(message);
            if (!colored.equals(message)) {
                event.message(serializer.deserialize(colored));
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Resume mute timer if player was muted
        plugin.getMuteManager().handleReconnect(player.getUniqueId());
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Pause mute timer if player is muted
        plugin.getMuteManager().handleDisconnect(uuid);
        
        // Clear spam detection data
        plugin.getSpamDetectionManager().clearPlayerData(uuid);
        
        // Clear PM conversation data
        plugin.getPrivateMessageManager().clearPlayerData(uuid);
    }
    
    /**
     * Apply color prefix to message if it starts with the configured prefix
     */
    private String applyColorPrefix(String message) {
        String prefix = plugin.getConfigManager().getColorPrefix();
        
        if (message.startsWith(prefix)) {
            // Remove prefix and add color
            String withoutPrefix = message.substring(prefix.length()).trim();
            String color = plugin.getConfigManager().getPrefixColor();
            return color + withoutPrefix;
        }
        
        return message;
    }
}
