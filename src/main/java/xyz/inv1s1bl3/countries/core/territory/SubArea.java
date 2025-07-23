package xyz.inv1s1bl3.countries.core.territory;

import org.bukkit.Location;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a sub-area within a territory with specific permissions and settings.
 */
public class SubArea {
    
    private final String name;
    private final String territoryName;
    private final String worldName;
    private Location corner1;
    private Location corner2;
    private final long createdDate;
    
    // Permissions
    private final Map<UUID, TerritoryRole> playerRoles;
    private final Map<TerritoryRole, Map<TerritoryFlag, Boolean>> roleFlags;
    
    // Settings
    private String description;
    private boolean forRent;
    private double rentPrice;
    private long rentDuration; // in milliseconds
    private UUID currentTenant;
    private long rentExpiry;
    
    // Messages
    private String enterMessage;
    private String leaveMessage;
    
    public SubArea(String name, String territoryName, String worldName, 
                   Location corner1, Location corner2) {
        this.name = name;
        this.territoryName = territoryName;
        this.worldName = worldName;
        this.corner1 = corner1;
        this.corner2 = corner2;
        this.createdDate = System.currentTimeMillis();
        
        this.playerRoles = new ConcurrentHashMap<>();
        this.roleFlags = new ConcurrentHashMap<>();
        
        this.description = "A sub-area within " + territoryName;
        this.forRent = false;
        this.rentPrice = 0.0;
        this.rentDuration = 2592000000L; // 30 days default
        this.currentTenant = null;
        this.rentExpiry = 0L;
        
        this.enterMessage = "";
        this.leaveMessage = "";
        
        // Initialize default role flags
        initializeDefaultRoleFlags();
    }
    
    private void initializeDefaultRoleFlags() {
        for (TerritoryRole role : TerritoryRole.values()) {
            roleFlags.put(role, new EnumMap<>(role.getDefaultFlags()));
        }
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public String getTerritoryName() {
        return territoryName;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public Location getCorner1() {
        return corner1;
    }
    
    public Location getCorner2() {
        return corner2;
    }
    
    public long getCreatedDate() {
        return createdDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isForRent() {
        return forRent;
    }
    
    public double getRentPrice() {
        return rentPrice;
    }
    
    public long getRentDuration() {
        return rentDuration;
    }
    
    public UUID getCurrentTenant() {
        return currentTenant;
    }
    
    public long getRentExpiry() {
        return rentExpiry;
    }
    
    public String getEnterMessage() {
        return enterMessage;
    }
    
    public String getLeaveMessage() {
        return leaveMessage;
    }
    
    // Setters
    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }
    
    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }
    
    public void setForRent(boolean forRent) {
        this.forRent = forRent;
    }
    
    public void setRentPrice(double rentPrice) {
        this.rentPrice = Math.max(0, rentPrice);
    }
    
    public void setRentDuration(long rentDuration) {
        this.rentDuration = Math.max(0, rentDuration);
    }
    
    public void setCurrentTenant(UUID currentTenant) {
        this.currentTenant = currentTenant;
        if (currentTenant != null) {
            this.rentExpiry = System.currentTimeMillis() + rentDuration;
            playerRoles.put(currentTenant, TerritoryRole.TENANT);
        } else {
            this.rentExpiry = 0L;
        }
    }
    
    public void setEnterMessage(String enterMessage) {
        this.enterMessage = enterMessage != null ? enterMessage : "";
    }
    
    public void setLeaveMessage(String leaveMessage) {
        this.leaveMessage = leaveMessage != null ? leaveMessage : "";
    }
    
    /**
     * Check if a location is within this sub-area
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }
        
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        
        return location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
    }
    
    /**
     * Get player's role in this sub-area
     */
    public TerritoryRole getPlayerRole(UUID playerUUID) {
        return playerRoles.getOrDefault(playerUUID, TerritoryRole.VISITOR);
    }
    
    /**
     * Set player's role in this sub-area
     */
    public void setPlayerRole(UUID playerUUID, TerritoryRole role) {
        if (role == TerritoryRole.VISITOR) {
            playerRoles.remove(playerUUID);
        } else {
            playerRoles.put(playerUUID, role);
        }
    }
    
    /**
     * Check if player has a specific flag permission
     */
    public boolean hasFlag(UUID playerUUID, TerritoryFlag flag) {
        TerritoryRole role = getPlayerRole(playerUUID);
        Map<TerritoryFlag, Boolean> flags = roleFlags.get(role);
        return flags != null && flags.getOrDefault(flag, false);
    }
    
    /**
     * Set flag for a specific role
     */
    public void setRoleFlag(TerritoryRole role, TerritoryFlag flag, boolean value) {
        roleFlags.computeIfAbsent(role, k -> new EnumMap<>(TerritoryFlag.class))
                 .put(flag, value);
    }
    
    /**
     * Get all flags for a role
     */
    public Map<TerritoryFlag, Boolean> getRoleFlags(TerritoryRole role) {
        return new EnumMap<>(roleFlags.getOrDefault(role, new EnumMap<>(TerritoryFlag.class)));
    }
    
    /**
     * Check if rent is expired
     */
    public boolean isRentExpired() {
        return currentTenant != null && System.currentTimeMillis() > rentExpiry;
    }
    
    /**
     * Get days until rent expires
     */
    public long getDaysUntilRentExpiry() {
        if (currentTenant == null) {
            return 0;
        }
        long remaining = rentExpiry - System.currentTimeMillis();
        return Math.max(0, remaining / 86400000L);
    }
    
    /**
     * Calculate area size in blocks
     */
    public int getAreaSize() {
        int width = (int) Math.abs(corner2.getX() - corner1.getX()) + 1;
        int height = (int) Math.abs(corner2.getY() - corner1.getY()) + 1;
        int depth = (int) Math.abs(corner2.getZ() - corner1.getZ()) + 1;
        return width * height * depth;
    }
    
    /**
     * Get center location of the sub-area
     */
    public Location getCenterLocation() {
        double centerX = (corner1.getX() + corner2.getX()) / 2;
        double centerY = (corner1.getY() + corner2.getY()) / 2;
        double centerZ = (corner1.getZ() + corner2.getZ()) / 2;
        return new Location(corner1.getWorld(), centerX, centerY, centerZ);
    }
    
    /**
     * Get formatted sub-area information
     */
    public List<String> getFormattedInfo() {
        List<String> info = new ArrayList<>();
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6&l📦 " + name));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6Territory: &e" + territoryName));
        info.add(ChatUtils.colorize("&6World: &e" + worldName));
        info.add(ChatUtils.colorize("&6Size: &e" + getAreaSize() + " blocks"));
        info.add(ChatUtils.colorize("&6For Rent: " + (forRent ? "&aYes" : "&cNo")));
        
        if (forRent) {
            info.add(ChatUtils.colorize("&6Rent Price: &e" + ChatUtils.formatCurrency(rentPrice)));
            info.add(ChatUtils.colorize("&6Rent Duration: &e" + (rentDuration / 86400000L) + " days"));
            
            if (currentTenant != null) {
                info.add(ChatUtils.colorize("&6Current Tenant: &e" + currentTenant));
                info.add(ChatUtils.colorize("&6Rent Expires: &e" + getDaysUntilRentExpiry() + " days"));
            } else {
                info.add(ChatUtils.colorize("&6Status: &aAvailable"));
            }
        }
        
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
        SubArea subArea = (SubArea) obj;
        return Objects.equals(name, subArea.name) && 
               Objects.equals(territoryName, subArea.territoryName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name, territoryName);
    }
    
    @Override
    public String toString() {
        return String.format("SubArea{name=%s, territory=%s, size=%d}", 
                           name, territoryName, getAreaSize());
    }
}