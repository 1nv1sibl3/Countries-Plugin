package xyz.inv1s1bl3.countries.core.territory;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a chunk coordinate in the world.
 * Used for territory management and chunk tracking.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode
public final class ChunkCoordinate {
    
    @Expose
    private final String worldName;
    
    @Expose
    private final int x;
    
    @Expose
    private final int z;
    
    /**
     * Create a new chunk coordinate.
     * 
     * @param worldName the world name
     * @param x the chunk X coordinate
     * @param z the chunk Z coordinate
     */
    public ChunkCoordinate(@NotNull final String worldName, final int x, final int z) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }
    
    /**
     * Create a chunk coordinate from a Bukkit chunk.
     * 
     * @param chunk the Bukkit chunk
     * @return the chunk coordinate
     */
    @NotNull
    public static ChunkCoordinate fromChunk(@NotNull final Chunk chunk) {
        return new ChunkCoordinate(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }
    
    /**
     * Get the Bukkit chunk for this coordinate.
     * 
     * @return the Bukkit chunk, or null if world not found
     */
    @Nullable
    public Chunk getChunk() {
        final World world = Bukkit.getWorld(this.worldName);
        if (world == null) {
            return null;
        }
        return world.getChunkAt(this.x, this.z);
    }
    
    /**
     * Get the world for this coordinate.
     * 
     * @return the world, or null if not found
     */
    @Nullable
    public World getWorld() {
        return Bukkit.getWorld(this.worldName);
    }
    
    /**
     * Check if this chunk is adjacent to another chunk.
     * 
     * @param other the other chunk coordinate
     * @return true if adjacent
     */
    public boolean isAdjacentTo(@NotNull final ChunkCoordinate other) {
        if (!this.worldName.equals(other.worldName)) {
            return false;
        }
        
        final int deltaX = Math.abs(this.x - other.x);
        final int deltaZ = Math.abs(this.z - other.z);
        
        return (deltaX == 1 && deltaZ == 0) || (deltaX == 0 && deltaZ == 1);
    }
    
    /**
     * Get the distance to another chunk coordinate.
     * 
     * @param other the other chunk coordinate
     * @return the distance in chunks
     */
    public double getDistance(@NotNull final ChunkCoordinate other) {
        if (!this.worldName.equals(other.worldName)) {
            return Double.MAX_VALUE;
        }
        
        final int deltaX = this.x - other.x;
        final int deltaZ = this.z - other.z;
        
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    /**
     * Get all adjacent chunk coordinates.
     * 
     * @return array of adjacent coordinates
     */
    @NotNull
    public ChunkCoordinate[] getAdjacentChunks() {
        return new ChunkCoordinate[] {
            new ChunkCoordinate(this.worldName, this.x + 1, this.z),
            new ChunkCoordinate(this.worldName, this.x - 1, this.z),
            new ChunkCoordinate(this.worldName, this.x, this.z + 1),
            new ChunkCoordinate(this.worldName, this.x, this.z - 1)
        };
    }
    
    /**
     * Convert to a string representation.
     * 
     * @return string representation
     */
    @Override
    @NotNull
    public String toString() {
        return String.format("%s:%d,%d", this.worldName, this.x, this.z);
    }
    
    /**
     * Parse a chunk coordinate from string.
     * 
     * @param str the string representation
     * @return the chunk coordinate, or null if invalid
     */
    @Nullable
    public static ChunkCoordinate fromString(@NotNull final String str) {
        try {
            final String[] parts = str.split(":");
            if (parts.length != 2) {
                return null;
            }
            
            final String worldName = parts[0];
            final String[] coords = parts[1].split(",");
            if (coords.length != 2) {
                return null;
            }
            
            final int x = Integer.parseInt(coords[0]);
            final int z = Integer.parseInt(coords[1]);
            
            return new ChunkCoordinate(worldName, x, z);
            
        } catch (final NumberFormatException exception) {
            return null;
        }
    }
}