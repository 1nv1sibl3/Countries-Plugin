package xyz.inv1s1bl3.countries.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for handling chat messages and text formatting.
 * Provides methods for sending formatted messages to players and console.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class ChatUtils {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    // Color constants
    public static final String PRIMARY = "&6";
    public static final String SECONDARY = "&e";
    public static final String SUCCESS = "&a";
    public static final String ERROR = "&c";
    public static final String INFO = "&b";
    public static final String WARNING = "&e";
    public static final String RESET = "&r";
    
    private ChatUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Send a formatted message to a command sender.
     * 
     * @param sender the command sender
     * @param message the message to send
     */
    public static void sendMessage(@NotNull final CommandSender sender, @NotNull final String message) {
        if (message.isEmpty()) {
            return;
        }
        
        final Component component = LEGACY_SERIALIZER.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Send multiple formatted messages to a command sender.
     * 
     * @param sender the command sender
     * @param messages the messages to send
     */
    public static void sendMessages(@NotNull final CommandSender sender, @NotNull final List<String> messages) {
        for (final String message : messages) {
            sendMessage(sender, message);
        }
    }
    
    /**
     * Send a formatted message to a player.
     * 
     * @param player the player
     * @param message the message to send
     */
    public static void sendMessage(@NotNull final Player player, @NotNull final String message) {
        sendMessage((CommandSender) player, message);
    }
    
    /**
     * Send multiple formatted messages to a player.
     * 
     * @param player the player
     * @param messages the messages to send
     */
    public static void sendMessages(@NotNull final Player player, @NotNull final List<String> messages) {
        sendMessages((CommandSender) player, messages);
    }
    
    /**
     * Send a success message to a command sender.
     * 
     * @param sender the command sender
     * @param message the message to send
     */
    public static void sendSuccess(@NotNull final CommandSender sender, @NotNull final String message) {
        sendMessage(sender, SUCCESS + message);
    }
    
    /**
     * Send an error message to a command sender.
     * 
     * @param sender the command sender
     * @param message the message to send
     */
    public static void sendError(@NotNull final CommandSender sender, @NotNull final String message) {
        sendMessage(sender, ERROR + message);
    }
    
    /**
     * Send an info message to a command sender.
     * 
     * @param sender the command sender
     * @param message the message to send
     */
    public static void sendInfo(@NotNull final CommandSender sender, @NotNull final String message) {
        sendMessage(sender, INFO + message);
    }
    
    /**
     * Send a warning message to a command sender.
     * 
     * @param sender the command sender
     * @param message the message to send
     */
    public static void sendWarning(@NotNull final CommandSender sender, @NotNull final String message) {
        sendMessage(sender, WARNING + message);
    }
    
    /**
     * Format a message with color codes.
     * 
     * @param message the raw message
     * @return the formatted message
     */
    @NotNull
    public static String format(@NotNull final String message) {
        return message.replace('&', '§');
    }
    
    /**
     * Strip color codes from a message.
     * 
     * @param message the formatted message
     * @return the message without color codes
     */
    @NotNull
    public static String stripColors(@NotNull final String message) {
        return message.replaceAll("§[0-9a-fk-or]", "");
    }
    
    /**
     * Create a formatted header for information displays.
     * 
     * @param title the header title
     * @return the formatted header
     */
    @NotNull
    public static String createHeader(@NotNull final String title) {
        final String border = "&6&l" + "=".repeat(10);
        return border + " " + PRIMARY + "&l" + title + " " + border;
    }
    
    /**
     * Create a formatted list item.
     * 
     * @param label the item label
     * @param value the item value
     * @return the formatted list item
     */
    @NotNull
    public static String createListItem(@NotNull final String label, @NotNull final String value) {
        return SECONDARY + label + ": " + RESET + value;
    }
    
    /**
     * Convert a Component to a legacy string.
     * 
     * @param component the component
     * @return the legacy string representation
     */
    @NotNull
    public static String componentToString(@NotNull final Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Convert a legacy string to a Component.
     * 
     * @param text the legacy text
     * @return the Component representation
     */
    @NotNull
    public static Component stringToComponent(@NotNull final String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }
    
    /**
     * Create a clickable component with hover text.
     * 
     * @param text the display text
     * @param hoverText the hover text (can be null)
     * @param command the command to run when clicked (can be null)
     * @return the interactive component
     */
    @NotNull
    public static Component createInteractiveText(@NotNull final String text, 
                                                  @Nullable final String hoverText, 
                                                  @Nullable final String command) {
        Component component = LEGACY_SERIALIZER.deserialize(text)
            .decoration(TextDecoration.ITALIC, false);
        
        if (hoverText != null) {
            component = component.hoverEvent(LEGACY_SERIALIZER.deserialize(hoverText));
        }
        
        if (command != null) {
            component = component.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command));
        }
        
        return component;
    }
    
    /**
     * Center text within a given width.
     * 
     * @param text the text to center
     * @param width the total width
     * @return the centered text
     */
    @NotNull
    public static String centerText(@NotNull final String text, final int width) {
        final String strippedText = stripColors(text);
        final int textLength = strippedText.length();
        
        if (textLength >= width) {
            return text;
        }
        
        final int padding = (width - textLength) / 2;
        final String spaces = " ".repeat(padding);
        
        return spaces + text;
    }
    
    /**
     * Format a list of strings with proper indentation.
     * 
     * @param items the list items
     * @param indent the indentation string
     * @return the formatted list
     */
    @NotNull
    public static List<String> formatList(@NotNull final List<String> items, @NotNull final String indent) {
        return items.stream()
            .map(item -> indent + "• " + item)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a progress bar representation.
     * 
     * @param current the current value
     * @param max the maximum value
     * @param length the length of the progress bar
     * @return the formatted progress bar
     */
    @NotNull
    public static String createProgressBar(final double current, final double max, final int length) {
        final double percentage = Math.max(0.0, Math.min(1.0, current / max));
        final int filledLength = (int) (length * percentage);
        final int emptyLength = length - filledLength;
        
        return "&a" + "█".repeat(filledLength) + "&7" + "█".repeat(emptyLength) + 
               " &f" + String.format("%.1f%%", percentage * 100);
    }
}