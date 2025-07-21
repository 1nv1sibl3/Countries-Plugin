package xyz.inv1s1bl3.countries.core.territory;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a territory in the plugin.
 * Contains chunks claimed by a country with protection and management features.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Territory {
    
    @Expose
    @EqualsAndHashCode.Include
    private final String name;
    
    @Expose
    private final String countryName;
    
    @Expose
    private final UUID ownerId;
    
    @Expose
    private final LocalDateTime claimedDate;
    
    @Expose
    private TerritoryType type;
    
    @Expose
    private String description;
    
    @Expose
    private final Set<ChunkCoordinate> chunks;
    
    @Expose
    private final Map<String, Boolean> permissions;
    
    @Expose
    private final Map<String, Object> settings;
    
    @Expose
    private boolean isActive;
    
    @Expose
    private LocalDateTime lastActivity;
    
    @Expose
    private double maintenanceCost;
    
    /**
     * Create a new territory.
     * 
     * @param name the territory name
     * @param countryName the owning country name
     * @param ownerId the owner's UUID
     * @param type the territory type
     */
    public Territory(@NotNull final String name,
                    @NotNull final String countryName,
                    @NotNull final UUID ownerId,
                    @NotNull final TerritoryType type) {
        this.name = name;
        this.countryName = countryName;
        this.ownerId = ownerId;
        this.type = type;
        this.claimedDate = LocalDateTime.now();
        this.description = "A territory of " + countryName;
        this.chunks = new HashSet<>();
        this.permissions = new HashMap<>();
        this.settings = new HashMap<>();
        this.isActive = true;
        this.lastActivity = LocalDateTime.now();
        this.maintenanceCost = type.getMaintenanceCost();
        
        // Initialize default permissions
        this.initializeDefaultPermissions();
    }
    
    /**
     * Initialize default permissions for the territory.
     */
    private void initializeDefaultPermissions() {
        this.permissions.put("build", true);
        this.permissions.put("break", true);
        this.permissions.put("interact", true);
        this.permissions.put("pvp", false);
        this.permissions.put("mob_spawning", true);
        this.permissions.put("fire_spread", false);
        this.permissions.put("explosion_damage", false);
    }
    
    /**
     * Add a chunk to the territory.
     * 
     * @param chunk the chunk to add
     * @return true if added successfully
     */
    public boolean addChunk(@NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = new ChunkCoordinate(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        
        if (this.chunks.add(coordinate)) {
            this.updateActivity();
            return true;
        }
        return false;
    }
    
    /**
     * Remove a chunk from the territory.
     * 
     * @param chunk the chunk to remove
     * @return true if removed successfully
     */
    public boolean removeChunk(@NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = new ChunkCoordinate(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        
        if (this.chunks.remove(coordinate)) {
            this.updateActivity();
            return true;
        }
        return false;
    }
    
    /**
     * Check if a chunk is part of this territory.
     * 
     * @param chunk the chunk to check
     * @return true if the chunk is claimed
     */
    public boolean containsChunk(@NotNull final Chunk chunk) {
        final ChunkCoordinate coordinate = new ChunkCoordinate(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        return this.chunks.contains(coordinate);
    }
    
    /**
     * Check if a location is within this territory.
     * 
     * @param location the location to check
     * @return true if the location is within the territory
     */
    public boolean containsLocation(@NotNull final Location location) {
        final World world = location.getWorld();
        if (world == null) {
            return false;
        }
        
        final Chunk chunk = location.getChunk();
        return this.containsChunk(chunk);
    }
    
    /**
     * Get the center location of the territory.
     * 
     * @return the center location, or null if no chunks
     */
    @Nullable
    public Location getCenterLocation() {
        if (this.chunks.isEmpty()) {
            return null;
        }
        
        int totalX = 0;
        int totalZ = 0;
        String worldName = null;
        
        for (final ChunkCoordinate coord : this.chunks) {
            totalX += coord.getX();
            totalZ += coord.getZ();
            if (worldName == null) {
                worldName = coord.getWorldName();
            }
        }
        
        final int centerX = (totalX / this.chunks.size()) * 16 + 8;
        final int centerZ = (totalZ / this.chunks.size()) * 16 + 8;
        
        // Find the highest block at the center
        final World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        
        final int centerY = world.getHighestBlockYAt(centerX, centerZ);
        return new Location(world, centerX, centerY, centerZ);
    }
    
    /**
     * Get the total area of the territory in chunks.
     * 
     * @return the number of chunks
     */
    public int getChunkCount() {
        return this.chunks.size();
    }
    
    /**
     * Get the total area of the territory in blocks.
     * 
     * @return the number of blocks (chunks * 256)
     */
    public int getBlockCount() {
        return this.chunks.size() * 256;
    }
    
    /**
     * Check if a permission is enabled.
     * 
     * @param permission the permission name
     * @return true if enabled
     */
    public boolean hasPermission(@NotNull final String permission) {
        return this.permissions.getOrDefault(permission, false);
    }
    
    /**
     * Set a permission value.
     * 
     * @param permission the permission name
     * @param value the permission value
     */
    public void setPermission(@NotNull final String permission, final boolean value) {
        this.permissions.put(permission, value);
        this.updateActivity();
    }
    
    /**
     * Get a setting value.
     * 
     * @param key the setting key
     * @param defaultValue the default value
     * @param <T> the value type
     * @return the setting value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(@NotNull final String key, @NotNull final T defaultValue) {
        final Object value = this.settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (final ClassCastException exception) {
            return defaultValue;
        }
    }
    
    /**
     * Set a setting value.
     * 
     * @param key the setting key
     * @param value the setting value
     */
    public void setSetting(@NotNull final String key, @NotNull final Object value) {
        this.settings.put(key, value);
        this.updateActivity();
    }
    
    /**
     * Update the last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Check if the territory has been inactive for a certain number of days.
     * 
     * @param days the number of days
     * @return true if inactive for the specified days
     */
    public boolean isInactiveFor(final int days) {
        return this.lastActivity.isBefore(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * Calculate the daily maintenance cost for this territory.
     * 
     * @return the maintenance cost
     */
    public double calculateMaintenanceCost() {
        final double baseCost = this.type.getMaintenanceCost();
        final double chunkMultiplier = this.chunks.size() * 0.1;
        return baseCost + chunkMultiplier;
    }
    
    /**
     * Get all chunk coordinates as a list.
     * 
     * @return list of chunk coordinates
     */
    @NotNull
    public List<ChunkCoordinate> getChunkList() {
        return new ArrayList<>(this.chunks);
    }
    
    /**
     * Get the border chunks of the territory.
     * 
     * @return set of border chunk coordinates
     */
    @NotNull
    public Set<ChunkCoordinate> getBorderChunks() {
        final Set<ChunkCoordinate> borderChunks = new HashSet<>();
        
        for (final ChunkCoordinate chunk : this.chunks) {
            // Check if any adjacent chunk is not part of the territory
            final ChunkCoordinate[] adjacent = {
                new ChunkCoordinate(chunk.getWorldName(), chunk.getX() + 1, chunk.getZ()),
                new ChunkCoordinate(chunk.getWorldName(), chunk.getX() - 1, chunk.getZ()),
                new ChunkCoordinate(chunk.getWorldName(), chunk.getX(), chunk.getZ() + 1),
                new ChunkCoordinate(chunk.getWorldName(), chunk.getX(), chunk.getZ() - 1)
            };
            
            for (final ChunkCoordinate adj : adjacent) {
                if (!this.chunks.contains(adj)) {
                    borderChunks.add(chunk);
                    break;
                }
            }
        }
        
        return borderChunks;
    }
}