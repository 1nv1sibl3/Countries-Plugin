package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.territory.Territory;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

/**
 * Handles chunk-related events for territory protection.
 */
public class ChunkListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public ChunkListener(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        var player = event.getPlayer();
        var location = event.getBlock().getLocation();
        
        // Check if protection is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("territory.enable-protection", true)) {
            return;
        }
        
        // Check if player can build at this location
        if (!plugin.getTerritoryManager().canPlayerBuild(player, location)) {
            Territory territory = plugin.getTerritoryManager().getTerritoryAt(location);
            if (territory != null) {
                ChatUtils.sendPrefixedConfigMessage(player, "territory.protected", territory.getCountryName());
            } else {
                ChatUtils.sendError(player, "You cannot build in this area!");
            }
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        var player = event.getPlayer();
        var location = event.getBlock().getLocation();
        
        // Check if protection is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("territory.enable-protection", true)) {
            return;
        }
        
        // Check if player can build at this location
        if (!plugin.getTerritoryManager().canPlayerBuild(player, location)) {
            Territory territory = plugin.getTerritoryManager().getTerritoryAt(location);
            if (territory != null) {
                ChatUtils.sendPrefixedConfigMessage(player, "territory.protected", territory.getCountryName());
            } else {
                ChatUtils.sendError(player, "You cannot build in this area!");
            }
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        var player = event.getPlayer();
        var block = event.getClickedBlock();
        
        if (block == null) return;
        
        var location = block.getLocation();
        
        // Check if protection is enabled
        if (!plugin.getConfigManager().getConfig().getBoolean("territory.enable-protection", true)) {
            return;
        }
        
        // Check if player can access this location
        if (!plugin.getTerritoryManager().canPlayerAccess(player, location)) {
            Territory territory = plugin.getTerritoryManager().getTerritoryAt(location);
            if (territory != null) {
                ChatUtils.sendPrefixedConfigMessage(player, "territory.protected", territory.getCountryName());
            } else {
                ChatUtils.sendError(player, "You cannot access this area!");
            }
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var from = event.getFrom();
        var to = event.getTo();
        
        if (to == null) return;
        
        // Check if player moved to a different chunk
        if (from.getChunk().equals(to.getChunk())) {
            return;
        }
        
        Territory fromTerritory = plugin.getTerritoryManager().getTerritoryAt(from);
        Territory toTerritory = plugin.getTerritoryManager().getTerritoryAt(to);
        
        // Show territory borders if enabled
        if (plugin.getConfigManager().getConfig().getBoolean("territory.show-borders", true)) {
            showTerritoryInfo(player, fromTerritory, toTerritory);
        }
        
        // Update territory visitor count
        if (toTerritory != null) {
            toTerritory.incrementVisitors();
        }
    }
    
    /**
     * Show territory information when crossing borders
     */
    private void showTerritoryInfo(org.bukkit.entity.Player player, Territory from, Territory to) {
        if (from == to) return; // Same territory
        
        if (to == null) {
            // Entering wilderness
            if (from != null) {
                ChatUtils.sendInfo(player, "Leaving " + from.getName() + " (" + from.getCountryName() + ")");
                ChatUtils.sendInfo(player, "Entering wilderness");
            }
        } else {
            // Entering a territory
            if (from == null) {
                ChatUtils.sendInfo(player, "Leaving wilderness");
            } else {
                ChatUtils.sendInfo(player, "Leaving " + from.getName() + " (" + from.getCountryName() + ")");
            }
            
            ChatUtils.sendInfo(player, "Entering " + to.getName() + " (" + to.getCountryName() + ")");
            
            // Show access status
            if (!to.allowsPublicAccess()) {
                if (plugin.getTerritoryManager().canPlayerAccess(player, player.getLocation())) {
                    ChatUtils.sendInfo(player, "&aYou have access to this territory");
                } else {
                    ChatUtils.sendWarning(player, "&cThis is a private territory");
                }
            }
        }
    }
}