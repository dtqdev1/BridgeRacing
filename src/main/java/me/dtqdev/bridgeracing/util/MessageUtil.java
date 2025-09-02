package me.dtqdev.bridgeracing.util;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {

    private final FileConfiguration messagesConfig;
    private final String prefix;

    public MessageUtil(BridgeRacing plugin) {
        this.messagesConfig = plugin.getMessagesConfig();
        this.prefix = ChatColor.translateAlternateColorCodes('&', messagesConfig.getString("prefix", ""));
    }

    // Gửi tin nhắn có prefix
    public void sendMessage(CommandSender target, String path, String... replacements) {
        String message = getMessage(path, replacements);
        if (!message.isEmpty()) {
            target.sendMessage(prefix + message);
        }
    }

    // Lấy tin nhắn thô, không có prefix
    public String getRawMessage(String path, String... replacements) {
        return getMessage(path, replacements);
    }

    // Gửi Title
    public void sendTitle(Player player, String titlePath, String subtitlePath, String... replacements) {
        String title = getMessage(titlePath, replacements);
        String subtitle = getMessage(subtitlePath, replacements);
        player.sendTitle(title, subtitle);
    }
    
    private String getMessage(String path, String... replacements) {
        String message = messagesConfig.getString(path, "&cMissing message: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> messages = messagesConfig.getStringList(path);
        return messages.stream().map(line -> {
            for (int i = 0; i < replacements.length; i += 2) {
                if (i + 1 < replacements.length) {
                    line = line.replace(replacements[i], replacements[i + 1]);
                }
            }
            return ChatColor.translateAlternateColorCodes('&', line);
        }).collect(Collectors.toList());
    }
}