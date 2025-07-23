package xyz.inv1s1bl3.countries.core.territory;

import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a territory owned by a country, consisting of one or more chunks.
 */
public class Territory {
    
    private final String name;
    private final String countryName;
    private final String worldName;
    private TerritoryType type;
    private final long claimedDate;
    private final Set<ChunkCoordinate> chunks;
    private final Map<String, SubArea> subAreas;
    
    // Settings
    private boolean allowPublicAccess;
    private boolean allowBuilding;
    private boolean allowPvP;
    private double upkeepCost;
    private String description;
    private final Map<TerritoryFlag, Boolean> flags;
    private final Map<TerritoryRole, Map<TerritoryFlag, Boolean>> roleFlags;
    private final Map<UUID, TerritoryRole> playerRoles;
    
    // Permissions
    private final Set<UUID> allowedPlayers;
    private final Set<String> allowedCountries;
    
    // Economic features
    private double taxRate;
    private boolean taxEnabled;
    private long lastTaxCollection;
    
    // Messages
    private String enterMessage;
    private String leaveMessage;
    
    // Statistics
    private long lastActive;
    private int totalVisitors;
    
    public Territory(String name, String countryName, String worldName, TerritoryType type) {
        this.name = name;
        this.countryName = countryName;
        this.worldName = worldName;
        this.type = type;
        this.claimedDate = System.currentTimeMillis();
        this.chunks = ConcurrentHashMap.newKeySet();
        this.subAreas = new ConcurrentHashMap<>();
        
        // Initialize settings based on territory type
        this.allowPublicAccess = type.allowsPublicAccess();
        this.allowBuilding = type.allowsBuilding();
        this.allowPvP = false;
        this.upkeepCost = 0.0;
        this.description = "A " + type.getDisplayName().toLowerCase() + " territory.";
        this.flags = new EnumMap<>(TerritoryFlag.class);
        this.roleFlags = new ConcurrentHashMap<>();
        this.playerRoles = new ConcurrentHashMap<>();
        
        this.allowedPlayers = ConcurrentHashMap.newKeySet();
        this.allowedCountries = ConcurrentHashMap.newKeySet();
        
        this.taxRate = 0.0;
        this.taxEnabled = false;
        this.lastTaxCollection = 0L;
        
        this.enterMessage = "";
        this.leaveMessage = "";
        
        this.lastActive = System.currentTimeMillis();
        this.totalVisitors = 0;
        
        // Initialize default flags and roles
        initializeDefaultFlags();
        initializeDefaultRoles();
    }
    
    private void initializeDefaultFlags() {
        for (TerritoryFlag flag : TerritoryFlag.values()) {
            flags.put(flag, flag.getDefaultValue());
        }
    }
    
    private void initializeDefaultRoles() {
        for (TerritoryRole role : TerritoryRole.values()) {
            roleFlags.put(role, new EnumMap<>(role.getDefaultFlags()));
        }
    }
    
    // Basic getters
    public String getName() {
        return name;
    }
    
    public String getCountryName() {
        return countryName;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public TerritoryType getType() {
        return type;
    }
    
    public long getClaimedDate() {
        return claimedDate;
    }
    
    public boolean allowsPublicAccess() {
        return allowPublicAccess;
    }
    
    public boolean allowsBuilding() {
        return allowBuilding;
    }
    
    public boolean allowsPvP() {
        return allowPvP;
    }
    
    public double getUpkeepCost() {
        return upkeepCost;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getLastActive() {
        return lastActive;
    }
    
    public int getTotalVisitors() {
        return totalVisitors;
    }
    
    public Map<String, SubArea> getSubAreas() {
        return new HashMap<>(subAreas);
    }
    
    public double getTaxRate() {
        return taxRate;
    }
    
    public boolean isTaxEnabled() {
        return taxEnabled;
    }
    
    public long getLastTaxCollection() {
        return lastTaxCollection;
    }
    
    public String getEnterMessage() {
        return enterMessage;
    }
    
    public String getLeaveMessage() {
        return leaveMessage;
    }
    
    // Setters
    public void setType(TerritoryType type) {
        this.type = type;
        updateLastActive();
    }
    
    public void setAllowPublicAccess(boolean allowPublicAccess) {
        this.allowPublicAccess = allowPublicAccess;
        updateLastActive();
    }
    
    public void setAllowBuilding(boolean allowBuilding) {
        this.allowBuilding = allowBuilding;
        updateLastActive();
    }
    
    public void setAllowPvP(boolean allowPvP) {
        this.allowPvP = allowPvP;
        updateLastActive();
    }
    
    public void setUpkeepCost(double upkeepCost) {
        this.upkeepCost = Math.max(0, upkeepCost);
        updateLastActive();
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
        updateLastActive();
    }
    
    public void setTaxRate(double taxRate) {
        this.taxRate = Math.max(0, Math.min(1, taxRate));
        updateLastActive();
    }
    
    public void setTaxEnabled(boolean taxEnabled) {
        this.taxEnabled = taxEnabled;
        updateLastActive();
    }
    
    public void setLastTaxCollection(long lastTaxCollection) {
        this.lastTaxCollection = lastTaxCollection;
    }
    
    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage != null ? enterMessage : "";
        updateLastActive();
    }
    
    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage != null ? leaveMessage : "";
        updateLastActive();
    }
    
    public void incrementVisitors() {
        this.totalVisitors++;
        updateLastActive();
    }
    
    // Chunk management
    public Set<ChunkCoordinate> getChunks() {
        return new HashSet<>(chunks);
    }
    
    public int getChunkCount() {
        return chunks.size();
    }
    
    public boolean containsChunk(ChunkCoordinate chunk) {
        return chunks.contains(chunk);
    }
    
    public boolean addChunk(ChunkCoordinate chunk) {
        if (!chunk.getWorldName().equals(worldName)) {
            return false;
        }
        
        boolean added = chunks.add(chunk);
        if (added) {
            updateLastActive();
        }
        return added;
    }
    
    public boolean removeChunk(ChunkCoordinate chunk) {
        boolean removed = chunks.remove(chunk);
        if (removed) {
            updateLastActive();
        }
        return removed;
    }
    
    /**
     * Check if territory is contiguous (all chunks are connected)
     */
    public boolean isContiguous() {
        if (chunks.size() <= 1) {
            return true;
        }
        
        Set<ChunkCoordinate> visited = new HashSet<>();
        Queue<ChunkCoordinate> queue = new LinkedList<>();
        
        // Start with first chunk
        ChunkCoordinate start = chunks.iterator().next();
        queue.offer(start);
        visited.add(start);
        
        while (!queue.isEmpty()) {
            ChunkCoordinate current = queue.poll();
            
            // Check all chunks for adjacency
            for (ChunkCoordinate chunk : chunks) {
                if (!visited.contains(chunk) && current.isAdjacentTo(chunk)) {
                    visited.add(chunk);
                    queue.offer(chunk);
                }
            }
        }
        
        return visited.size() == chunks.size();
    }
    
    /**
     * Get the center chunk of the territory
     */
    public ChunkCoordinate getCenterChunk() {
        if (chunks.isEmpty()) {
            return null;
        }
        
        int totalX = 0;
        int totalZ = 0;
        
        for (ChunkCoordinate chunk : chunks) {
            totalX += chunk.getX();
            totalZ += chunk.getZ();
        }
        
        int centerX = totalX / chunks.size();
        int centerZ = totalZ / chunks.size();
        
        // Find the closest actual chunk to the calculated center
        ChunkCoordinate center = new ChunkCoordinate(worldName, centerX, centerZ);
        ChunkCoordinate closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (ChunkCoordinate chunk : chunks) {
            double distance = chunk.distanceTo(center);
            if (distance < minDistance) {
                minDistance = distance;
                closest = chunk;
            }
        }
        
        return closest;
    }
    
    // Permission management
    public Set<UUID> getAllowedPlayers() {
        return new HashSet<>(allowedPlayers);
    }
    
    public Set<String> getAllowedCountries() {
        return new HashSet<>(allowedCountries);
    }
    
    public boolean isPlayerAllowed(UUID playerUUID) {
        return allowPublicAccess || allowedPlayers.contains(playerUUID);
    }
    
    public boolean isCountryAllowed(String countryName) {
        return allowPublicAccess || allowedCountries.contains(countryName.toLowerCase());
    }
    
    public void addAllowedPlayer(UUID playerUUID) {
        allowedPlayers.add(playerUUID);
        updateLastActive();
    }
    
    public void removeAllowedPlayer(UUID playerUUID) {
        allowedPlayers.remove(playerUUID);
        updateLastActive();
    }
    
    public void addAllowedCountry(String countryName) {
        allowedCountries.add(countryName.toLowerCase());
        updateLastActive();
    }
    
    public void removeAllowedCountry(String countryName) {
        allowedCountries.remove(countryName.toLowerCase());
        updateLastActive();
    }
    
    // Sub-area management
    public SubArea getSubArea(String name) {
        return subAreas.get(name.toLowerCase());
    }
    
    public boolean addSubArea(SubArea subArea) {
        if (subAreas.containsKey(subArea.getName().toLowerCase())) {
            return false;
        }
        subAreas.put(subArea.getName().toLowerCase(), subArea);
        updateLastActive();
        return true;
    }
    
    public boolean removeSubArea(String name) {
        boolean removed = subAreas.remove(name.toLowerCase()) != null;
        if (removed) {
            updateLastActive();
        }
        return removed;
    }
    
    public SubArea getSubAreaAt(org.bukkit.Location location) {
        for (SubArea subArea : subAreas.values()) {
            if (subArea.contains(location)) {
                return subArea;
            }
        }
        return null;
    }
    
    // Flag management
    public boolean hasFlag(TerritoryFlag flag) {
        return flags.getOrDefault(flag, flag.getDefaultValue());
    }
    
    public void setFlag(TerritoryFlag flag, boolean value) {
        flags.put(flag, value);
        updateLastActive();
    }
    
    public Map<TerritoryFlag, Boolean> getAllFlags() {
        return new EnumMap<>(flags);
    }
    
    // Role management
    public TerritoryRole getPlayerRole(UUID playerUUID) {
        return playerRoles.getOrDefault(playerUUID, TerritoryRole.VISITOR);
    }
    
    public void setPlayerRole(UUID playerUUID, TerritoryRole role) {
        if (role == TerritoryRole.VISITOR) {
            playerRoles.remove(playerUUID);
        } else {
            playerRoles.put(playerUUID, role);
        }
        updateLastActive();
    }
    
    public boolean hasRoleFlag(UUID playerUUID, TerritoryFlag flag) {
        TerritoryRole role = getPlayerRole(playerUUID);
        Map<TerritoryFlag, Boolean> roleFlagMap = roleFlags.get(role);
        return roleFlagMap != null && roleFlagMap.getOrDefault(flag, false);
    }
    
    public void setRoleFlag(TerritoryRole role, TerritoryFlag flag, boolean value) {
        roleFlags.computeIfAbsent(role, k -> new EnumMap<>(TerritoryFlag.class))
                 .put(flag, value);
        updateLastActive();
    }
    
    public Map<TerritoryFlag, Boolean> getRoleFlags(TerritoryRole role) {
        return new EnumMap<>(roleFlags.getOrDefault(role, new EnumMap<>(TerritoryFlag.class)));
    }
    
    public Map<UUID, TerritoryRole> getAllPlayerRoles() {
        return new HashMap<>(playerRoles);
    }
    
    // Utility methods
    private void updateLastActive() {
        this.lastActive = System.currentTimeMillis();
    }
    
    public long getDaysSinceClaimed() {
        return (System.currentTimeMillis() - claimedDate) / 86400000;
    }
    
    public boolean isActive() {
        // Consider territory active if used within last 7 days
        return (System.currentTimeMillis() - lastActive) < 604800000;
    }
    
    /**
     * Check if tax collection is due
     */
    public boolean isTaxDue() {
        return taxEnabled && taxRate > 0 && 
               (System.currentTimeMillis() - lastTaxCollection) >= 86400000; // 24 hours
    }
    
    /**
     * Calculate daily upkeep cost based on chunk count and base cost
     */
    public double calculateDailyUpkeep(double baseUpkeepPerChunk) {
        return chunks.size() * baseUpkeepPerChunk + upkeepCost;
    }
    
    /**
     * Calculate tax income from all players in territory
     */
    public double calculateTaxIncome() {
        if (!taxEnabled || taxRate <= 0) {
            return 0.0;
        }
        
        // This would integrate with economy system to calculate based on player activity
        return playerRoles.size() * 50.0 * taxRate; // Placeholder calculation
    }
    
    /**
     * Get formatted territory information
     */
    public List<String> getFormattedInfo() {
        List<String> info = new ArrayList<>();
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6&l📍 " + name));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6Country: &e" + countryName));
        info.add(ChatUtils.colorize("&6World: &e" + worldName));
        info.add(ChatUtils.colorize("&6Type: &e" + type.getDisplayName()));
        info.add(ChatUtils.colorize("&6Chunks: &e" + chunks.size()));
        info.add(ChatUtils.colorize("&6Sub-Areas: &e" + subAreas.size()));
        info.add(ChatUtils.colorize("&6Trusted Players: &e" + playerRoles.size()));
        info.add(ChatUtils.colorize("&6Claimed: &e" + getDaysSinceClaimed() + " days ago"));
        info.add(ChatUtils.colorize("&6Daily Upkeep: &e" + ChatUtils.formatCurrency(upkeepCost)));
        
        if (taxEnabled) {
            info.add(ChatUtils.colorize("&6Tax Rate: &e" + ChatUtils.formatPercentage(taxRate)));
            info.add(ChatUtils.colorize("&6Tax Income: &e" + ChatUtils.formatCurrency(calculateTaxIncome()) + "/day"));
        }
        
        info.add(ChatUtils.colorize("&6Public Access: " + (allowPublicAccess ? "&aYes" : "&cNo")));
        info.add(ChatUtils.colorize("&6Building: " + (allowBuilding ? "&aAllowed" : "&cRestricted")));
        info.add(ChatUtils.colorize("&6PvP: " + (allowPvP ? "&cEnabled" : "&aDisabled")));
        info.add(ChatUtils.colorize("&6Status: " + (isActive() ? "&aActive" : "&7Inactive")));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        if (!description.isEmpty()) {
            info.add(ChatUtils.colorize("&7" + description));
            info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        }
        
        return info;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Territory territory = (Territory) obj;
        return Objects.equals(name, territory.name) && 
               Objects.equals(countryName, territory.countryName) &&
               Objects.equals(worldName, territory.worldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, countryName, worldName);
    }
    
    @Override
    public String toString() {
        return String.format("Territory{name=%s, country=%s, chunks=%d}", 
                           name, countryName, chunks.size());
    }
}