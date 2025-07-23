package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.territory.Territory;
import xyz.inv1s1bl3.countries.core.territory.TerritoryType;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Handles all territory-related commands.
 */
public class TerritoryCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public TerritoryCommand(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "claim" -> handleClaim(sender, args);
            case "unclaim" -> handleUnclaim(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "type" -> handleType(sender, args);
            case "access" -> handleAccess(sender, args);
            case "flag" -> handleFlag(sender, args);
            case "trust" -> handleTrust(sender, args);
            case "untrust" -> handleUntrust(sender, args);
            case "subarea" -> handleSubArea(sender, args);
            case "rent" -> handleRent(sender, args);
            case "tax" -> handleTax(sender, args);
            case "gui" -> handleGUI(sender, args);
            case "visualize", "borders" -> handleVisualize(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendError(sender, "Unknown subcommand. Use /territory help for available commands.");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can claim territories!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /territory claim <name>");
            return;
        }
        
        String territoryName = args[1];
        
        // Check if player is in a country
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.not-in-country");
            return;
        }
        
        // Check claim cost
        double claimCost = plugin.getConfigManager().getConfig()
                .getDouble("territory.chunk-claim-cost", 100.0);
        
        if (plugin.hasVaultEconomy() && claimCost > 0) {
            if (!plugin.getVaultEconomy().has(player, claimCost)) {
                ChatUtils.sendPrefixedConfigMessage(sender, "territory.insufficient-funds", 
                        ChatUtils.formatCurrency(claimCost));
                return;
            }
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        // Check if chunk is already claimed
        if (plugin.getTerritoryManager().isChunkClaimed(chunk)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.already-claimed");
            return;
        }
        
        // Attempt to claim chunk
        if (plugin.getTerritoryManager().claimChunk(player, territoryName, chunk)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.claimed", territoryName);
            
            if (claimCost > 0) {
                ChatUtils.sendInfo(sender, "Claim cost of " + ChatUtils.formatCurrency(claimCost) + " has been deducted.");
            }
        } else {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.claim-denied");
        }
    }
    
    private void handleUnclaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can unclaim territories!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.unclaim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        // Check if chunk is claimed
        if (!plugin.getTerritoryManager().isChunkClaimed(chunk)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.not-claimed");
            return;
        }
        
        Territory territory = plugin.getTerritoryManager().getTerritoryAt(chunk);
        if (territory == null) {
            ChatUtils.sendError(sender, "Error: Territory data not found!");
            return;
        }
        
        // Attempt to unclaim chunk
        if (plugin.getTerritoryManager().unclaimChunk(player, chunk)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "territory.unclaimed", territory.getName());
        } else {
            ChatUtils.sendError(sender, "Failed to unclaim chunk. You may not have permission.");
        }
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can check territory info!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Territory territory;
        
        if (args.length >= 2) {
            // Show info for specified territory
            territory = plugin.getTerritoryManager().getTerritory(args[1]);
            if (territory == null) {
                ChatUtils.sendError(sender, "Territory '" + args[1] + "' not found!");
                return;
            }
        } else {
            // Show info for current location
            territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
            if (territory == null) {
                ChatUtils.sendInfo(sender, "You are currently in unclaimed wilderness.");
                return;
            }
        }
        
        // Send territory information
        for (String line : territory.getFormattedInfo()) {
            sender.sendMessage(line);
        }
    }
    
    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("countries.territory.list")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Collection<Territory> territories;
        String title;
        
        if (args.length >= 2) {
            // List territories for specified country
            String countryName = args[1];
            Country country = plugin.getCountryManager().getCountry(countryName);
            if (country == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", countryName);
                return;
            }
            
            territories = plugin.getTerritoryManager().getCountryTerritories(countryName);
            title = countryName + "'s Territories";
        } else if (sender instanceof Player player) {
            // List territories for player's country
            Country country = plugin.getCountryManager().getPlayerCountry(player);
            if (country == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
                return;
            }
            
            territories = plugin.getTerritoryManager().getCountryTerritories(country.getName());
            title = "Your Country's Territories";
        } else {
            // List all territories for console
            territories = plugin.getTerritoryManager().getAllTerritories();
            title = "All Territories";
        }
        
        if (territories.isEmpty()) {
            ChatUtils.sendInfo(sender, "No territories found!");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&l" + title + " &7(" + territories.size() + " total)"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Territory territory : territories) {
            String status = territory.isActive() ? "&a●" : "&7●";
            sender.sendMessage(ChatUtils.colorize(status + " &e" + territory.getName() + 
                    " &7(" + territory.getType().getDisplayName() + ", " + 
                    territory.getChunkCount() + " chunks)"));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can delete territories!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.unclaim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /territory delete <name>");
            return;
        }
        
        String territoryName = args[1];
        
        // Check if player is in a country
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Territory territory = plugin.getTerritoryManager().getTerritory(territoryName);
        if (territory == null) {
            ChatUtils.sendError(sender, "Territory '" + territoryName + "' not found!");
            return;
        }
        
        if (!territory.getCountryName().equalsIgnoreCase(country.getName())) {
            ChatUtils.sendError(sender, "You can only delete territories owned by your country!");
            return;
        }
        
        // Attempt to delete territory
        if (plugin.getTerritoryManager().deleteTerritory(territoryName, player.getUniqueId())) {
            ChatUtils.sendSuccess(sender, "Territory '" + territoryName + "' has been deleted!");
        } else {
            ChatUtils.sendError(sender, "Failed to delete territory. You may not have permission.");
        }
    }
    
    private void handleType(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can change territory types!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /territory type <territory> <type>");
            ChatUtils.sendInfo(sender, "Available types: " + 
                    String.join(", ", Arrays.stream(TerritoryType.values())
                            .map(TerritoryType::getDisplayName)
                            .toArray(String[]::new)));
            return;
        }
        
        String territoryName = args[1];
        String typeName = args[2];
        
        Territory territory = plugin.getTerritoryManager().getTerritory(territoryName);
        if (territory == null) {
            ChatUtils.sendError(sender, "Territory '" + territoryName + "' not found!");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            ChatUtils.sendError(sender, "You can only modify territories owned by your country!");
            return;
        }
        
        TerritoryType newType = TerritoryType.fromString(typeName);
        territory.setType(newType);
        
        ChatUtils.sendSuccess(sender, "Territory '" + territoryName + "' type changed to " + newType.getDisplayName() + "!");
    }
    
    private void handleAccess(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage territory access!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /territory access <territory> <public|private>");
            return;
        }
        
        String territoryName = args[1];
        String accessType = args[2].toLowerCase();
        
        Territory territory = plugin.getTerritoryManager().getTerritory(territoryName);
        if (territory == null) {
            ChatUtils.sendError(sender, "Territory '" + territoryName + "' not found!");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            ChatUtils.sendError(sender, "You can only modify territories owned by your country!");
            return;
        }
        
        switch (accessType) {
            case "public" -> {
                territory.setAllowPublicAccess(true);
                ChatUtils.sendSuccess(sender, "Territory '" + territoryName + "' is now public!");
            }
            case "private" -> {
                territory.setAllowPublicAccess(false);
                ChatUtils.sendSuccess(sender, "Territory '" + territoryName + "' is now private!");
            }
            default -> ChatUtils.sendError(sender, "Invalid access type! Use 'public' or 'private'.");
        }
    }
    
    private void handleFlag(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage territory flags!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /territory flag <territory> <flag> <true|false>");
            ChatUtils.sendInfo(sender, "Available flags: " + 
                    String.join(", ", Arrays.stream(TerritoryFlag.values())
                            .map(TerritoryFlag::getDisplayName)
                            .toArray(String[]::new)));
            return;
        }
        
        String territoryName = args[1];
        String flagName = args[2];
        String valueStr = args[3];
        
        TerritoryFlag flag = TerritoryFlag.fromString(flagName);
        boolean value = Boolean.parseBoolean(valueStr);
        
        if (plugin.getTerritoryManager().setTerritoryFlag(player, territoryName, flag, value)) {
            ChatUtils.sendSuccess(sender, "Flag '" + flag.getDisplayName() + "' set to " + value + 
                    " for territory '" + territoryName + "'!");
        } else {
            ChatUtils.sendError(sender, "Failed to set flag. Check permissions and territory name.");
        }
    }
    
    private void handleTrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can trust others!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /territory trust <territory> <player> <role>");
            ChatUtils.sendInfo(sender, "Available roles: " + 
                    String.join(", ", Arrays.stream(TerritoryRole.values())
                            .map(TerritoryRole::getDisplayName)
                            .toArray(String[]::new)));
            return;
        }
        
        String territoryName = args[1];
        String playerName = args[2];
        String roleName = args[3];
        
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = target != null ? target.getUniqueId() : 
                Bukkit.getOfflinePlayer(playerName).getUniqueId();
        
        TerritoryRole role = TerritoryRole.fromString(roleName);
        
        if (plugin.getTerritoryManager().trustPlayer(player, territoryName, targetUUID, role)) {
            ChatUtils.sendSuccess(sender, "Player " + playerName + " trusted as " + 
                    role.getDisplayName() + " in territory '" + territoryName + "'!");
        } else {
            ChatUtils.sendError(sender, "Failed to trust player. Check permissions and names.");
        }
    }
    
    private void handleUntrust(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can untrust others!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /territory untrust <territory> <player>");
            return;
        }
        
        String territoryName = args[1];
        String playerName = args[2];
        
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = target != null ? target.getUniqueId() : 
                Bukkit.getOfflinePlayer(playerName).getUniqueId();
        
        if (plugin.getTerritoryManager().trustPlayer(player, territoryName, targetUUID, TerritoryRole.VISITOR)) {
            ChatUtils.sendSuccess(sender, "Player " + playerName + " untrusted from territory '" + territoryName + "'!");
        } else {
            ChatUtils.sendError(sender, "Failed to untrust player. Check permissions and names.");
        }
    }
    
    private void handleSubArea(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage sub-areas!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /territory subarea <create|delete|list> <territory> [name]");
            return;
        }
        
        String action = args[1].toLowerCase();
        String territoryName = args[2];
        
        switch (action) {
            case "create" -> {
                if (args.length < 4) {
                    ChatUtils.sendError(sender, "Usage: /territory subarea create <territory> <name>");
                    return;
                }
                
                String subAreaName = args[3];
                Location[] selection = plugin.getTerritoryManager().getPlayerSelection(player);
                
                if (selection[0] == null || selection[1] == null) {
                    ChatUtils.sendError(sender, "You need to select two corners first! Use a selection tool.");
                    return;
                }
                
                if (plugin.getTerritoryManager().createSubArea(player, territoryName, subAreaName, 
                        selection[0], selection[1])) {
                    ChatUtils.sendSuccess(sender, "Sub-area '" + subAreaName + "' created in territory '" + territoryName + "'!");
                    plugin.getTerritoryManager().clearPlayerSelection(player);
                } else {
                    ChatUtils.sendError(sender, "Failed to create sub-area. Check permissions and names.");
                }
            }
            case "list" -> {
                Territory territory = plugin.getTerritoryManager().getTerritory(territoryName);
                if (territory == null) {
                    ChatUtils.sendError(sender, "Territory not found!");
                    return;
                }
                
                Map<String, SubArea> subAreas = territory.getSubAreas();
                if (subAreas.isEmpty()) {
                    ChatUtils.sendInfo(sender, "No sub-areas in territory '" + territoryName + "'!");
                    return;
                }
                
                sender.sendMessage(ChatUtils.colorize("&6Sub-areas in " + territoryName + ":"));
                for (SubArea subArea : subAreas.values()) {
                    String rentStatus = subArea.isForRent() ? 
                            (subArea.getCurrentTenant() != null ? "&cRented" : "&aAvailable") : "&7Not for rent";
                    sender.sendMessage(ChatUtils.colorize("  &e" + subArea.getName() + " " + rentStatus));
                }
            }
        }
    }
    
    private void handleRent(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can rent sub-areas!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 3) {
            ChatUtils.sendError(sender, "Usage: /territory rent <territory> <subarea>");
            return;
        }
        
        String territoryName = args[1];
        String subAreaName = args[2];
        
        if (plugin.getTerritoryManager().rentSubArea(player, territoryName, subAreaName)) {
            ChatUtils.sendSuccess(sender, "Successfully rented sub-area '" + subAreaName + "'!");
        } else {
            ChatUtils.sendError(sender, "Failed to rent sub-area. Check availability and balance.");
        }
    }
    
    private void handleTax(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can manage territory taxes!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.claim")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 4) {
            ChatUtils.sendError(sender, "Usage: /territory tax <territory> <rate> <enable|disable>");
            return;
        }
        
        String territoryName = args[1];
        double taxRate;
        boolean enable;
        
        try {
            taxRate = Double.parseDouble(args[2]);
            enable = args[3].equalsIgnoreCase("enable");
        } catch (NumberFormatException e) {
            ChatUtils.sendError(sender, "Invalid tax rate!");
            return;
        }
        
        Territory territory = plugin.getTerritoryManager().getTerritory(territoryName);
        if (territory == null) {
            ChatUtils.sendError(sender, "Territory not found!");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            ChatUtils.sendError(sender, "You can only manage taxes for your country's territories!");
            return;
        }
        
        territory.setTaxRate(taxRate);
        territory.setTaxEnabled(enable);
        
        ChatUtils.sendSuccess(sender, "Territory tax " + (enable ? "enabled" : "disabled") + 
                " with rate " + ChatUtils.formatPercentage(taxRate) + "!");
    }
    
    private void handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can use GUIs!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.gui")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.getTerritoryGUIListener().getTerritoryGUI().openTerritoryManagement(player);
    }
    
    private void handleVisualize(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can visualize borders!");
            return;
        }
        
        if (!sender.hasPermission("countries.territory.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Territory territory;
        
        if (args.length >= 2) {
            territory = plugin.getTerritoryManager().getTerritory(args[1]);
            if (territory == null) {
                ChatUtils.sendError(sender, "Territory '" + args[1] + "' not found!");
                return;
            }
        } else {
            territory = plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
            if (territory == null) {
                ChatUtils.sendError(sender, "No territory at your current location!");
                return;
            }
        }
        
        plugin.getTerritoryManager().getBorderVisualizer().showBorders(player, territory);
        ChatUtils.sendSuccess(sender, "Showing borders for territory: " + territory.getName());
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lTerritory Commands"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&e/territory claim <name> &7- Claim current chunk"));
        sender.sendMessage(ChatUtils.colorize("&e/territory unclaim &7- Unclaim current chunk"));
        sender.sendMessage(ChatUtils.colorize("&e/territory info [name] &7- View territory information"));
        sender.sendMessage(ChatUtils.colorize("&e/territory list [country] &7- List territories"));
        sender.sendMessage(ChatUtils.colorize("&e/territory delete <name> &7- Delete a territory"));
        sender.sendMessage(ChatUtils.colorize("&e/territory type <territory> <type> &7- Change territory type"));
        sender.sendMessage(ChatUtils.colorize("&e/territory access <territory> <public|private> &7- Set access"));
        sender.sendMessage(ChatUtils.colorize("&e/territory flag <territory> <flag> <value> &7- Set territory flags"));
        sender.sendMessage(ChatUtils.colorize("&e/territory trust <territory> <player> <role> &7- Trust player"));
        sender.sendMessage(ChatUtils.colorize("&e/territory untrust <territory> <player> &7- Untrust player"));
        sender.sendMessage(ChatUtils.colorize("&e/territory subarea <create|list> <territory> [name] &7- Manage sub-areas"));
        sender.sendMessage(ChatUtils.colorize("&e/territory rent <territory> <subarea> &7- Rent a sub-area"));
        sender.sendMessage(ChatUtils.colorize("&e/territory tax <territory> <rate> <enable|disable> &7- Set territory tax"));
        sender.sendMessage(ChatUtils.colorize("&e/territory gui &7- Open territory management GUI"));
        sender.sendMessage(ChatUtils.colorize("&e/territory visualize [territory] &7- Show territory borders"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"claim", "unclaim", "info", "list", "delete", "type", "access", 
                                   "flag", "trust", "untrust", "subarea", "rent", "tax", "gui", "visualize", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "info", "delete", "type", "access", "flag", "trust", "untrust", "subarea", "rent", "tax" -> {
                    // Territory names for player's country
                    if (sender instanceof Player player) {
                        Country country = plugin.getCountryManager().getPlayerCountry(player);
                        if (country != null) {
                            for (Territory territory : plugin.getTerritoryManager().getCountryTerritories(country.getName())) {
                                if (territory.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(territory.getName());
                                }
                            }
                        }
                    }
                }
                case "list" -> {
                    // Country names
                    for (String countryName : plugin.getCountryManager().getCountryNames()) {
                        if (countryName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(countryName);
                        }
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "type" -> {
                    // Territory types
                    for (TerritoryType type : TerritoryType.values()) {
                        if (type.getDisplayName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(type.getDisplayName());
                        }
                    }
                }
                case "access" -> {
                    // Access types
                    String[] accessTypes = {"public", "private"};
                    for (String accessType : accessTypes) {
                        if (accessType.startsWith(args[2].toLowerCase())) {
                            completions.add(accessType);
                        }
                    }
                }
                case "flag" -> {
                    // Territory flags
                    for (TerritoryFlag flag : TerritoryFlag.values()) {
                        if (flag.getDisplayName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(flag.getDisplayName());
                        }
                    }
                }
                case "trust", "untrust" -> {
                    // Online players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                }
                case "subarea" -> {
                    String[] subAreaActions = {"create", "delete", "list"};
                    for (String action : subAreaActions) {
                        if (action.startsWith(args[2].toLowerCase())) {
                            completions.add(action);
                        }
                    }
                }
                case "tax" -> {
                    // Tax rate suggestions
                    completions.add("0.05");
                    completions.add("0.10");
                    completions.add("0.15");
                }
            }
        } else if (args.length == 4) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "flag" -> {
                    // Boolean values
                    String[] boolValues = {"true", "false"};
                    for (String value : boolValues) {
                        if (value.startsWith(args[3].toLowerCase())) {
                            completions.add(value);
                        }
                    }
                }
                case "trust" -> {
                    // Territory roles
                    for (TerritoryRole role : TerritoryRole.values()) {
                        if (role.getDisplayName().toLowerCase().startsWith(args[3].toLowerCase())) {
                            completions.add(role.getDisplayName());
                        }
                    }
                }
                case "tax" -> {
                    // Enable/disable
                    String[] enableDisable = {"enable", "disable"};
                    for (String option : enableDisable) {
                        if (option.startsWith(args[3].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}