package xyz.inv1s1bl3.countries.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.Map;

/**
 * Utility class for message handling and formatting
 */
public final class MessageUtil {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    
    private MessageUtil() {
        // Utility class
    }
    
    /**
     * Send a message to a command sender
     * @param sender Command sender
     * @param messageKey Message key from messages.yml
     * @param placeholders Placeholders to replace
     */
    public static void sendMessage(final CommandSender sender, final String messageKey, final Map<String, String> placeholders) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        String message = plugin.getConfigManager().getMessageConfig().getString(messageKey, "Message not found: " + messageKey);
        
        // Add prefix if not already present
        if (!message.startsWith("&8[&6Countries&8]") && !messageKey.startsWith("gui.") && !messageKey.startsWith("time.")) {
            final String prefix = plugin.getConfigManager().getMessageConfig().getString("general.prefix", "&8[&6Countries&8]&r");
            message = prefix + " " + message;
        }
        
        // Replace placeholders
        if (placeholders != null) {
            for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        // Convert to component and send
        final Component component = LEGACY_SERIALIZER.deserialize(message);
        sender.sendMessage(component);
    }
    
    /**
     * Send a message to a command sender without placeholders
     * @param sender Command sender
     * @param messageKey Message key from messages.yml
     */
    public static void sendMessage(final CommandSender sender, final String messageKey) {
        sendMessage(sender, messageKey, null);
    }
    
    /**
     * Send a success message
     * @param sender Command sender
     * @param message Message to send
     */
    public static void sendSuccess(final CommandSender sender, final String message) {
        final Component component = Component.text(message)
                .color(NamedTextColor.GREEN);
        sender.sendMessage(component);
    }
    
    /**
     * Send an error message
     * @param sender Command sender
     * @param message Message to send
     */
    public static void sendError(final CommandSender sender, final String message) {
        final Component component = Component.text(message)
                .color(NamedTextColor.RED);
        sender.sendMessage(component);
    }
    
    /**
     * Send a warning message
     * @param sender Command sender
     * @param message Message to send
     */
    public static void sendWarning(final CommandSender sender, final String message) {
        final Component component = Component.text(message)
                .color(NamedTextColor.YELLOW);
        sender.sendMessage(component);
    }
    
    /**
     * Send an info message
     * @param sender Command sender
     * @param message Message to send
     */
    public static void sendInfo(final CommandSender sender, final String message) {
        final Component component = Component.text(message)
                .color(NamedTextColor.AQUA);
        sender.sendMessage(component);
    }
    
    /**
     * Format a message with placeholders
     * @param messageKey Message key from messages.yml
     * @param placeholders Placeholders to replace
     * @return Formatted message
     */
    public static String formatMessage(final String messageKey, final Map<String, String> placeholders) {
        final CountriesPlugin plugin = CountriesPlugin.getInstance();
        String message = plugin.getConfigManager().getMessageConfig().getString(messageKey, "Message not found: " + messageKey);
        
        // Replace placeholders
        if (placeholders != null) {
            for (final Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
    
    /**
     * Create a clickable component
     * @param text Text to display
     * @param command Command to run when clicked
     * @param hover Hover text
     * @return Clickable component
     */
    public static Component createClickableComponent(final String text, final String command, final String hover) {
        Component component = LEGACY_SERIALIZER.deserialize(text);
        
        if (command != null) {
            component = component.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(command));
        }
        
        if (hover != null) {
            component = component.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                    LEGACY_SERIALIZER.deserialize(hover)
            ));
        }
        
        return component;
    }
    
    /**
     * Create a header component
     * @param title Header title
     * @return Header component
     */
    public static Component createHeader(final String title) {
        final String line = "&8&m----------";
        return LEGACY_SERIALIZER.deserialize(line + "&r &6" + title + " " + line);
    }
    
    /**
     * Send a formatted list to a player
     * @param player Player to send to
     * @param title List title
     * @param items List items
     */
    public static void sendList(final Player player, final String title, final java.util.List<String> items) {
        player.sendMessage(createHeader(title));
        
        for (int i = 0; i < items.size(); i++) {
            final Component component = Component.text((i + 1) + ". ")
                    .color(NamedTextColor.GRAY)
                    .append(LEGACY_SERIALIZER.deserialize(items.get(i)));
            player.sendMessage(component);
        }
    }
    
    /**
     * Format time duration
     * @param minutes Duration in minutes
     * @return Formatted time string
     */
    public static String formatTime(final long minutes) {
        if (minutes < 60) {
            return formatMessage("time.minutes", Map.of("time", String.valueOf(minutes)));
        } else if (minutes < 1440) { // Less than 24 hours
            final long hours = minutes / 60;
            return formatMessage("time.hours", Map.of("time", String.valueOf(hours)));
        } else if (minutes < 10080) { // Less than 7 days
            final long days = minutes / 1440;
            return formatMessage("time.days", Map.of("time", String.valueOf(days)));
        } else {
            final long weeks = minutes / 10080;
            return formatMessage("time.weeks", Map.of("time", String.valueOf(weeks)));
        }
    }
    
    /**
     * Format money amount
     * @param amount Money amount
     * @return Formatted money string
     */
    public static String formatMoney(final double amount) {
        if (amount >= 1000000) {
            return String.format("$%.1fM", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("$%.1fK", amount / 1000);
        } else {
            return String.format("$%.2f", amount);
        }
    }
    
    /**
     * Format percentage
     * @param percentage Percentage value
     * @return Formatted percentage string
     */
    public static String formatPercentage(final double percentage) {
        return String.format("%.1f%%", percentage);
    }
    
    /**
     * Send a confirmation message with accept/decline buttons
     * @param player Player to send to
     * @param message Confirmation message
     * @param acceptCommand Command to run on accept
     * @param declineCommand Command to run on decline
     */
    public static void sendConfirmation(final Player player, final String message, final String acceptCommand, final String declineCommand) {
        player.sendMessage(LEGACY_SERIALIZER.deserialize(message));
        
        final Component acceptButton = Component.text("[Accept]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(acceptCommand))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to accept")));
        
        final Component declineButton = Component.text("[Decline]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(declineCommand))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to decline")));
        
        final Component buttons = acceptButton.append(Component.text(" ")).append(declineButton);
        player.sendMessage(buttons);
    }
}