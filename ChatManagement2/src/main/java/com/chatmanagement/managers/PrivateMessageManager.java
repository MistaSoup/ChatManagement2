package com.chatmanagement.managers;

import com.chatmanagement.ChatManagement2;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivateMessageManager {
    
    private final ChatManagement2 plugin;
    private final Map<UUID, UUID> lastMessaged; // sender -> receiver
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
    
    public PrivateMessageManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.lastMessaged = new HashMap<>();
    }
    
    /**
     * Send a private message from one player to another
     * Returns true if message was sent successfully
     */
    public boolean sendPrivateMessage(Player sender, Player receiver, String message) {
        // Check if sender is muted
        if (plugin.getMuteManager().isMuted(sender.getUniqueId())) {
            int remaining = plugin.getMuteManager().getRemainingTime(sender.getUniqueId());
            String muteMsg = plugin.getConfigManager().getMuteMessage()
                    .replace("{time}", String.valueOf(remaining));
            sender.sendMessage(serializer.deserialize(muteMsg));
            return false;
        }
        
        // Check for blocked words
        if (plugin.getBlockedWordsManager().containsBlockedWord(message)) {
            if (plugin.getConfigManager().shouldNotifyBlocked()) {
                sender.sendMessage(serializer.deserialize(plugin.getConfigManager().getBlockedMessageNotification()));
            }
            return false;
        }
        
        // Check for spam (duplicate messages)
        if (plugin.getSpamDetectionManager().isDuplicateSpam(sender, message)) {
            if (plugin.getConfigManager().shouldNotifyBlocked()) {
                sender.sendMessage(serializer.deserialize(plugin.getConfigManager().getBlockedMessageNotification()));
            }
            return false;
        }
        
        // Format messages
        String senderFormat = plugin.getConfigManager().getPMSentFormat()
                .replace("{sender}", sender.getName())
                .replace("{receiver}", receiver.getName())
                .replace("{message}", message);
        
        String receiverFormat = plugin.getConfigManager().getPMReceivedFormat()
                .replace("{sender}", sender.getName())
                .replace("{receiver}", receiver.getName())
                .replace("{message}", message);
        
        // Send messages
        sender.sendMessage(serializer.deserialize(senderFormat));
        
        // Check if receiver can receive messages (if they're muted)
        if (plugin.getMuteManager().isMuted(receiver.getUniqueId())) {
            if (!plugin.getConfigManager().canMutedReceivePM()) {
                sender.sendMessage(serializer.deserialize("&cThat player is currently muted and cannot receive messages."));
                return false;
            }
        }
        
        receiver.sendMessage(serializer.deserialize(receiverFormat));
        
        // Track last messaged for reply functionality
        lastMessaged.put(sender.getUniqueId(), receiver.getUniqueId());
        lastMessaged.put(receiver.getUniqueId(), sender.getUniqueId());
        
        if (plugin.getConfigManager().isVerboseEnabled()) {
            plugin.getLogger().info("PM from " + sender.getName() + " to " + receiver.getName() + ": " + message);
        }
        
        return true;
    }
    
    /**
     * Send a reply to the last person who messaged this player
     * Returns true if reply was sent successfully
     */
    public boolean sendReply(Player sender, String message) {
        UUID lastRecipient = lastMessaged.get(sender.getUniqueId());
        
        if (lastRecipient == null) {
            sender.sendMessage(serializer.deserialize("&cYou have no one to reply to."));
            return false;
        }
        
        Player receiver = Bukkit.getPlayer(lastRecipient);
        
        if (receiver == null || !receiver.isOnline()) {
            sender.sendMessage(serializer.deserialize("&cThat player is no longer online."));
            lastMessaged.remove(sender.getUniqueId());
            return false;
        }
        
        return sendPrivateMessage(sender, receiver, message);
    }
    
    /**
     * Get the last person this player messaged
     */
    public UUID getLastMessaged(UUID uuid) {
        return lastMessaged.get(uuid);
    }
    
    /**
     * Clear conversation data for a player
     */
    public void clearPlayerData(UUID uuid) {
        lastMessaged.remove(uuid);
        
        // Also remove any references to this player from others
        lastMessaged.entrySet().removeIf(entry -> entry.getValue().equals(uuid));
    }
}
