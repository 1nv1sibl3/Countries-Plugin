package xyz.inv1s1bl3.countries.territory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages chunk-related operations and caching
 */
@RequiredArgsConstructor
public final class ChunkManager {
    
    private final CountriesPlugin plugin;
    private final TerritoryService territoryService;
    
    // Cache for territory lookups
    private final Map<String, Territory> territoryCache = new ConcurrentHashMap<>();
    
    // Auto-claim tracking
    private final Map<UUID, Boolean> autoClaimEnabled = new ConcurrentHashMap<>();
    
    /**
     * Initialize the chunk manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing Chunk Manager...");
        
        // Load territories into cache
        this.loadTerritoryCache();
        
        this.plugin.getLogger().info("Chunk Manager initialized successfully!");
    }
    
    /**
     * Load territories into cache
     */
    private void loadTerritoryCache() {
        try {
            // This would load all territories, but we'll implement lazy loading for now
            this.plugin.getLogger().info("Territory cache initialized with lazy loading");
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to load territory cache", exception);
        }
    }
    
    /**
     * Get territory at a chunk
     * @param chunk Chunk to check
     * @return Territory if claimed, empty otherwise
     */
    public Optional<Territory> getTerritoryAt(final Chunk chunk) {
        final String chunkKey = this.getChunkKey(chunk);
        
        // Check cache first
        final Territory cachedTerritory = this.territoryCache.get(chunkKey);
        if (cachedTerritory != null) {
            return Optional.of(cachedTerritory);
        }
        
        // Load from database
        try {
            final Optional<Territory> territoryOpt = this.territoryService.getTerritoryAt(
                new Location(chunk.getWorld(), chunk.getX() * 16, 64, chunk.getZ() * 16)
            );
            
            if (territoryOpt.isPresent()) {
                this.territoryCache.put(chunkKey, territoryOpt.get());
            }
            
            return territoryOpt;
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting territory", exception);
            return Optional.empty();
        }
    }
    
    /**
     * Claim a chunk
     * @param player Player claiming the chunk
     * @param chunk Chunk to claim
     * @param territoryType Territory type
     * @return Claimed territory
     * @throws IllegalArgumentException if claim fails
     */
    public Territory claimChunk(final Player player, final Chunk chunk, final String territoryType) {
        try {
            final Territory territory = this.territoryService.claimChunk(
                player.getUniqueId(), chunk, territoryType
            );
            
            // Update cache
            final String chunkKey = this.getChunkKey(chunk);
            this.territoryCache.put(chunkKey, territory);
            
            return territory;
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while claiming chunk", exception);
            throw new IllegalArgumentException("Database error occurred while claiming chunk");
        }
    }
    
    /**
     * Unclaim a chunk
     * @param player Player unclaiming the chunk
     * @param chunk Chunk to unclaim
     * @throws IllegalArgumentException if unclaim fails
     */
    public void unclaimChunk(final Player player, final Chunk chunk) {
        try {
            this.territoryService.unclaimChunk(player.getUniqueId(), chunk);
            
            // Remove from cache
            final String chunkKey = this.getChunkKey(chunk);
            this.territoryCache.remove(chunkKey);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while unclaiming chunk", exception);
            throw new IllegalArgumentException("Database error occurred while unclaiming chunk");
        }
    }
    
    /**
     * Check if a player can build at a location
     * @param player Player to check
     * @param location Location to check
     * @return true if player can build
     */
    public boolean canPlayerBuild(final Player player, final Location location) {
        try {
            return this.territoryService.canPlayerBuild(player.getUniqueId(), location);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while checking build permission", exception);
            return false;
        }
    }
    
    /**
     * Toggle auto-claim for a player
     * @param player Player to toggle auto-claim for
     * @return New auto-claim status
     */
    public boolean toggleAutoClaim(final Player player) {
        final boolean currentStatus = this.autoClaimEnabled.getOrDefault(player.getUniqueId(), false);
        final boolean newStatus = !currentStatus;
        
        this.autoClaimEnabled.put(player.getUniqueId(), newStatus);
        return newStatus;
    }
    
    /**
     * Check if auto-claim is enabled for a player
     * @param player Player to check
     * @return true if auto-claim is enabled
     */
    public boolean isAutoClaimEnabled(final Player player) {
        return this.autoClaimEnabled.getOrDefault(player.getUniqueId(), false);
    }
    
    /**
     * Handle player movement for auto-claiming
     * @param player Player who moved
     * @param to Location moved to
     */
    public void handlePlayerMovement(final Player player, final Location to) {
        if (!this.isAutoClaimEnabled(player)) {
            return;
        }
        
        if (!player.hasPermission("countries.territory.autoclaim")) {
            return;
        }
        
        final Chunk chunk = to.getChunk();
        final Optional<Territory> territoryOpt = this.getTerritoryAt(chunk);
        
        // If chunk is unclaimed, try to claim it
        if (territoryOpt.isEmpty()) {
            try {
                final Territory territory = this.claimChunk(player, chunk, "residential");
                
                // Notify player
                final Map<String, String> placeholders = Map.of(
                    "x", String.valueOf(chunk.getX()),
                    "z", String.valueOf(chunk.getZ()),
                    "cost", String.format("%.2f", territory.getClaimCost())
                );
                
                xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(
                    player, "territory.auto-claim-chunk", placeholders
                );
                
            } catch (final IllegalArgumentException exception) {
                // Silently fail auto-claim if not possible
                // Could be due to insufficient funds, chunk limit, etc.
            }
        }
    }
    
    /**
     * Get chunk information for display
     * @param chunk Chunk to get info for
     * @return Map of information
     */
    public Map<String, String> getChunkInfo(final Chunk chunk) {
        final Map<String, String> info = new HashMap<>();
        final Optional<Territory> territoryOpt = this.getTerritoryAt(chunk);
        
        if (territoryOpt.isEmpty()) {
            info.put("status", "unclaimed");
            info.put("price", String.format("%.2f", this.plugin.getConfigManager().getBaseChunkPrice()));
        } else {
            final Territory territory = territoryOpt.get();
            final Optional<xyz.inv1s1bl3.countries.database.entities.Country> countryOpt = 
                this.plugin.getCountryManager().getCountry(territory.getCountryId());
            
            info.put("status", "claimed");
            info.put("country", countryOpt.map(xyz.inv1s1bl3.countries.database.entities.Country::getName).orElse("Unknown"));
            info.put("type", territory.getTerritoryType());
            info.put("claimed_date", territory.getClaimedAt().toLocalDate().toString());
        }
        
        return info;
    }
    
    /**
     * Clear territory cache
     */
    public void clearCache() {
        this.territoryCache.clear();
        this.plugin.getLogger().info("Territory cache cleared");
    }
    
    /**
     * Get chunk key for caching
     * @param chunk Chunk to get key for
     * @return Unique chunk key
     */
    private String getChunkKey(final Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}