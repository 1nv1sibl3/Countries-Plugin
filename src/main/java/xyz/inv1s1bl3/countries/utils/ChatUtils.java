package xyz.inv1s1bl3.countries.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;

/**
 * Utility class for chat-related operations.
 * Handles message formatting, color codes, and sending messages to players.
 */
public class ChatUtils {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    private static CountriesPlugin plugin = CountriesPlugin.getInstance();
    
    /**
     * Colorize a message using & color codes
     */
    public static String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Convert legacy color codes to Adventure Component
     */
    public static Component toComponent(String message) {
        return LEGACY_SERIALIZER.deserialize(message);
    }
    
    /**
     * Send a message to a command sender
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.sendMessage(toComponent(colorize(message)));
        } else {
            sender.sendMessage(colorize(message));
        }
    }
    
    /**
     * Send a formatted message with prefix to a command sender
     */
    public static void sendPrefixedMessage(CommandSender sender, String message) {
        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        sendMessage(sender, prefix + message);
    }
    
    /**
     * Send a message from the messages config
     */
    public static void sendConfigMessage(CommandSender sender, String key, Object... placeholders) {
        String message = plugin.getConfigManager().getMessage(key, placeholders);
        sendMessage(sender, message);
    }
    
    /**
     * Send a message from the messages config with prefix
     */
    public static void sendPrefixedConfigMessage(CommandSender sender, String key, Object... placeholders) {
        String prefix = plugin.getConfigManager().getMessage("general.prefix");
        String message = plugin.getConfigManager().getMessage(key, placeholders);
        sendMessage(sender, prefix + message);
    }
    
    /**
     * Send a success message (green)
     */
    public static void sendSuccess(CommandSender sender, String message) {
        sendPrefixedMessage(sender, "&a" + message);
    }
    
    /**
     * Send an error message (red)
     */
    public static void sendError(CommandSender sender, String message) {
        sendPrefixedMessage(sender, "&c" + message);
    }
    
    /**
     * Send a warning message (yellow)
     */
    public static void sendWarning(CommandSender sender, String message) {
        sendPrefixedMessage(sender, "&e" + message);
    }
    
    /**
     * Send an info message (aqua)
     */
    public static void sendInfo(CommandSender sender, String message) {
        sendPrefixedMessage(sender, "&b" + message);
    }
    
    /**
     * Check if a string contains only valid characters for names
     */
    public static boolean isValidName(String name) {
        return name.matches("^[a-zA-Z0-9_-]+$");
    }
    
    /**
     * Format a number as currency
     */
    public static String formatCurrency(double amount) {
        String symbol = plugin.getConfigManager().getConfig().getString("economy.currency-symbol", "$");
        return String.format("%s%.2f", symbol, amount);
    }
    
    /**
     * Format a percentage
     */
    public static String formatPercentage(double rate) {
        return String.format("%.1f%%", rate * 100);
    }
    
    /**
     * Create a centered message with a specific width
     */
    public static String centerMessage(String message, int width) {
        if (message.length() >= width) {
            return message;
        }
        
        int spaces = (width - message.length()) / 2;
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < spaces; i++) {
            builder.append(" ");
        }
        
        builder.append(message);
        return builder.toString();
    }
    
    /**
     * Create a header line with a specific character
     */
    public static String createHeader(char character, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(character);
        }
        return builder.toString();
    }
    
    /**
     * Strip color codes from a message
     */
    public static String stripColors(String message) {
        return ChatColor.stripColor(colorize(message));
    }
    
    /**
     * Capitalize the first letter of each word
     */
    public static String capitalizeWords(String input) {
        StringBuilder builder = new StringBuilder();
        boolean nextUpperCase = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c)) {
                nextUpperCase = true;
                builder.append(c);
            } else if (nextUpperCase) {
                builder.append(Character.toUpperCase(c));
                nextUpperCase = false;
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        
        return builder.toString();
    }
    
    /**
     * Format a time duration in milliseconds to a readable string
     */
    public static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d days, %d hours", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
    
    /**
     * Create a progress bar
     */
    public static String createProgressBar(double current, double max, int length, char fillChar, char emptyChar) {
        double percentage = Math.min(current / max, 1.0);
        int filledLength = (int) (percentage * length);
        
        StringBuilder builder = new StringBuilder();
        builder.append("&a");
        
        for (int i = 0; i < filledLength; i++) {
            builder.append(fillChar);
        }
        
        builder.append("&7");
        for (int i = filledLength; i < length; i++) {
            builder.append(emptyChar);
        }
        
        return builder.toString();
    }
}