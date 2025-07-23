package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;

/**
 * Handles all country-related commands.
 */
public class CountryCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public CountryCommand(CountriesPlugin plugin) {
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
            case "create" -> handleCreate(sender, args);
            case "info" -> handleInfo(sender, args);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender, args);
            case "invite" -> handleInvite(sender, args);
            case "kick" -> handleKick(sender, args);
            case "promote" -> handlePromote(sender, args);
            case "demote" -> handleDemote(sender, args);
            case "list" -> handleList(sender, args);
            case "gui" -> handleGUI(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                ChatUtils.sendPrefixedConfigMessage(sender, "general.invalid-command");
                sendHelp(sender);
            }
        }
        
        return true;
    }
    
    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can create countries!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.create")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country create <name>");
            return;
        }
        
        String countryName = args[1];
        
        // Check if player already has a country
        if (plugin.getCountryManager().isPlayerCitizen(player.getUniqueId())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.already-member");
            return;
        }
        
        // Check creation cost
        double creationCost = plugin.getConfigManager().getConfig()
                .getDouble("country.creation-cost", 10000.0);
        
        if (plugin.hasVaultEconomy() && creationCost > 0) {
            if (!plugin.getVaultEconomy().has(player, creationCost)) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.insufficient-funds", 
                        ChatUtils.formatCurrency(creationCost));
                return;
            }
        }
        
        // Attempt to create country
        if (plugin.getCountryManager().createCountry(countryName, player.getUniqueId(), player.getName())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.created", countryName);
            
            if (creationCost > 0) {
                ChatUtils.sendInfo(sender, "Creation cost of " + ChatUtils.formatCurrency(creationCost) + " has been deducted.");
            }
        } else {
            // Determine why creation failed
            if (plugin.getCountryManager().getCountry(countryName) != null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.already-exists", countryName);
            } else if (!isValidCountryName(countryName)) {
                ChatUtils.sendError(sender, "Invalid country name! Must be 3-16 characters, letters/numbers only.");
            } else {
                ChatUtils.sendError(sender, "Failed to create country. Please try again.");
            }
        }
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("countries.country.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Country country;
        
        if (args.length >= 2) {
            // Show info for specified country
            country = plugin.getCountryManager().getCountry(args[1]);
            if (country == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", args[1]);
                return;
            }
        } else {
            // Show info for player's country
            if (!(sender instanceof Player player)) {
                ChatUtils.sendError(sender, "Console must specify a country name!");
                return;
            }
            
            country = plugin.getCountryManager().getPlayerCountry(player);
            if (country == null) {
                ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
                return;
            }
        }
        
        // Send country information
        for (String line : country.getFormattedInfo()) {
            sender.sendMessage(line);
        }
        
        // Show citizens if sender is a member
        if (sender instanceof Player player && country.isCitizen(player.getUniqueId())) {
            sender.sendMessage(ChatUtils.colorize("&6Citizens:"));
            for (Citizen citizen : country.getCitizens()) {
                String status = Bukkit.getPlayer(citizen.getPlayerUUID()) != null ? "&a●" : "&7●";
                sender.sendMessage(ChatUtils.colorize("  " + status + " &e" + citizen.getPlayerName() + 
                        " &7(" + citizen.getRole().getDisplayName() + ")"));
            }
        }
    }
    
    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can join countries!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.join")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country join <name>");
            return;
        }
        
        String countryName = args[1];
        
        // Check if player is already in a country
        if (plugin.getCountryManager().isPlayerCitizen(player.getUniqueId())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.already-member");
            return;
        }
        
        // Check if country exists
        Country country = plugin.getCountryManager().getCountry(countryName);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-found", countryName);
            return;
        }
        
        // Check if player has invitation or country has open borders
        if (!country.hasInvitation(player.getUniqueId()) && !country.hasOpenBorders()) {
            ChatUtils.sendError(sender, "You need an invitation to join this country!");
            return;
        }
        
        // Attempt to join
        if (plugin.getCountryManager().acceptInvitation(player.getUniqueId(), player.getName(), countryName)) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.joined", countryName);
            
            // Notify country members
            for (Citizen citizen : country.getCitizens()) {
                Player citizenPlayer = Bukkit.getPlayer(citizen.getPlayerUUID());
                if (citizenPlayer != null && !citizenPlayer.equals(player)) {
                    ChatUtils.sendPrefixedMessage(citizenPlayer, "&e" + player.getName() + "&a has joined the country!");
                }
            }
        } else {
            ChatUtils.sendError(sender, "Failed to join country. It may be full or you may not have permission.");
        }
    }
    
    private void handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can leave countries!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.leave")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        // Check if player is owner
        if (country.isOwner(player.getUniqueId())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.cannot-leave-owner");
            return;
        }
        
        // Leave country
        if (plugin.getCountryManager().removePlayerFromCountry(player.getUniqueId())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.left", country.getName());
            
            // Notify remaining members
            for (Citizen citizen : country.getCitizens()) {
                Player citizenPlayer = Bukkit.getPlayer(citizen.getPlayerUUID());
                if (citizenPlayer != null) {
                    ChatUtils.sendPrefixedMessage(citizenPlayer, "&e" + player.getName() + "&c has left the country!");
                }
            }
        } else {
            ChatUtils.sendError(sender, "Failed to leave country. Please try again.");
        }
    }
    
    private void handleInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can invite others!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.invite")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country invite <player>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.player-not-found", args[1]);
            return;
        }
        
        // Check if target is already in a country
        if (plugin.getCountryManager().isPlayerCitizen(target.getUniqueId())) {
            ChatUtils.sendError(sender, "That player is already in a country!");
            return;
        }
        
        // Send invitation
        if (plugin.getCountryManager().invitePlayer(player.getUniqueId(), target.getUniqueId(), country.getName())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.invite-sent", target.getName());
            ChatUtils.sendPrefixedConfigMessage(target, "country.invited", country.getName());
            ChatUtils.sendInfo(target, "Use '/country join " + country.getName() + "' to accept!");
        } else {
            ChatUtils.sendError(sender, "Failed to send invitation. You may not have permission or the country may be full.");
        }
    }
    
    private void handleKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can kick others!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.kick")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country kick <player>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        String targetName;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Try to find offline player
            targetName = args[1];
            targetUUID = null;
            
            for (Citizen citizen : country.getCitizens()) {
                if (citizen.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetUUID = citizen.getPlayerUUID();
                    break;
                }
            }
            
            if (targetUUID == null) {
                ChatUtils.sendError(sender, "Player not found in your country!");
                return;
            }
        }
        
        // Attempt to kick
        if (plugin.getCountryManager().kickCitizen(player.getUniqueId(), targetUUID, country.getName())) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.kicked-player", targetName);
            
            if (target != null) {
                ChatUtils.sendPrefixedConfigMessage(target, "country.kicked", country.getName());
            }
        } else {
            ChatUtils.sendError(sender, "Failed to kick player. You may not have permission or they may be a higher rank.");
        }
    }
    
    private void handlePromote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can promote others!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.promote")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country promote <player>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = null;
        String targetName = args[1];
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Find offline player
            for (Citizen citizen : country.getCitizens()) {
                if (citizen.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetUUID = citizen.getPlayerUUID();
                    break;
                }
            }
        }
        
        if (targetUUID == null) {
            ChatUtils.sendError(sender, "Player not found in your country!");
            return;
        }
        
        // Attempt to promote
        if (plugin.getCountryManager().promoteCitizen(player.getUniqueId(), targetUUID, country.getName())) {
            Citizen promotedCitizen = country.getCitizen(targetUUID);
            ChatUtils.sendSuccess(sender, targetName + " has been promoted to " + promotedCitizen.getRole().getDisplayName() + "!");
            
            if (target != null) {
                ChatUtils.sendPrefixedConfigMessage(target, "country.promoted", promotedCitizen.getRole().getDisplayName());
            }
        } else {
            ChatUtils.sendError(sender, "Failed to promote player. You may not have permission or they may already be at the highest rank.");
        }
    }
    
    private void handleDemote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can demote others!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.demote")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(sender, "Usage: /country demote <player>");
            return;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            ChatUtils.sendPrefixedConfigMessage(sender, "country.not-member");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID = null;
        String targetName = args[1];
        
        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Find offline player
            for (Citizen citizen : country.getCitizens()) {
                if (citizen.getPlayerName().equalsIgnoreCase(targetName)) {
                    targetUUID = citizen.getPlayerUUID();
                    break;
                }
            }
        }
        
        if (targetUUID == null) {
            ChatUtils.sendError(sender, "Player not found in your country!");
            return;
        }
        
        // Attempt to demote
        if (plugin.getCountryManager().demoteCitizen(player.getUniqueId(), targetUUID, country.getName())) {
            Citizen demotedCitizen = country.getCitizen(targetUUID);
            ChatUtils.sendSuccess(sender, targetName + " has been demoted to " + demotedCitizen.getRole().getDisplayName() + "!");
            
            if (target != null) {
                ChatUtils.sendPrefixedConfigMessage(target, "country.demoted", demotedCitizen.getRole().getDisplayName());
            }
        } else {
            ChatUtils.sendError(sender, "Failed to demote player. You may not have permission or they may already be at the lowest rank.");
        }
    }
    
    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("countries.country.info")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        Set<Country> countries = plugin.getCountryManager().getAllCountries();
        
        if (countries.isEmpty()) {
            ChatUtils.sendInfo(sender, "No countries exist yet!");
            return;
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        sender.sendMessage(ChatUtils.colorize("&6&lCountries List &7(" + countries.size() + " total)"));
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        for (Country country : countries) {
            String status = country.isActive() ? "&a●" : "&7●";
            sender.sendMessage(ChatUtils.colorize(status + " &e" + country.getName() + 
                    " &7(" + country.getCitizenCount() + " citizens, " + 
                    country.getTotalTerritories() + " territories)"));
        }
        
        sender.sendMessage(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private void handleGUI(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.sendError(sender, "Only players can use GUIs!");
            return;
        }
        
        if (!sender.hasPermission("countries.country.gui")) {
            ChatUtils.sendPrefixedConfigMessage(sender, "general.no-permission");
            return;
        }
        
        plugin.getGUIListener().getCountryGUI().openCountryManagement(player);
    }
    
    private void sendHelp(CommandSender sender) {
        ChatUtils.sendPrefixedConfigMessage(sender, "help.header");
        ChatUtils.sendConfigMessage(sender, "help.title");
        ChatUtils.sendPrefixedConfigMessage(sender, "help.header");
        ChatUtils.sendConfigMessage(sender, "help.country-create");
        ChatUtils.sendConfigMessage(sender, "help.country-info");
        ChatUtils.sendConfigMessage(sender, "help.country-join");
        ChatUtils.sendConfigMessage(sender, "help.country-leave");
        ChatUtils.sendConfigMessage(sender, "help.country-invite");
        ChatUtils.sendConfigMessage(sender, "help.country-kick");
        ChatUtils.sendConfigMessage(sender, "help.country-promote");
        ChatUtils.sendConfigMessage(sender, "help.country-demote");
        ChatUtils.sendPrefixedConfigMessage(sender, "help.footer");
    }
    
    private boolean isValidCountryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        int minLength = plugin.getConfigManager().getConfig().getInt("country.min-name-length", 3);
        int maxLength = plugin.getConfigManager().getConfig().getInt("country.max-name-length", 16);
        
        return name.length() >= minLength && name.length() <= maxLength && ChatUtils.isValidName(name);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            String[] subCommands = {"create", "info", "join", "leave", "invite", "kick", "promote", "demote", "list", "gui", "help"};
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "info", "join" -> {
                    // Country names
                    for (String countryName : plugin.getCountryManager().getCountryNames()) {
                        if (countryName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(countryName);
                        }
                    }
                }
                case "invite" -> {
                    // Online players not in a country
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!plugin.getCountryManager().isPlayerCitizen(player.getUniqueId()) &&
                            player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(player.getName());
                        }
                    }
                }
                case "kick", "promote", "demote" -> {
                    // Citizens of sender's country
                    if (sender instanceof Player player) {
                        Country country = plugin.getCountryManager().getPlayerCountry(player);
                        if (country != null) {
                            for (Citizen citizen : country.getCitizens()) {
                                if (citizen.getPlayerName().toLowerCase().startsWith(args[1].toLowerCase()) &&
                                    !citizen.getPlayerUUID().equals(player.getUniqueId())) {
                                    completions.add(citizen.getPlayerName());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return completions;
    }
}