package xyz.inv1s1bl3.countries.territory;

import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;
import xyz.inv1s1bl3.countries.database.repositories.TerritoryRepository;
import xyz.inv1s1bl3.countries.database.repositories.CountryRepository;
import xyz.inv1s1bl3.countries.database.repositories.PlayerRepository;
import xyz.inv1s1bl3.countries.territory.protection.AdvancedProtectionManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Manager for territory-related operations
 */
@Getter
public final class TerritoryManager {
    
    private final CountriesPlugin plugin;
    private final TerritoryRepository territoryRepository;
    private final CountryRepository countryRepository;
    private final PlayerRepository playerRepository;
    private final TerritoryService territoryService;
    private final ChunkManager chunkManager;
    private final ProtectionManager protectionManager;
    private final AdvancedProtectionManager advancedProtectionManager;
    
    public TerritoryManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.territoryRepository = new TerritoryRepository(plugin);
        this.countryRepository = new CountryRepository(plugin);
        this.playerRepository = new PlayerRepository(plugin);
        this.territoryService = new TerritoryService(plugin, this.territoryRepository, this.countryRepository, this.playerRepository);
        this.chunkManager = new ChunkManager(plugin, this.territoryService);
        this.advancedProtectionManager = new AdvancedProtectionManager(plugin, this.chunkManager);
        this.protectionManager = new ProtectionManager(plugin, this.advancedProtectionManager);
    }
    
    /**
     * Initialize the territory manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing Territory Manager...");
        
        try {
            // Initialize chunk manager
            this.chunkManager.initialize();
            
            this.plugin.getLogger().info("Territory Manager initialized successfully!");
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize Territory Manager!", exception);
            throw new RuntimeException("Territory Manager initialization failed", exception);
        }
    }
    
    /**
     * Claim a chunk for a player's country
     * @param player Player claiming the chunk
     * @param chunk Chunk to claim
     * @param territoryType Territory type
     * @return Claimed territory
     */
    public Territory claimChunk(final Player player, final Chunk chunk, final String territoryType) {
        return this.chunkManager.claimChunk(player, chunk, territoryType);
    }
    
    /**
     * Unclaim a chunk
     * @param player Player unclaiming the chunk
     * @param chunk Chunk to unclaim
     */
    public void unclaimChunk(final Player player, final Chunk chunk) {
        this.chunkManager.unclaimChunk(player, chunk);
    }
    
    /**
     * Get territory at a location
     * @param location Location to check
     * @return Territory if claimed, empty otherwise
     */
    public Optional<Territory> getTerritoryAt(final Location location) {
        return this.chunkManager.getTerritoryAt(location.getChunk());
    }
    
    /**
     * Get all territories for a country
     * @param countryId Country ID
     * @return List of territories
     */
    public List<Territory> getCountryTerritories(final Integer countryId) {
        try {
            return this.territoryRepository.findByCountryId(countryId);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting country territories", exception);
            return List.of();
        }
    }
    
    /**
     * Check if a player can build at a location
     * @param player Player to check
     * @param location Location to check
     * @return true if player can build
     */
    public boolean canPlayerBuild(final Player player, final Location location) {
        return this.protectionManager.canPlaceBlock(player, location);
    }
    
    /**
     * Check if a player can break a block
     * @param player Player to check
     * @param location Location to check
     * @return true if player can break
     */
    public boolean canPlayerBreak(final Player player, final Location location) {
        return this.protectionManager.canBreakBlock(player, location.getBlock());
    }
    
    /**
     * Toggle auto-claim for a player
     * @param player Player to toggle auto-claim for
     * @return New auto-claim status
     */
    public boolean toggleAutoClaim(final Player player) {
        return this.chunkManager.toggleAutoClaim(player);
    }
    
    /**
     * Handle player movement for auto-claiming
     * @param player Player who moved
     * @param to Location moved to
     */
    public void handlePlayerMovement(final Player player, final Location to) {
        this.chunkManager.handlePlayerMovement(player, to);
    }
    
    /**
     * Get chunk information
     * @param chunk Chunk to get info for
     * @return Map of chunk information
     */
    public Map<String, String> getChunkInfo(final Chunk chunk) {
        return this.chunkManager.getChunkInfo(chunk);
    }
    
    /**
     * Get protection status for a location
     * @param location Location to check
     * @return Protection status description
     */
    public String getProtectionStatus(final Location location) {
        return this.protectionManager.getProtectionStatus(location);
    }
    
    /**
     * Check if PvP is allowed at a location
     * @param location Location to check
     * @return true if PvP is allowed
     */
    public boolean isPvpAllowed(final Location location) {
        return this.protectionManager.isPvpAllowed(location);
    }
    
    /**
     * Check if explosions are allowed at a location
     * @param location Location to check
     * @return true if explosions are allowed
     */
    public boolean areExplosionsAllowed(final Location location) {
        return this.protectionManager.areExplosionsAllowed(location);
    }
    
    /**
     * Check if fire spread is allowed at a location
     * @param location Location to check
     * @return true if fire spread is allowed
     */
    public boolean isFireSpreadAllowed(final Location location) {
        return this.protectionManager.isFireSpreadAllowed(location);
    }
    
    /**
     * Check if mob spawning is allowed at a location
     * @param location Location to check
     * @param entityType Entity type to spawn
     * @return true if spawning is allowed
     */
    public boolean isMobSpawningAllowed(final Location location, final org.bukkit.entity.EntityType entityType) {
        return this.protectionManager.isMobSpawningAllowed(location, entityType);
    }
    
    /**
     * Change territory type
     * @param player Player changing the type
     * @param territory Territory to change
     * @param newType New territory type
     */
    public void changeTerritoryType(final Player player, final Territory territory, final String newType) {
        try {
            this.territoryService.changeTerritoryType(player.getUniqueId(), territory, newType);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while changing territory type", exception);
            throw new RuntimeException("Failed to change territory type due to database error", exception);
        }
    }
        
    /**
     * Get territories needing maintenance
     * @param maintenanceInterval Maintenance interval in hours
     * @return List of territories needing maintenance
     */
    public List<Territory> getTerritoriesNeedingMaintenance(final int maintenanceInterval) {
        try {
            return this.territoryRepository.findTerritoriesNeedingMaintenance(maintenanceInterval);
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting territories needing maintenance", exception);
            return List.of();
        }
    }
    
    /**
     * Save all territory data
     */
    public void saveAllData() {
        this.plugin.getLogger().info("Saving all territory data...");
        
        try {
            // Clear cache to ensure fresh data on next load
            this.chunkManager.clearCache();
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error saving territory data", exception);
        }
        
        this.plugin.getLogger().info("All territory data saved successfully!");
    }
    
    /**
     * Reload territory manager
     */
    public void reload() {
        this.plugin.getLogger().info("Reloading Territory Manager...");
        
        try {
            // Clear cache and reinitialize
            this.chunkManager.clearCache();
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error reloading Territory Manager", exception);
        }
        
        this.plugin.getLogger().info("Territory Manager reloaded successfully!");
    }
}