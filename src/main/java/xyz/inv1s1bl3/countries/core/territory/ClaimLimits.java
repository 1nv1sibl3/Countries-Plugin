package xyz.inv1s1bl3.countries.core.territory;

import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;

/**
 * Manages claiming limits based on permission nodes.
 */
public class ClaimLimits {
    
    private final CountriesPlugin plugin;
    
    public ClaimLimits(CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get maximum chunks a player can claim based on permissions
     * Format: countries.chunks.X where X is the number
     */
    public int getMaxChunks(Player player) {
        int maxChunks = plugin.getConfigManager().getConfig().getInt("territory.default-max-chunks", 50);
        
        // Check for permission-based limits
        for (int i = 1; i <= 10000; i *= 10) {
            for (int j = 1; j < 10; j++) {
                int limit = i * j;
                if (player.hasPermission("countries.chunks." + limit)) {
                    maxChunks = Math.max(maxChunks, limit);
                }
            }
        }
        
        // Check for unlimited permission
        if (player.hasPermission("countries.chunks.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        return maxChunks;
    }
    
    /**
     * Get maximum territories a player can create based on permissions
     * Format: countries.territories.X where X is the number
     */
    public int getMaxTerritories(Player player) {
        int maxTerritories = plugin.getConfigManager().getConfig().getInt("territory.default-max-territories", 5);
        
        // Check for permission-based limits
        for (int i = 1; i <= 1000; i *= 10) {
            for (int j = 1; j < 10; j++) {
                int limit = i * j;
                if (player.hasPermission("countries.territories." + limit)) {
                    maxTerritories = Math.max(maxTerritories, limit);
                }
            }
        }
        
        // Check for unlimited permission
        if (player.hasPermission("countries.territories.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        return maxTerritories;
    }
    
    /**
     * Get current chunk usage for a player
     */
    public int getCurrentChunks(Player player) {
        return plugin.getTerritoryManager().getPlayerChunkCount(player.getUniqueId());
    }
    
    /**
     * Get current territory count for a player
     */
    public int getCurrentTerritories(Player player) {
        return plugin.getTerritoryManager().getPlayerTerritoryCount(player.getUniqueId());
    }
    
    /**
     * Check if player can claim more chunks
     */
    public boolean canClaimChunk(Player player) {
        int current = getCurrentChunks(player);
        int max = getMaxChunks(player);
        return current < max;
    }
    
    /**
     * Check if player can create more territories
     */
    public boolean canCreateTerritory(Player player) {
        int current = getCurrentTerritories(player);
        int max = getMaxTerritories(player);
        return current < max;
    }
    
    /**
     * Get remaining chunks a player can claim
     */
    public int getRemainingChunks(Player player) {
        return Math.max(0, getMaxChunks(player) - getCurrentChunks(player));
    }
    
    /**
     * Get remaining territories a player can create
     */
    public int getRemainingTerritories(Player player) {
        return Math.max(0, getMaxTerritories(player) - getCurrentTerritories(player));
    }
}