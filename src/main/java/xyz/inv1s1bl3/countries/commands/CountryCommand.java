package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.CitizenRole;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.GovernmentType;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all country-related commands.
 * Provides functionality for creating, managing, and interacting with countries.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class CountryCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public CountryCommand(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, 
                           @NotNull final Command command, 
                           @NotNull final String label, 
                           @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            ChatUtils.sendError(sender, this.plugin.getMessage("general.player-only"));
            return true;
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "create":
                this.handleCreateCommand(player, args);
                break;
            case "info":
                this.handleInfoCommand(player, args);
                break;
            case "join":
                this.handleJoinCommand(player, args);
                break;
            case "leave":
                this.handleLeaveCommand(player);
                break;
            case "invite":
                this.handleInviteCommand(player, args);
                break;
            case "kick":
                this.handleKickCommand(player, args);
                break;
            case "promote":
                this.handlePromoteCommand(player, args);
                break;
            case "demote":
                this.handleDemoteCommand(player, args);
                break;
            case "list":
                this.handleListCommand(player, args);
                break;
            case "gui":
                this.handleGuiCommand(player);
                break;
            default:
                ChatUtils.sendError(player, this.plugin.getMessage("general.unknown-command"));
                break;
        }
        
        return true;
    }
    
    /**
     * Handle the create subcommand.
     */
    private void handleCreateCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.create")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country create <name> [government]"));
            return;
        }
        
        final String countryName = args[1];
        final GovernmentType governmentType = args.length > 2 ? 
            GovernmentType.fromString(args[2]) : 
            GovernmentType.fromString(this.plugin.getConfigManager().getConfigValue("country.default-government", "DEMOCRACY"));
        
        // Check if player is already in a country
        if (this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId()) != null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.already-member"));
            return;
        }
        
        // Check creation cost
        final double creationCost = this.plugin.getConfigManager().getConfigValue("country.creation-cost", 5000.0);
        if (this.plugin.getEconomyManager().getBalance(player.getUniqueId()) < creationCost) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.insufficient-funds", 
                "amount", String.format("%.2f", creationCost)));
            return;
        }
        
        // Create the country
        final Country country = this.plugin.getCountryManager().createCountry(countryName, player.getUniqueId(), governmentType);
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.create-failed", "country", countryName));
            return;
        }
        
        // Charge the player
        this.plugin.getEconomyManager().withdraw(player.getUniqueId(), creationCost);
        
        ChatUtils.sendSuccess(player, this.plugin.getMessage("country.created", "country", countryName));
    }
    
    /**
     * Handle the info subcommand.
     */
    private void handleInfoCommand(@NotNull final Player player, @NotNull final String[] args) {
        String countryName = null;
        
        if (args.length > 1) {
            countryName = args[1];
        } else {
            final Country playerCountry = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
            if (playerCountry != null) {
                countryName = playerCountry.getName();
            }
        }
        
        if (countryName == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getCountry(countryName);
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-found", "country", countryName));
            return;
        }
        
        this.sendCountryInfo(player, country);
    }
    
    /**
     * Handle the join subcommand.
     */
    private void handleJoinCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.join")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country join <name>"));
            return;
        }
        
        final String countryName = args[1];
        final Country country = this.plugin.getCountryManager().getCountry(countryName);
        
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-found", "country", countryName));
            return;
        }
        
        if (this.plugin.getCountryManager().addPlayerToCountry(player.getUniqueId(), countryName, CitizenRole.CITIZEN)) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("country.joined", "country", countryName));
        } else {
            ChatUtils.sendError(player, this.plugin.getMessage("country.already-member"));
        }
    }
    
    /**
     * Handle the leave subcommand.
     */
    private void handleLeaveCommand(@NotNull final Player player) {
        if (!player.hasPermission("countries.country.leave")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        if (player.getUniqueId().equals(country.getLeaderId())) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.cant-leave-leader"));
            return;
        }
        
        if (this.plugin.getCountryManager().removePlayerFromCountry(player.getUniqueId())) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("country.left", "country", country.getName()));
        }
    }
    
    /**
     * Handle the invite subcommand.
     */
    private void handleInviteCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.invite")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country invite <player>"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || !playerRole.canManage(CitizenRole.CITIZEN)) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.player-not-found", "player", args[1]));
            return;
        }
        
        if (this.plugin.getCountryManager().getPlayerCountry(targetPlayer.getUniqueId()) != null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.already-member"));
            return;
        }
        
        // Send invitation
        ChatUtils.sendSuccess(player, this.plugin.getMessage("country.invite-sent", "player", targetPlayer.getName()));
        ChatUtils.sendInfo(targetPlayer, this.plugin.getMessage("country.invite-received", "country", country.getName()));
    }
    
    /**
     * Handle the kick subcommand.
     */
    private void handleKickCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.kick")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country kick <player>"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || !playerRole.canManage(CitizenRole.CITIZEN)) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        if (!country.isCitizen(targetPlayer.getUniqueId())) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        if (this.plugin.getCountryManager().removePlayerFromCountry(targetPlayer.getUniqueId())) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("country.kicked", "player", targetPlayer.getName()));
            
            if (targetPlayer.isOnline()) {
                ChatUtils.sendError(targetPlayer.getPlayer(), this.plugin.getMessage("country.kicked", "country", country.getName()));
            }
        }
    }
    
    /**
     * Handle the promote subcommand.
     */
    private void handlePromoteCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.manage")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country promote <player>"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || playerRole != CitizenRole.LEADER) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        final CitizenRole targetRole = country.getCitizenRole(targetPlayer.getUniqueId());
        
        if (targetRole == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole newRole = targetRole.getNextHigher();
        if (newRole == targetRole) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.already-highest-rank"));
            return;
        }
        
        if (country.promoteCitizen(targetPlayer.getUniqueId(), newRole)) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("country.promoted", 
                "player", targetPlayer.getName(), "role", newRole.getDisplayName()));
            
            if (targetPlayer.isOnline()) {
                ChatUtils.sendSuccess(targetPlayer.getPlayer(), this.plugin.getMessage("country.promoted", 
                    "role", newRole.getDisplayName(), "country", country.getName()));
            }
        }
    }
    
    /**
     * Handle the demote subcommand.
     */
    private void handleDemoteCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.country.manage")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/country demote <player>"));
            return;
        }
        
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || playerRole != CitizenRole.LEADER) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[1]);
        final CitizenRole targetRole = country.getCitizenRole(targetPlayer.getUniqueId());
        
        if (targetRole == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final CitizenRole newRole = targetRole.getNextLower();
        if (newRole == targetRole) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.already-lowest-rank"));
            return;
        }
        
        if (country.demoteCitizen(targetPlayer.getUniqueId(), newRole)) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("country.demoted", 
                "player", targetPlayer.getName(), "role", newRole.getDisplayName()));
            
            if (targetPlayer.isOnline()) {
                ChatUtils.sendWarning(targetPlayer.getPlayer(), this.plugin.getMessage("country.demoted", 
                    "role", newRole.getDisplayName(), "country", country.getName()));
            }
        }
    }
    
    /**
     * Handle the list subcommand.
     */
    private void handleListCommand(@NotNull final Player player, @NotNull final String[] args) {
        final List<Country> countries = new ArrayList<>(this.plugin.getCountryManager().getAllCountries());
        
        if (countries.isEmpty()) {
            ChatUtils.sendInfo(player, "&7No countries exist yet!");
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Countries"));
        
        countries.stream()
            .sorted((c1, c2) -> Integer.compare(c2.getCitizenCount(), c1.getCitizenCount()))
            .limit(10)
            .forEach(country -> {
                final String leaderName = this.plugin.getCountryManager().getPlayerDisplayName(country.getLeaderId());
                ChatUtils.sendMessage(player, String.format("&e%s &7- &f%s &7(%d citizens)", 
                    country.getName(), leaderName, country.getCitizenCount()));
            });
    }
    
    /**
     * Handle the gui subcommand.
     */
    private void handleGuiCommand(@NotNull final Player player) {
        // TODO: Implement GUI opening
        ChatUtils.sendInfo(player, "&7GUI system coming soon!");
    }
    
    /**
     * Send country information to a player.
     */
    private void sendCountryInfo(@NotNull final Player player, @NotNull final Country country) {
        final String leaderName = this.plugin.getCountryManager().getPlayerDisplayName(country.getLeaderId());
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-header", "country", country.getName()));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-leader", "leader", leaderName));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-government", "government", country.getGovernmentType().getDisplayName()));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-citizens", 
            "citizens", String.valueOf(country.getCitizenCount()),
            "max", String.valueOf(this.plugin.getConfigManager().getConfigValue("country.max-citizens", 50))));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-balance", "balance", String.format("%.2f", country.getBalance())));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-territories", "territories", String.valueOf(country.getTerritoryCount())));
        ChatUtils.sendMessage(player, this.plugin.getMessage("country.info-founded", "date", country.getFoundedDate().format(formatter)));
    }
    
    /**
     * Send help message to a player.
     */
    private void sendHelpMessage(@NotNull final Player player) {
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Country Commands"));
        ChatUtils.sendMessage(player, "&e/country create <name> [government] &7- Create a country");
        ChatUtils.sendMessage(player, "&e/country info [name] &7- View country information");
        ChatUtils.sendMessage(player, "&e/country join <name> &7- Join a country");
        ChatUtils.sendMessage(player, "&e/country leave &7- Leave your country");
        ChatUtils.sendMessage(player, "&e/country invite <player> &7- Invite a player");
        ChatUtils.sendMessage(player, "&e/country kick <player> &7- Remove a player");
        ChatUtils.sendMessage(player, "&e/country promote <player> &7- Promote a player");
        ChatUtils.sendMessage(player, "&e/country demote <player> &7- Demote a player");
        ChatUtils.sendMessage(player, "&e/country list &7- List all countries");
        ChatUtils.sendMessage(player, "&e/country gui &7- Open country GUI");
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender sender, 
                                    @NotNull final Command command, 
                                    @NotNull final String alias, 
                                    @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 1) {
            return Arrays.asList("create", "info", "join", "leave", "invite", "kick", "promote", "demote", "list", "gui")
                .stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            final String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "info":
                case "join":
                    return this.plugin.getCountryManager().getAllCountries().stream()
                        .map(Country::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(GovernmentType.values())
                .map(type -> type.name().toLowerCase())
                .filter(name -> name.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}