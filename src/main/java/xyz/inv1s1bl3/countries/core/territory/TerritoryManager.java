package xyz.inv1s1bl3.countries.core.territory;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all territories in the plugin.
 * Handles territory creation, claiming, and protection.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class TerritoryManager {
    
    private final CountriesPlugin plugin;
    
    @Getter
    private final Map<String, Territory> territories;
    private final Map<ChunkCoordinate, String> chunkClaims;
    
    public TerritoryManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.territories = new ConcurrentHashMap<>();
        this.chunkClaims = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new territory.
     * 
     * @param name the territory name
     * @param countryName the owning country name
     * @param ownerId the owner's UUID
     * @param type the territory type
     * @return the created territory, or null if creation failed
     */
    @Nullable
    public Territory createTerritory(@NotNull final String name,
                                   @NotNull final String countryName,
                                   @NotNull final UUID ownerId,
                                   @NotNull final TerritoryType type) {
        // Validate territory name
        if (!this.isValidTerritoryName(name)) {
            return null;
        }
        
        // Check if territory already exists
        if (this.territories.containsKey(name.toLowerCase())) {
            return null;
        }
        
        // Check if country exists
        final Country country = this.plugin.getCountryManager().getCountry(countryName);
        if (country == null) {
            return null;
        }
        
        // Check territory limit
        final int maxTerritories = this.plugin.getConfigManager()
            .getConfigValue("territory.max-territories-per-country", 10);
        if (country.getTerritoryCount() >= maxTerritories) {
            return null;
        }
        
        // Create the territory
        final Territory territory = new Territory(name, countryName, ownerId, type);
        
        // Add to maps
        this.territories.put(name.toLowerCase(), territory);
        country.addTerritory(name);
        
        // Save data
        this.plugin.getDataManager().saveTerritoryData();
        
        return territory;
    }
    
    /**
     * Delete a territory.
     * 
     * @param name the territory name
     * @return true if deleted successfully
     */
    public boolean deleteTerritory(@NotNull final String name) {
        final Territory territory = this.territories.get(name.toLowerCase());
        if (territory == null) {
            return false;
        }
        
        // Remove all chunk claims
        final List<ChunkCoordinate> chunksToRemove = new ArrayList<>();
        for (final Map.Entry<ChunkCoordinate, String> entry : this.chunkClaims.entrySet()) {
            if (entry.getValue().equals(name.toLowerCase())) {
                chunksToRemove.add(entry.getKey());
            }
        }
        
        for (final ChunkCoordinate chunk : chunksToRemove) {
            this.chunkClaims.remove(chunk);
        }
        
        // Remove from country
        final Country country = this.plugin.getCountryManager().getCountry(territory.getCountryName());
        if (country != null) {
            country.removeTerritory(name);
        }
        
        // Remove territory
        this.territories.remove(name.toLowerCase());
        
        // Save data
        this.plugin.getDataManager().saveTerritoryData();
        
        return true;
    }
    
    /**
     * Get a territory by name.
     * 
     * @param name the territory name
     * @return the territory, or null if not found
     */
    @Nullable
    public Territory getTerritory(@NotNull final String name) {
        return this.territories.get(name.toLowerCase());
    }
    
    /**
     * Get the territory that owns a specific chunk.
     * 
     * @param chunk the chunk
     * @return the territory, or null if not claimed
     */
    @Nullable
    public Territory getTerritoryAt(@NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = ChunkCoordinate.fromChunk(chunk);
        final String territoryName = this.chunkClaims.get(coordinate);
        
        if (territoryName == null) {
            return null;
        }
        
        return this.territories.get(territoryName);
    }
    
    /**
     * Get the territory that contains a specific location.
     * 
     * @param location the location
     * @return the territory, or null if not claimed
     */
    @Nullable
    public Territory getTerritoryAt(@NotNull final Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return null;
        }
        
        final Chunk chunk = location.getChunk();
        return this.getTerritoryAt(chunk);
    }
    
    /**
     * Claim a chunk for a territory.
     * 
     * @param territory the territory
     * @param chunk the chunk to claim
     * @return true if claimed successfully
     */
    public boolean claimChunk(@NotNull final Territory territory, @NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = ChunkCoordinate.fromChunk(chunk);
        
        // Check if chunk is already claimed
        if (this.chunkClaims.containsKey(coordinate)) {
            return false;
        }
        
        // Check chunk limit for territory type
        if (territory.getChunkCount() >= territory.getType().getMaxChunks()) {
            return false;
        }
        
        // Check if claiming is allowed in wilderness only
        final boolean wildernessOnly = this.plugin.getConfigManager()
            .getConfigValue("territory.wilderness-claims-only", true);
        
        if (wildernessOnly && this.isNearOtherTerritory(coordinate)) {
            return false;
        }
        
        // Check country balance for claim cost
        final Country country = this.plugin.getCountryManager().getCountry(territory.getCountryName());
        if (country == null) {
            return false;
        }
        
        final double claimCost = this.plugin.getConfigManager()
            .getConfigValue("territory.chunk-claim-cost", 100.0);
        
        if (country.getBalance() < claimCost) {
            return false;
        }
        
        // Claim the chunk
        territory.addChunk(chunk);
        this.chunkClaims.put(coordinate, territory.getName().toLowerCase());
        country.withdraw(claimCost);
        
        // Save data
        this.plugin.getDataManager().saveTerritoryData();
        
        return true;
    }
    
    /**
     * Unclaim a chunk from a territory.
     * 
     * @param territory the territory
     * @param chunk the chunk to unclaim
     * @return true if unclaimed successfully
     */
    public boolean unclaimChunk(@NotNull final Territory territory, @NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = ChunkCoordinate.fromChunk(chunk);
        
        // Check if chunk is claimed by this territory
        final String claimedBy = this.chunkClaims.get(coordinate);
        if (claimedBy == null || !claimedBy.equals(territory.getName().toLowerCase())) {
            return false;
        }
        
        // Unclaim the chunk
        territory.removeChunk(chunk);
        this.chunkClaims.remove(coordinate);
        
        // Save data
        this.plugin.getDataManager().saveTerritoryData();
        
        return true;
    }
    
    /**
     * Get all territories owned by a country.
     * 
     * @param countryName the country name
     * @return a list of territories
     */
    @NotNull
    public List<Territory> getTerritoriesByCountry(@NotNull final String countryName) {
        return this.territories.values().stream()
            .filter(territory -> territory.getCountryName().equalsIgnoreCase(countryName))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all territories owned by a player.
     * 
     * @param ownerId the owner's UUID
     * @return a list of territories
     */
    @NotNull
    public List<Territory> getTerritoriesByOwner(@NotNull final UUID ownerId) {
        return this.territories.values().stream()
            .filter(territory -> territory.getOwnerId().equals(ownerId))
            .collect(Collectors.toList());
    }
    
    /**
     * Search for territories by name.
     * 
     * @param query the search query
     * @return a list of matching territories
     */
    @NotNull
    public List<Territory> searchTerritories(@NotNull final String query) {
        final String lowerQuery = query.toLowerCase();
        return this.territories.values().stream()
            .filter(territory -> territory.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a territory name is valid.
     * 
     * @param name the territory name
     * @return true if valid
     */
    public boolean isValidTerritoryName(@NotNull final String name) {
        if (name.length() < 3 || name.length() > 30) {
            return false;
        }
        
        // Check for valid characters (letters, numbers, spaces, hyphens, underscores)
        return name.matches("^[a-zA-Z0-9 _-]+$");
    }
    
    /**
     * Check if a chunk is near another territory.
     * 
     * @param coordinate the chunk coordinate
     * @return true if near another territory
     */
    private boolean isNearOtherTerritory(@NotNull final ChunkCoordinate coordinate) {
        final int minDistance = this.plugin.getConfigManager()
            .getConfigValue("territory.min-territory-distance", 5);
        
        for (final ChunkCoordinate claimed : this.chunkClaims.keySet()) {
            if (coordinate.getDistance(claimed) < minDistance) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get territory statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_territories", this.territories.size());
        stats.put("total_claimed_chunks", this.chunkClaims.size());
        stats.put("average_chunks_per_territory", 
            this.territories.isEmpty() ? 0.0 : (double) this.chunkClaims.size() / this.territories.size());
        
        final Map<TerritoryType, Long> typeCounts = this.territories.values().stream()
            .collect(Collectors.groupingBy(Territory::getType, Collectors.counting()));
        stats.put("territory_type_distribution", typeCounts);
        
        return stats;
    }
    
    /**
     * Load territory data from storage.
     * 
     * @param territoryData the territory data map
     */
    public void loadTerritoryData(@NotNull final Map<String, Territory> territoryData) {
        this.territories.clear();
        this.chunkClaims.clear();
        
        for (final Map.Entry<String, Territory> entry : territoryData.entrySet()) {
            final Territory territory = entry.getValue();
            this.territories.put(entry.getKey(), territory);
            
            // Rebuild chunk claims mapping
            for (final ChunkCoordinate chunk : territory.getChunks()) {
                this.chunkClaims.put(chunk, entry.getKey());
            }
        }
    }
    
    /**
     * Check if a player can build in a territory.
     * 
     * @param player the player
     * @param territory the territory
     * @return true if allowed to build
     */
    public boolean canBuild(@NotNull final Player player, @NotNull final Territory territory) {
        // Territory owner can always build
        if (player.getUniqueId().equals(territory.getOwnerId())) {
            return true;
        }
        
        // Check if player is from the same country
        final Country playerCountry = this.plugin.getCountryManager().getPlayerCountry(player.getUniqueId());
        if (playerCountry != null && playerCountry.getName().equals(territory.getCountryName())) {
            return territory.hasPermission("build");
        }
        
        // Check diplomatic relations
        // TODO: Implement diplomacy check
        
        return false;
    }
    
    /**
     * Check if PvP is allowed in a territory.
     * 
     * @param territory the territory
     * @return true if PvP is allowed
     */
    public boolean isPvpAllowed(@NotNull final Territory territory) {
        return territory.hasPermission("pvp");
    }
    
    /**
     * Get all territories.
     * 
     * @return a collection of all territories
     */
    @NotNull
    public Collection<Territory> getAllTerritories() {
        return this.territories.values();
    }
}