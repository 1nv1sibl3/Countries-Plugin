package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.territory.Territory;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Handles chunk-related events for territory management.
 * Manages territory protection and player notifications.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class ChunkListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public ChunkListener(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player movement between chunks.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(@NotNull final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        
        // Check if player moved to a different chunk
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) {
            return;
        }
        
        final Territory fromTerritory = this.plugin.getTerritoryManager().getTerritoryAt(event.getFrom());
        final Territory toTerritory = this.plugin.getTerritoryManager().getTerritoryAt(event.getTo());
        
        // Handle territory exit
        if (fromTerritory != null && (toTerritory == null || !fromTerritory.equals(toTerritory))) {
            ChatUtils.sendMessage(player, this.plugin.getMessage("territory.leaving", 
                "territory", fromTerritory.getName()));
        }
        
        // Handle territory entry
        if (toTerritory != null && (fromTerritory == null || !toTerritory.equals(fromTerritory))) {
            ChatUtils.sendMessage(player, this.plugin.getMessage("territory.entering", 
                "territory", toTerritory.getName(), "country", toTerritory.getCountryName()));
        }
    }
    
    /**
     * Handle block breaking in territories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(@NotNull final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Territory territory = this.plugin.getTerritoryManager().getTerritoryAt(event.getBlock().getLocation());
        
        if (territory == null) {
            return; // No territory protection
        }
        
        // Check if player can build in this territory
        if (!this.plugin.getTerritoryManager().canBuild(player, territory)) {
            event.setCancelled(true);
            ChatUtils.sendError(player, this.plugin.getMessage("territory.protected", 
                "country", territory.getCountryName()));
        }
    }
    
    /**
     * Handle block placing in territories.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(@NotNull final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Territory territory = this.plugin.getTerritoryManager().getTerritoryAt(event.getBlock().getLocation());
        
        if (territory == null) {
            return; // No territory protection
        }
        
        // Check if player can build in this territory
        if (!this.plugin.getTerritoryManager().canBuild(player, territory)) {
            event.setCancelled(true);
            ChatUtils.sendError(player, this.plugin.getMessage("territory.protected", 
                "country", territory.getCountryName()));
        }
    }
}