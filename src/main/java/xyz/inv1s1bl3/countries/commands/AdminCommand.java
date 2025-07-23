package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Administrative commands for the Countries plugin.
 */
public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public AdminCommand(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("countries.admin.*")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "debug" -> handleDebug(sender, args);
            case "stats" -> handleStats(sender);
            case "force" -> handleForce(sender, args);
            case "backup" -> handleBackup(sender);
            case "gui" -> handleGUI(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendError(sender, "Unknown subcommand. Use /cadmin help for available commands.");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleReload(CommandSender sender) {
        try {
            plugin.reload();
            ChatUtils.sendPrefixedConfigMessage(sender, "general.reload-success");
        } catch (Exception e) {
            ChatUtils.sendError(sender, "Failed to reload plugin: " + e.getMessage());
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
        }
    }
    
    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean debugEnabled = plugin.getConfigManager().isDebugEnabled();
            ChatUtils.sendInfo(sender, "Debug mode is currently " + (debugEnabled ? "&aenabled" : "&cdisabled"));
            return;
        }
        
        String action = args[1].toLowerCase();
        switch (action) {
            case "on", "enable", "true" -> {
                plugin.getConfigManager().toggleDebug();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    ChatUtils.sendPrefixedConfigMessage(sender, "general.debug-enabled");
                }
            }
            case "off", "disable", "false" -> {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getConfigManager().toggleDebug();
                }
                ChatUtils.sendPrefixedConfigMessage(sender, "general.debug-disabled");
            }
            case "toggle" -> {
                plugin.getConfigManager().toggleDebug();
                boolean enabled = plugin.getConfigManager().isDebugEnabled();
                ChatUtils.sendConfigMessage(sender, enabled ? "general.debug-enabled" : "general.debug-disabled");
            }
            default -> ChatUtils.sendError(sender, "Usage: /cadmin debug <on|off|toggle>");
        }
    }
    
    private void handleStats(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lCountries Plugin Statistics"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        // Country statistics
        Map<String, Object> countryStats = plugin.getCountryManager().getStatistics();
        sender.sendMessage(ChatUtils.colorize("&6Countries:"));
        sender.sendMessage(ChatUtils.colorize("  &7Total: &e" + countryStats.get("total_countries")));
        sender.sendMessage(ChatUtils.colorize("  &7Active: &e" + countryStats.get("active_countries")));
        sender.sendMessage(ChatUtils.colorize("  &7Citizens: &e" + countryStats.get("total_citizens")));
        sender.sendMessage(ChatUtils.colorize("  &7Avg Citizens/Country: &e" + 
                String.format("%.1f", (Double) countryStats.get("average_citizens_per_country"))));
        
        // Territory statistics
        Map<String, Object> territoryStats = plugin.getTerritoryManager().getStatistics();
        sender.sendMessage(ChatUtils.colorize("&6Territories:"));
        sender.sendMessage(ChatUtils.colorize("  &7Total: &e" + territoryStats.get("total_territories")));
        sender.sendMessage(ChatUtils.colorize("  &7Active: &e" + territoryStats.get("active_territories")));
        sender.sendMessage(ChatUtils.colorize("  &7Chunks Claimed: &e" + territoryStats.get("total_chunks_claimed")));
        sender.sendMessage(ChatUtils.colorize("  &7Avg Chunks/Territory: &e" + 
                String.format("%.1f", (Double) territoryStats.get("average_chunks_per_territory"))));
        
        // Economy statistics
        Map<String, Object> economyStats = plugin.getEconomyManager().getStatistics();
        sender.sendMessage(ChatUtils.colorize("&6Economy:"));
        sender.sendMessage(ChatUtils.colorize("  &7Total Accounts: &e" + economyStats.get("total_accounts")));
        sender.sendMessage(ChatUtils.colorize("  &7Player Accounts: &e" + economyStats.get("player_accounts")));
        sender.sendMessage(ChatUtils.colorize("  &7Country Accounts: &e" + economyStats.get("country_accounts")));
        sender.sendMessage(ChatUtils.colorize("  &7Total Player Balance: &e" + 
                ChatUtils.formatCurrency((Double) economyStats.get("total_player_balance"))));
        sender.sendMessage(ChatUtils.colorize("  &7Total Country Balance: &e" + 
                ChatUtils.formatCurrency((Double) economyStats.get("total_country_balance"))));
        
        // Diplomacy statistics
        Map<String, Object> diplomacyStats = plugin.getDiplomacyManager().getStatistics();
        sender.sendMessage(ChatUtils.colorize("&6Diplomacy:"));
        sender.sendMessage(ChatUtils.colorize("  &7Relations: &e" + diplomacyStats.get("total_relations")));
        sender.sendMessage(ChatUtils.colorize("  &7Alliances: &e" + diplomacyStats.get("alliances")));
        sender.sendMessage(ChatUtils.colorize("  &7Wars: &e" + diplomacyStats.get("wars")));
        sender.sendMessage(ChatUtils.colorize("  &7Trade Agreements: &e" + diplomacyStats.get("trade_agreements")));
        
        // Law statistics
        Map<String, Object> lawStats = plugin.getLawSystem().getStatistics();
        sender.sendMessage(ChatUtils.colorize("&6Law & Order:"));
        sender.sendMessage(ChatUtils.colorize("  &7Total Laws: &e" + lawStats.get("total_laws")));
        sender.sendMessage(ChatUtils.colorize("  &7Total Crimes: &e" + lawStats.get("total_crimes")));
        sender.sendMessage(ChatUtils.colorize("  &7Active Crimes: &e" + lawStats.get("active_crimes")));
        sender.sendMessage(ChatUtils.colorize("  &7Jailed Players: &e" + lawStats.get("jailed_players")));
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleForce(CommandSender sender, String[] args) {
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /cadmin force <action> <target> [args...]");
            return;
        }
        
        String action = args[1].toLowerCase();
        String target = args[2];
        
        switch (action) {
            case "delete" -> {
                Country country = plugin.getCountryManager().getCountry(target);
                if (country == null) {
                    ChatUtils.sendError(sender, "Country not found: " + target);
                    return;
                }
                
                if (plugin.getCountryManager().deleteCountry(target, country.getOwnerUUID())) {
                    ChatUtils.sendSuccess(sender, "Force deleted country: " + target);
                } else {
                    ChatUtils.sendError(sender, "Failed to delete country: " + target);
                }
            }
            case "kick" -> {
                if (args.length < 4) {
                    ChatUtils.sendError(sender, "Usage: /cadmin force kick <country> <player>");
                    return;
                }
                
                String playerName = args[3];
                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer == null) {
                    ChatUtils.sendError(sender, "Player not found: " + playerName);
                    return;
                }
                
                if (plugin.getCountryManager().removePlayerFromCountry(targetPlayer.getUniqueId())) {
                    ChatUtils.sendSuccess(sender, "Force kicked " + playerName + " from their country");
                } else {
                    ChatUtils.sendError(sender, "Failed to kick player: " + playerName);
                }
            }
            default -> ChatUtils.sendError(sender, "Unknown force action: " + action);
        }
    }
    
    private void handleBackup(CommandSender sender) {
        ChatUtils.sendInfo(sender, "Starting data backup...");
        plugin.getDataManager().saveAll();
        ChatUtils.sendSuccess(sender, "Data backup completed!");
    }
    
    private void handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can use GUI commands!");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /cadmin gui <country|stats>");
            return;
        }
        
        String guiType = args[1].toLowerCase();
        switch (guiType) {
            case "country" -> {
                // This would open an admin country management GUI
                ChatUtils.sendInfo(player, "Admin country GUI not yet implemented");
            }
            case "stats" -> {
                // This would open a statistics GUI
                ChatUtils.sendInfo(player, "Statistics GUI not yet implemented");
            }
            default -> ChatUtils.sendError(sender, "Unknown GUI type: " + guiType);
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lCountries Admin Commands"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin reload &7- Reload plugin configuration"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin debug <on|off|toggle> &7- Toggle debug mode"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin stats &7- View plugin statistics"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin force <action> <target> &7- Force admin actions"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin backup &7- Force data backup"));
        sender.sendMessage(ChatUtils.colorize("&e/cadmin gui <type> &7- Open admin GUIs"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("countries.admin.*")) {
            return completions;
        }
        
        if (args.length == 1) {
            String[] subCommands = {"reload", "debug", "stats", "force", "backup", "gui", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "debug" -> {
                    String[] debugOptions = {"on", "off", "toggle"};
                    for (String option : debugOptions) {
                        if (option.startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                }
                case "force" -> {
                    String[] forceActions = {"delete", "kick"};
                    for (String action : forceActions) {
                        if (action.startsWith(args[1].toLowerCase())) {
                            completions.add(action);
                        }
                    }
                }
                case "gui" -> {
                    String[] guiTypes = {"country", "stats"};
                    for (String type : guiTypes) {
                        if (type.startsWith(args[1].toLowerCase())) {
                            completions.add(type);
                        }
                    }
                }
            }
        } else if (args.length == 3 && "force".equals(args[0].toLowerCase())) {
            String action = args[1].toLowerCase();
            if ("delete".equals(action)) {
                // Country names
                for (String countryName : plugin.getCountryManager().getCountryNames()) {
                    if (countryName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(countryName);
                    }
                }
            } else if ("kick".equals(action)) {
                // Country names
                for (String countryName : plugin.getCountryManager().getCountryNames()) {
                    if (countryName.toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(countryName);
                    }
                }
            }
        }
        
        return completions;
    }
}