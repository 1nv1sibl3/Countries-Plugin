package xyz.inv1s1bl3.countries.core.territory;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all territories and chunk claims.
 */
public class TerritoryManager {
    
    private final CountriesPlugin plugin;
    private final ClaimLimits claimLimits;
    private final BorderVisualizer borderVisualizer;
    private final Map<String, Territory> territories; // Territory name -> Territory
    private final Map<ChunkCoordinate, String> chunkClaims; // Chunk -> Territory name
    private final Map<String, Set<String>> countryTerritories; // Country name -> Territory names
    private final Map<UUID, String> playerSelections; // Player UUID -> Selection tool mode
    private final Map<UUID, Location> selectionCorner1; // Player UUID -> First corner
    private final Map<UUID, Location> selectionCorner2; // Player UUID -> Second corner
    
    public TerritoryManager(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.claimLimits = new ClaimLimits(plugin);
        this.borderVisualizer = new BorderVisualizer(plugin);
        this.territories = new ConcurrentHashMap<>();
        this.chunkClaims = new ConcurrentHashMap<>();
        this.countryTerritories = new ConcurrentHashMap<>();
        this.playerSelections = new ConcurrentHashMap<>();
        this.selectionCorner1 = new ConcurrentHashMap<>();
        this.selectionCorner2 = new ConcurrentHashMap<>();
    }
    
    /**
     * Load all territories from storage
     */
    public void loadTerritories() {
        plugin.debug("Loading territories from storage...");
        
        // This will be implemented when storage system is complete
        plugin.debug("Territory loading system ready");
    }
    
    /**
     * Reload territory data
     */
    public void reload() {
        plugin.debug("Reloading territory manager...");
        
        // Clear current data
        territories.clear();
        chunkClaims.clear();
        countryTerritories.clear();
        
        // Reload from storage
        loadTerritories();
        
        plugin.debug("Territory manager reloaded successfully");
    }
    
    /**
     * Claim a chunk for a territory
     */
    public boolean claimChunk(Player player, String territoryName, Chunk chunk) {
        // Check claiming limits first
        if (!claimLimits.canClaimChunk(player)) {
            ChatUtils.sendError(player, "You have reached your chunk claiming limit! (" + 
                    claimLimits.getCurrentChunks(player) + "/" + claimLimits.getMaxChunks(player) + ")");
            return false;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            return false;
        }
        
        Citizen citizen = country.getCitizen(player.getUniqueId());
        if (citizen == null || !citizen.getRole().canBuild()) {
            return false;
        }
        
        ChunkCoordinate coord = new ChunkCoordinate(chunk);
        
        // Check if chunk is already claimed
        if (isChunkClaimed(coord)) {
            return false;
        }
        
        // Check claim cost
        double claimCost = plugin.getConfigManager().getConfig()
                .getDouble("territory.chunk-claim-cost", 100.0);
        
        // Check territory limits
        int maxTerritories = plugin.getConfigManager().getConfig()
                .getInt("general.max-territories-per-country", 50);
        
        if (getCountryTerritoryCount(country.getName()) >= maxTerritories) {
            return false;
        }
        
        try {
            // Check and withdraw claim cost BEFORE claiming
            if (plugin.hasVaultEconomy() && claimCost > 0) {
                if (!plugin.getVaultEconomy().has(player, claimCost)) {
                    plugin.debug("Player " + player.getName() + " has insufficient funds for chunk claim");
                    return false;
                }
                
                // Withdraw claim cost
                if (!plugin.getVaultEconomy().withdrawPlayer(player, claimCost).transactionSuccess()) {
                    plugin.debug("Failed to withdraw claim cost from player " + player.getName());
                    return false;
                }
                
                plugin.debug("Withdrew " + claimCost + " from player " + player.getName() + " for chunk claim");
            }
            
            // Get or create territory
            Territory territory = getTerritory(territoryName);
            if (territory == null) {
                // Check territory creation limits
                if (!claimLimits.canCreateTerritory(player)) {
                    ChatUtils.sendError(player, "You have reached your territory creation limit! (" + 
                            claimLimits.getCurrentTerritories(player) + "/" + claimLimits.getMaxTerritories(player) + ")");
                    return false;
                }
                
                territory = new Territory(territoryName, country.getName(), 
                        chunk.getWorld().getName(), TerritoryType.RESIDENTIAL);
                territories.put(territoryName.toLowerCase(), territory);
                
                // Add to country territories
                countryTerritories.computeIfAbsent(country.getName().toLowerCase(), 
                        k -> ConcurrentHashMap.newKeySet()).add(territoryName.toLowerCase());
            } else {
                // Check if territory belongs to the same country
                if (!territory.getCountryName().equalsIgnoreCase(country.getName())) {
                    return false;
                }
            }
            
            // Check chunk limit per territory
            int maxChunks = plugin.getConfigManager().getConfig()
                    .getInt("territory.max-chunks-per-territory", 100);
            
            if (territory.getChunkCount() >= maxChunks) {
                return false;
            }
            
            // Add chunk to territory
            if (territory.addChunk(coord)) {
                chunkClaims.put(coord, territoryName.toLowerCase());
                
                // Update country territory count
                country.setTotalTerritories(getCountryTerritoryCount(country.getName()));
                
                // Save territory
                saveTerritory(territory);
                
                plugin.debug("Chunk claimed: " + coord + " for territory: " + territoryName);
                return true;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error claiming chunk: " + coord, e);
            
            // Refund claim cost if something went wrong
            if (plugin.hasVaultEconomy() && claimCost > 0) {
                plugin.getVaultEconomy().depositPlayer(player, claimCost);
            }
        }
        
        return false;
    }
    
    /**
     * Unclaim a chunk from a territory
     */
    public boolean unclaimChunk(Player player, Chunk chunk) {
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null) {
            return false;
        }
        
        Citizen citizen = country.getCitizen(player.getUniqueId());
        if (citizen == null || !citizen.getRole().canBuild()) {
            return false;
        }
        
        ChunkCoordinate coord = new ChunkCoordinate(chunk);
        String territoryName = chunkClaims.get(coord);
        
        if (territoryName == null) {
            return false;
        }
        
        Territory territory = territories.get(territoryName);
        if (territory == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            return false;
        }
        
        try {
            // Remove chunk from territory
            if (territory.removeChunk(coord)) {
                chunkClaims.remove(coord);
                
                // If territory has no chunks left, remove it
                if (territory.getChunkCount() == 0) {
                    territories.remove(territoryName);
                    
                    Set<String> countryTerrs = countryTerritories.get(country.getName().toLowerCase());
                    if (countryTerrs != null) {
                        countryTerrs.remove(territoryName);
                    }
                    
                    // Delete from storage
                    deleteTerritoryFromStorage(territory);
                } else {
                    // Save updated territory
                    saveTerritory(territory);
                }
                
                // Update country territory count
                country.setTotalTerritories(getCountryTerritoryCount(country.getName()));
                
                plugin.debug("Chunk unclaimed: " + coord + " from territory: " + territoryName);
                return true;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error unclaiming chunk: " + coord, e);
        }
        
        return false;
    }
    
    /**
     * Get territory by name
     */
    public Territory getTerritory(String name) {
        if (name == null) return null;
        return territories.get(name.toLowerCase());
    }
    
    /**
     * Get territory at a specific location
     */
    public Territory getTerritoryAt(Location location) {
        ChunkCoordinate coord = new ChunkCoordinate(location);
        String territoryName = chunkClaims.get(coord);
        return territoryName != null ? territories.get(territoryName) : null;
    }
    
    /**
     * Get territory at a specific chunk
     */
    public Territory getTerritoryAt(Chunk chunk) {
        ChunkCoordinate coord = new ChunkCoordinate(chunk);
        String territoryName = chunkClaims.get(coord);
        return territoryName != null ? territories.get(territoryName) : null;
    }
    
    /**
     * Check if a chunk is claimed
     */
    public boolean isChunkClaimed(ChunkCoordinate coord) {
        return chunkClaims.containsKey(coord);
    }
    
    /**
     * Check if a chunk is claimed
     */
    public boolean isChunkClaimed(Chunk chunk) {
        return isChunkClaimed(new ChunkCoordinate(chunk));
    }
    
    /**
     * Get all territories
     */
    public Collection<Territory> getAllTerritories() {
        return new ArrayList<>(territories.values());
    }
    
    /**
     * Get territories owned by a country
     */
    public Set<Territory> getCountryTerritories(String countryName) {
        Set<String> territoryNames = countryTerritories.get(countryName.toLowerCase());
        if (territoryNames == null) {
            return new HashSet<>();
        }
        
        Set<Territory> result = new HashSet<>();
        for (String name : territoryNames) {
            Territory territory = territories.get(name);
            if (territory != null) {
                result.add(territory);
            }
        }
        
        return result;
    }
    
    /**
     * Get territory count for a country
     */
    public int getCountryTerritoryCount(String countryName) {
        Set<String> territoryNames = countryTerritories.get(countryName.toLowerCase());
        return territoryNames != null ? territoryNames.size() : 0;
    }
    
    /**
     * Get total chunk count for a country
     */
    public int getCountryChunkCount(String countryName) {
        return getCountryTerritories(countryName).stream()
                .mapToInt(Territory::getChunkCount)
                .sum();
    }
    
    /**
     * Check if a player can build at a location
     */
    public boolean canPlayerBuild(Player player, Location location) {
        Territory territory = getTerritoryAt(location);
        if (territory == null) {
            // Allow building in wilderness if configured
            return plugin.getConfigManager().getConfig()
                    .getBoolean("territory.allow-wilderness-claims", true);
        }
        
        // Check sub-area first
        SubArea subArea = territory.getSubAreaAt(location);
        if (subArea != null) {
            return subArea.hasFlag(player.getUniqueId(), TerritoryFlag.BUILD);
        }
        
        // Check territory flags
        if (!territory.hasFlag(TerritoryFlag.BUILD)) {
            return false;
        }
        
        // Check role-based permissions
        if (!territory.hasRoleFlag(player.getUniqueId(), TerritoryFlag.BUILD)) {
            return false;
        }
        
        // Check if territory allows building
        if (!territory.allowsBuilding()) {
            return false;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        
        // Country members can always build in their territory
        if (playerCountry != null && 
            territory.getCountryName().equalsIgnoreCase(playerCountry.getName())) {
            return true;
        }
        
        // Check if player is specifically allowed
        if (territory.isPlayerAllowed(player.getUniqueId())) {
            return true;
        }
        
        // Check if player's country is allowed
        if (playerCountry != null && territory.isCountryAllowed(playerCountry.getName())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a player can access a location
     */
    public boolean canPlayerAccess(Player player, Location location) {
        Territory territory = getTerritoryAt(location);
        if (territory == null) {
            return true; // Wilderness is accessible
        }
        
        // Check sub-area first
        SubArea subArea = territory.getSubAreaAt(location);
        if (subArea != null) {
            return subArea.hasFlag(player.getUniqueId(), TerritoryFlag.INTERACT);
        }
        
        // Check territory flags
        if (!territory.hasFlag(TerritoryFlag.INTERACT)) {
            return false;
        }
        
        // Check role-based permissions
        if (!territory.hasRoleFlag(player.getUniqueId(), TerritoryFlag.INTERACT)) {
            return false;
        }
        
        // Check public access
        if (territory.allowsPublicAccess()) {
            return true;
        }
        
        Country playerCountry = plugin.getCountryManager().getPlayerCountry(player);
        
        // Country members can always access their territory
        if (playerCountry != null && 
            territory.getCountryName().equalsIgnoreCase(playerCountry.getName())) {
            return true;
        }
        
        // Check if player is specifically allowed
        if (territory.isPlayerAllowed(player.getUniqueId())) {
            return true;
        }
        
        // Check if player's country is allowed
        if (playerCountry != null && territory.isCountryAllowed(playerCountry.getName())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Create a sub-area within a territory
     */
    public boolean createSubArea(Player player, String territoryName, String subAreaName, 
                                Location corner1, Location corner2) {
        Territory territory = getTerritory(territoryName);
        if (territory == null) {
            return false;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            return false;
        }
        
        // Check if player has permission (manager or owner role)
        TerritoryRole playerRole = territory.getPlayerRole(player.getUniqueId());
        if (playerRole.getPriority() < TerritoryRole.MANAGER.getPriority()) {
            return false;
        }
        
        // Create sub-area
        SubArea subArea = new SubArea(subAreaName, territoryName, 
                corner1.getWorld().getName(), corner1, corner2);
        
        if (territory.addSubArea(subArea)) {
            saveTerritory(territory);
            return true;
        }
        
        return false;
    }
    
    /**
     * Set player selection corner
     */
    public void setSelectionCorner(Player player, Location location, boolean isFirstCorner) {
        if (isFirstCorner) {
            selectionCorner1.put(player.getUniqueId(), location);
        } else {
            selectionCorner2.put(player.getUniqueId(), location);
        }
    }
    
    /**
     * Get player's selection corners
     */
    public Location[] getPlayerSelection(Player player) {
        Location corner1 = selectionCorner1.get(player.getUniqueId());
        Location corner2 = selectionCorner2.get(player.getUniqueId());
        return new Location[]{corner1, corner2};
    }
    
    /**
     * Clear player's selection
     */
    public void clearPlayerSelection(Player player) {
        selectionCorner1.remove(player.getUniqueId());
        selectionCorner2.remove(player.getUniqueId());
    }
    
    /**
     * Set territory flag
     */
    public boolean setTerritoryFlag(Player player, String territoryName, 
                                   TerritoryFlag flag, boolean value) {
        Territory territory = getTerritory(territoryName);
        if (territory == null) {
            return false;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(player);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            return false;
        }
        
        // Check if player has permission
        TerritoryRole playerRole = territory.getPlayerRole(player.getUniqueId());
        if (playerRole.getPriority() < TerritoryRole.MANAGER.getPriority()) {
            return false;
        }
        
        territory.setFlag(flag, value);
        saveTerritory(territory);
        return true;
    }
    
    /**
     * Trust player in territory
     */
    public boolean trustPlayer(Player manager, String territoryName, 
                              UUID playerUUID, TerritoryRole role) {
        Territory territory = getTerritory(territoryName);
        if (territory == null) {
            return false;
        }
        
        Country country = plugin.getCountryManager().getPlayerCountry(manager);
        if (country == null || !territory.getCountryName().equalsIgnoreCase(country.getName())) {
            return false;
        }
        
        // Check if manager has permission
        TerritoryRole managerRole = territory.getPlayerRole(manager.getUniqueId());
        if (!managerRole.canManage(role)) {
            return false;
        }
        
        territory.setPlayerRole(playerUUID, role);
        saveTerritory(territory);
        return true;
    }
    
    /**
     * Rent a sub-area to a player
     */
    public boolean rentSubArea(Player player, String territoryName, String subAreaName) {
        Territory territory = getTerritory(territoryName);
        if (territory == null) {
            return false;
        }
        
        SubArea subArea = territory.getSubArea(subAreaName);
        if (subArea == null || !subArea.isForRent() || subArea.getCurrentTenant() != null) {
            return false;
        }
        
        // Check if player can afford rent
        if (plugin.hasVaultEconomy() && subArea.getRentPrice() > 0) {
            if (!plugin.getVaultEconomy().has(player, subArea.getRentPrice())) {
                return false;
            }
            
            // Withdraw rent payment
            if (!plugin.getVaultEconomy().withdrawPlayer(player, subArea.getRentPrice()).transactionSuccess()) {
                return false;
            }
        }
        
        // Set tenant
        subArea.setCurrentTenant(player.getUniqueId());
        saveTerritory(territory);
        return true;
    }
    
    /**
     * Delete a territory
     */
    public boolean deleteTerritory(String territoryName, UUID requesterUUID) {
        Territory territory = getTerritory(territoryName);
        if (territory == null) {
            return false;
        }
        
        Country country = plugin.getCountryManager().getCountry(territory.getCountryName());
        if (country == null) {
            return false;
        }
        
        Citizen citizen = country.getCitizen(requesterUUID);
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            return false;
        }
        
        try {
            // Remove all chunk claims
            for (ChunkCoordinate chunk : territory.getChunks()) {
                chunkClaims.remove(chunk);
            }
            
            // Remove territory
            territories.remove(territoryName.toLowerCase());
            
            // Remove from country territories
            Set<String> countryTerrs = countryTerritories.get(country.getName().toLowerCase());
            if (countryTerrs != null) {
                countryTerrs.remove(territoryName.toLowerCase());
            }
            
            // Update country territory count
            country.setTotalTerritories(getCountryTerritoryCount(country.getName()));
            
            // Delete from storage
            deleteTerritoryFromStorage(territory);
            
            plugin.debug("Territory deleted: " + territoryName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error deleting territory: " + territoryName, e);
            return false;
        }
    }
    
    /**
     * Save a territory to storage
     */
    private void saveTerritory(Territory territory) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving territory: " + territory.getName());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving territory: " + territory.getName(), e);
            }
        });
    }
    
    /**
     * Delete a territory from storage
     */
    private void deleteTerritoryFromStorage(Territory territory) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Deleting territory from storage: " + territory.getName());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error deleting territory from storage: " + territory.getName(), e);
            }
        });
    }
    
    /**
     * Get player's total chunk count across all territories
     */
    public int getPlayerChunkCount(UUID playerUUID) {
        return territories.values().stream()
                .filter(t -> {
                    Country country = plugin.getCountryManager().getCountry(t.getCountryName());
                    return country != null && country.isCitizen(playerUUID);
                })
                .mapToInt(Territory::getChunkCount)
                .sum();
    }
    
    /**
     * Get player's total territory count
     */
    public int getPlayerTerritoryCount(UUID playerUUID) {
        return (int) territories.values().stream()
                .filter(t -> {
                    Country country = plugin.getCountryManager().getCountry(t.getCountryName());
                    return country != null && country.isCitizen(playerUUID);
                })
                .count();
    }
    
    /**
     * Get claim limits manager
     */
    public ClaimLimits getClaimLimits() {
        return claimLimits;
    }
    
    /**
     * Get border visualizer
     */
    public BorderVisualizer getBorderVisualizer() {
        return borderVisualizer;
    }
    
    /**
     * Get statistics about territories
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalSubAreas = territories.values().stream()
                .mapToInt(t -> t.getSubAreas().size())
                .sum();
        
        int totalTrustedPlayers = territories.values().stream()
                .mapToInt(t -> t.getAllPlayerRoles().size())
                .sum();
        
        stats.put("total_territories", territories.size());
        stats.put("total_chunks_claimed", chunkClaims.size());
        stats.put("total_sub_areas", totalSubAreas);
        stats.put("total_trusted_players", totalTrustedPlayers);
        stats.put("active_territories", territories.values().stream()
                .mapToInt(territory -> territory.isActive() ? 1 : 0)
                .sum());
        stats.put("average_chunks_per_territory", 
                territories.isEmpty() ? 0 : (double) chunkClaims.size() / territories.size());
        
        return stats;
    }
}