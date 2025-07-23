package xyz.inv1s1bl3.countries.core.territory;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;

/**
 * Represents a chunk coordinate in a specific world.
 */
public class ChunkCoordinate {
    
    private final String worldName;
    private final int x;
    private final int z;
    
    public ChunkCoordinate(String worldName, int x, int z) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
    }
    
    public ChunkCoordinate(Chunk chunk) {
        this.worldName = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }
    
    public ChunkCoordinate(Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getChunk().getX();
        this.z = location.getChunk().getZ();
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public int getX() {
        return x;
    }
    
    public int getZ() {
        return z;
    }
    
    /**
     * Get the chunk if the world is loaded
     */
    public Chunk getChunk(World world) {
        if (!world.getName().equals(worldName)) {
            return null;
        }
        return world.getChunkAt(x, z);
    }
    
    /**
     * Check if this coordinate is adjacent to another
     */
    public boolean isAdjacentTo(ChunkCoordinate other) {
        if (!worldName.equals(other.worldName)) {
            return false;
        }
        
        int deltaX = Math.abs(x - other.x);
        int deltaZ = Math.abs(z - other.z);
        
        return (deltaX == 1 && deltaZ == 0) || (deltaX == 0 && deltaZ == 1);
    }
    
    /**
     * Get distance to another chunk coordinate
     */
    public double distanceTo(ChunkCoordinate other) {
        if (!worldName.equals(other.worldName)) {
            return Double.MAX_VALUE;
        }
        
        int deltaX = x - other.x;
        int deltaZ = z - other.z;
        
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }
    
    /**
     * Convert to string format for storage
     */
    public String serialize() {
        return worldName + ":" + x + ":" + z;
    }
    
    /**
     * Create from serialized string
     */
    public static ChunkCoordinate deserialize(String serialized) {
        String[] parts = serialized.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid chunk coordinate format: " + serialized);
        }
        
        return new ChunkCoordinate(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChunkCoordinate that = (ChunkCoordinate) obj;
        return x == that.x && z == that.z && Objects.equals(worldName, that.worldName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(worldName, x, z);
    }
    
    @Override
    public String toString() {
        return String.format("ChunkCoordinate{world=%s, x=%d, z=%d}", worldName, x, z);
    }
}