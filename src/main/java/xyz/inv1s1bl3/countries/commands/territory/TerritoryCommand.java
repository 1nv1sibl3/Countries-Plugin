package xyz.inv1s1bl3.countries.commands.territory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;
import xyz.inv1s1bl3.countries.territory.TerritoryType;
import xyz.inv1s1bl3.countries.utils.MessageUtil;
import xyz.inv1s1bl3.countries.utils.PermissionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Territory command handler
 */
@RequiredArgsConstructor
public final class TerritoryCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, 
                           @NotNull final String label, @NotNull final String[] args) {
        
        if (args.length == 0) {
            this.sendHelpMessage(sender);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "claim" -> this.handleClaim(sender, args);
            case "unclaim" -> this.handleUnclaim(sender, args);
            case "info" -> this.handleInfo(sender, args);
            case "list" -> this.handleList(sender, args);
            case "type" -> this.handleType(sender, args);
            case "autoclaim" -> this.handleAutoClaim(sender, args);
            case "help" -> this.sendHelpMessage(sender);
            default -> MessageUtil.sendMessage(sender, "general.invalid-command", 
                    Map.of("usage", "/territory help"));
        }
        
        return true;
    }
    
    private void handleClaim(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.canClaimTerritory(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        final String territoryType = args.length > 1 ? args[1] : "residential";
        final Chunk chunk = player.getLocation().getChunk();
        
        try {
            final Territory territory = this.plugin.getTerritoryManager().claimChunk(player, chunk, territoryType);
            
            final Map<String, String> placeholders = Map.of(
                "x", String.valueOf(chunk.getX()),
                "z", String.valueOf(chunk.getZ()),
                "cost", String.format("%.2f", territory.getClaimCost())
            );
            
            MessageUtil.sendMessage(sender, "territory.claim-success", placeholders);
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendMessage(sender, "territory.claim-failed", 
                Map.of("reason", exception.getMessage()));
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "general.database-error");
            this.plugin.getLogger().severe("Error claiming territory: " + exception.getMessage());
        }
    }
    
    private void handleUnclaim(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!PermissionUtil.canUnclaimTerritory(player)) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        final Chunk chunk = player.getLocation().getChunk();
        
        try {
            this.plugin.getTerritoryManager().unclaimChunk(player, chunk);
            
            final Map<String, String> placeholders = Map.of(
                "x", String.valueOf(chunk.getX()),
                "z", String.valueOf(chunk.getZ())
            );
            
            MessageUtil.sendMessage(sender, "territory.unclaim-success", placeholders);
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendMessage(sender, "territory.unclaim-failed", 
                Map.of("reason", exception.getMessage()));
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "general.database-error");
            this.plugin.getLogger().severe("Error unclaiming territory: " + exception.getMessage());
        }
    }
    
    private void handleInfo(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Chunk chunk = player.getLocation().getChunk();
        final Map<String, String> chunkInfo = this.plugin.getTerritoryManager().getChunkInfo(chunk);
        
        if (chunkInfo.get("status").equals("unclaimed")) {
            MessageUtil.sendMessage(sender, "territory.info-unclaimed");
            MessageUtil.sendMessage(sender, "territory.info-price", 
                Map.of("price", chunkInfo.get("price")));
        } else {
            MessageUtil.sendMessage(sender, "territory.info-claimed", 
                Map.of("country", chunkInfo.get("country")));
            MessageUtil.sendMessage(sender, "territory.info-type", 
                Map.of("type", chunkInfo.get("type")));
        }
        
        // Show protection status
        final String protectionStatus = this.plugin.getTerritoryManager().getProtectionStatus(player.getLocation());
        MessageUtil.sendInfo(sender, protectionStatus);
    }
    
    private void handleList(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(player.getUniqueId());
        
        if (playerDataOpt.isEmpty() || !playerDataOpt.get().hasCountry()) {
            MessageUtil.sendMessage(sender, "country.not-in-country");
            return;
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        final List<Territory> territories = this.plugin.getTerritoryManager().getCountryTerritories(playerData.getCountryId());
        
        if (territories.isEmpty()) {
            MessageUtil.sendInfo(sender, "Your country has no claimed territories.");
            return;
        }
        
        MessageUtil.sendInfo(sender, "Country Territories (" + territories.size() + "):");
        for (final Territory territory : territories) {
            final String info = String.format("- %s (%d, %d) - %s", 
                territory.getWorldName(), 
                territory.getChunkX(), 
                territory.getChunkZ(),
                territory.getTerritoryType());
            MessageUtil.sendInfo(sender, info);
        }
    }
    
    private void handleType(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: /territory type <type>");
            return;
        }
        
        final String newType = args[1];
        final Optional<Territory> territoryOpt = this.plugin.getTerritoryManager().getTerritoryAt(player.getLocation());
        
        if (territoryOpt.isEmpty()) {
            MessageUtil.sendMessage(sender, "territory.unclaim-not-claimed");
            return;
        }
        
        final Territory territory = territoryOpt.get();
        
        try {
            this.plugin.getTerritoryManager().changeTerritoryType(player, territory, newType);
            MessageUtil.sendSuccess(sender, "Territory type changed to " + newType + "!");
            
        } catch (final IllegalArgumentException exception) {
            MessageUtil.sendError(sender, exception.getMessage());
        } catch (final Exception exception) {
            MessageUtil.sendMessage(sender, "general.database-error");
            this.plugin.getLogger().severe("Error changing territory type: " + exception.getMessage());
        }
    }
    
    private void handleAutoClaim(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "This command can only be used by players!");
            return;
        }
        
        if (!player.hasPermission("countries.territory.autoclaim")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }
        
        final boolean newStatus = this.plugin.getTerritoryManager().toggleAutoClaim(player);
        
        if (newStatus) {
            MessageUtil.sendMessage(sender, "territory.auto-claim-enabled");
        } else {
            MessageUtil.sendMessage(sender, "territory.auto-claim-disabled");
        }
    }
    
    private void sendHelpMessage(final CommandSender sender) {
        MessageUtil.sendInfo(sender, "Territory Commands:");
        MessageUtil.sendInfo(sender, "/territory claim [type] - Claim current chunk");
        MessageUtil.sendInfo(sender, "/territory unclaim - Unclaim current chunk");
        MessageUtil.sendInfo(sender, "/territory info - View chunk information");
        MessageUtil.sendInfo(sender, "/territory list - List your country's territories");
        MessageUtil.sendInfo(sender, "/territory type <type> - Change territory type");
        MessageUtil.sendInfo(sender, "/territory autoclaim - Toggle auto-claiming");
        MessageUtil.sendInfo(sender, "");
        MessageUtil.sendInfo(sender, "Territory Types:");
        for (final TerritoryType type : TerritoryType.values()) {
            MessageUtil.sendInfo(sender, "- " + type.getKey() + " ($" + type.getCreationCost() + ")");
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, 
                                               @NotNull final String alias, @NotNull final String[] args) {
        
        if (args.length == 1) {
            return Arrays.asList("claim", "unclaim", "info", "list", "type", "autoclaim", "help")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "claim", "type" -> {
                    return Arrays.stream(TerritoryType.values())
                        .map(TerritoryType::getKey)
                        .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return new ArrayList<>();
    }
}