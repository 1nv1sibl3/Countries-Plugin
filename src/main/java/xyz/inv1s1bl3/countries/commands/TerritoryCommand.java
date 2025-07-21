package xyz.inv1s1bl3.countries.commands;

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
import xyz.inv1s1bl3.countries.core.territory.Territory;
import xyz.inv1s1bl3.countries.core.territory.TerritoryType;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all territory-related commands.
 * Provides functionality for claiming, managing, and interacting with territories.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class TerritoryCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public TerritoryCommand(@NotNull final CountriesPlugin plugin) {
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
            case "claim":
                this.handleClaimCommand(player, args);
                break;
            case "unclaim":
                this.handleUnclaimCommand(player);
                break;
            case "info":
                this.handleInfoCommand(player, args);
                break;
            case "list":
                this.handleListCommand(player, args);
                break;
            case "delete":
                this.handleDeleteCommand(player, args);
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
        if (!player.hasPermission("countries.territory.create")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/territory create <name> [type]"));
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
        
        final String territoryName = args[1];
        final TerritoryType territoryType = args.length > 2 ? 
            TerritoryType.fromString(args[2]) : TerritoryType.WILDERNESS;
        
        // Check creation cost
        final double creationCost = territoryType.getClaimCost();
        if (country.getBalance() < creationCost) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.insufficient-funds", 
                "amount", String.format("%.2f", creationCost)));
            return;
        }
        
        // Create the territory
        final Territory territory = this.plugin.getTerritoryManager().createTerritory(
            territoryName, country.getName(), player.getUniqueId(), territoryType);
        
        if (territory == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.create-failed", 
                "territory", territoryName, "reason", "Territory may already exist"));
            return;
        }
        
        // Charge the country
        country.withdraw(creationCost);
        
        ChatUtils.sendSuccess(player, this.plugin.getMessage("territory.created", 
            "territory", territoryName, "type", territoryType.getDisplayName()));
    }
    
    /**
     * Handle the claim subcommand.
     */
    private void handleClaimCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.territory.claim")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/territory claim <territory>"));
            return;
        }
        
        final String territoryName = args[1];
        final Territory territory = this.plugin.getTerritoryManager().getTerritory(territoryName);
        
        if (territory == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.not-found", 
                "territory", territoryName));
            return;
        }
        
        // Check if player can manage this territory
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null || !country.getName().equals(territory.getCountryName())) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        // Check if chunk is already claimed
        final Territory existingTerritory = this.plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
        if (existingTerritory != null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.already-claimed"));
            return;
        }
        
        // Claim the chunk
        if (this.plugin.getTerritoryManager().claimChunk(territory, player.getLocation().getChunk())) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("territory.claimed", 
                "territory", territoryName));
        } else {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.claim-failed", 
                "reason", "Check territory limits and country balance"));
        }
    }
    
    /**
     * Handle the unclaim subcommand.
     */
    private void handleUnclaimCommand(@NotNull final Player player) {
        if (!player.hasPermission("countries.territory.unclaim")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Territory territory = this.plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
        if (territory == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.not-claimed"));
            return;
        }
        
        // Check if player can manage this territory
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null || !country.getName().equals(territory.getCountryName())) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        // Unclaim the chunk
        if (this.plugin.getTerritoryManager().unclaimChunk(territory, player.getLocation().getChunk())) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("territory.unclaimed"));
        }
    }
    
    /**
     * Handle the info subcommand.
     */
    private void handleInfoCommand(@NotNull final Player player, @NotNull final String[] args) {
        Territory territory = null;
        
        if (args.length > 1) {
            territory = this.plugin.getTerritoryManager().getTerritory(args[1]);
        } else {
            territory = this.plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
        }
        
        if (territory == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.not-found", 
                "territory", args.length > 1 ? args[1] : "current location"));
            return;
        }
        
        this.sendTerritoryInfo(player, territory);
    }
    
    /**
     * Handle the list subcommand.
     */
    private void handleListCommand(@NotNull final Player player, @NotNull final String[] args) {
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("country.not-member"));
            return;
        }
        
        final List<Territory> territories = this.plugin.getTerritoryManager()
            .getTerritoriesByCountry(country.getName());
        
        if (territories.isEmpty()) {
            ChatUtils.sendInfo(player, "&7Your country has no territories yet!");
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Territories"));
        
        for (final Territory territory : territories) {
            ChatUtils.sendMessage(player, String.format("&e%s &7- &f%s &7(%d chunks)", 
                territory.getName(), territory.getType().getDisplayName(), territory.getChunkCount()));
        }
    }
    
    /**
     * Handle the delete subcommand.
     */
    private void handleDeleteCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.territory.manage")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/territory delete <name>"));
            return;
        }
        
        final String territoryName = args[1];
        final Territory territory = this.plugin.getTerritoryManager().getTerritory(territoryName);
        
        if (territory == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("territory.not-found", 
                "territory", territoryName));
            return;
        }
        
        // Check if player can manage this territory
        final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (country == null || !country.getName().equals(territory.getCountryName())) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final CitizenRole playerRole = country.getCitizenRole(player.getUniqueId());
        if (playerRole == null || playerRole != CitizenRole.LEADER) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (this.plugin.getTerritoryManager().deleteTerritory(territoryName)) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("territory.deleted", 
                "territory", territoryName));
        }
    }
    
    /**
     * Handle the gui subcommand.
     */
    private void handleGuiCommand(@NotNull final Player player) {
        // TODO: Implement GUI opening
        ChatUtils.sendInfo(player, "&7Territory GUI system coming soon!");
    }
    
    /**
     * Send territory information to a player.
     */
    private void sendTerritoryInfo(@NotNull final Player player, @NotNull final Territory territory) {
        final String ownerName = this.plugin.getCountryManager().getPlayerDisplayName(territory.getOwnerId());
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Territory Info"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Name", territory.getName()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Type", territory.getType().getDisplayName()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Country", territory.getCountryName()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Owner", ownerName));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Chunks", String.valueOf(territory.getChunkCount())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Description", territory.getDescription()));
    }
    
    /**
     * Send help message to a player.
     */
    private void sendHelpMessage(@NotNull final Player player) {
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Territory Commands"));
        ChatUtils.sendMessage(player, "&e/territory create <name> [type] &7- Create a territory");
        ChatUtils.sendMessage(player, "&e/territory claim <territory> &7- Claim current chunk");
        ChatUtils.sendMessage(player, "&e/territory unclaim &7- Unclaim current chunk");
        ChatUtils.sendMessage(player, "&e/territory info [name] &7- View territory information");
        ChatUtils.sendMessage(player, "&e/territory list &7- List your country's territories");
        ChatUtils.sendMessage(player, "&e/territory delete <name> &7- Delete a territory");
        ChatUtils.sendMessage(player, "&e/territory gui &7- Open territory GUI");
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
            return Arrays.asList("create", "claim", "unclaim", "info", "list", "delete", "gui")
                .stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            final String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "info":
                case "delete":
                case "claim":
                    final Country country = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
                    if (country != null) {
                        return this.plugin.getTerritoryManager().getTerritoriesByCountry(country.getName())
                            .stream()
                            .map(Territory::getName)
                            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                    }
                    break;
            }
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(TerritoryType.values())
                .map(type -> type.name().toLowerCase())
                .filter(name -> name.startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}