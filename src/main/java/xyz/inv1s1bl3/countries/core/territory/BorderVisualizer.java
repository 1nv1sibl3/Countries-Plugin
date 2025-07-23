package xyz.inv1s1bl3.countries.core.territory;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles particle visualization for territory borders.
 */
public class BorderVisualizer {
    
    private final CountriesPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeVisualizations;
    private final Map<UUID, Set<ChunkCoordinate>> playerVisibleChunks;
    
    public BorderVisualizer(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.activeVisualizations = new ConcurrentHashMap<>();
        this.playerVisibleChunks = new ConcurrentHashMap<>();
    }
    
    /**
     * Show territory borders to a player
     */
    public void showBorders(Player player, Territory territory) {
        showBorders(player, territory, 10); // Default 10 second duration
    }
    
    /**
     * Show territory borders to a player with custom duration
     */
    public void showBorders(Player player, Territory territory, int durationSeconds) {
        if (!plugin.getConfigManager().getConfig().getBoolean("territory.particles.enabled", true)) {
            ChatUtils.sendError(player, "Particle borders are disabled!");
            return;
        }
        
        stopVisualization(player);
        
        Set<ChunkCoordinate> chunks = territory.getChunks();
        playerVisibleChunks.put(player.getUniqueId(), chunks);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                showChunkBorders(player, chunks, getParticleType(territory.getType()));
            }
        };
        
        activeVisualizations.put(player.getUniqueId(), task);
        
        // Show for configured duration
        task.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
        
        // Auto-stop after duration
        Bukkit.getScheduler().runTaskLater(plugin, () -> stopVisualization(player), durationSeconds * 20L);
        
        ChatUtils.sendSuccess(player, "Showing borders for " + territory.getName() + " (" + durationSeconds + "s)");
    }
    
    /**
     * Show selection borders to a player
     */
    public void showSelection(Player player, Location corner1, Location corner2) {
        if (!plugin.getConfigManager().getConfig().getBoolean("territory.particles.enabled", true)) {
            return;
        }
        
        stopVisualization(player);
        
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                
                showSelectionBorders(player, corner1, corner2);
            }
        };
        
        activeVisualizations.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 20L); // Every second
        
        // Auto-stop after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> stopVisualization(player), 600L);
    }
    
    /**
     * Show chunk borders with particles
     */
    private void showChunkBorders(Player player, Set<ChunkCoordinate> chunks, Particle particle) {
        World world = player.getWorld();
        
        for (ChunkCoordinate coord : chunks) {
            if (!coord.getWorldName().equals(world.getName())) {
                continue;
            }
            
            // Calculate chunk boundaries
            int chunkX = coord.getX() * 16;
            int chunkZ = coord.getZ() * 16;
            
            // Get height range
            int minY = plugin.getConfigManager().getConfig().getInt("territory.particles.min-height", 60);
            int maxY = plugin.getConfigManager().getConfig().getInt("territory.particles.max-height", 80);
            
            // Show corner particles
            for (int y = minY; y <= maxY; y += 5) {
                // Four corners of the chunk
                spawnParticle(player, world, chunkX, y, chunkZ, particle);
                spawnParticle(player, world, chunkX + 15, y, chunkZ, particle);
                spawnParticle(player, world, chunkX, y, chunkZ + 15, particle);
                spawnParticle(player, world, chunkX + 15, y, chunkZ + 15, particle);
            }
            
            // Show edge particles (less dense)
            for (int i = 0; i < 16; i += 4) {
                int y = world.getHighestBlockYAt(chunkX + i, chunkZ) + 1;
                spawnParticle(player, world, chunkX + i, y, chunkZ, particle);
                spawnParticle(player, world, chunkX + i, y, chunkZ + 15, particle);
                spawnParticle(player, world, chunkX, y, chunkZ + i, particle);
                spawnParticle(player, world, chunkX + 15, y, chunkZ + i, particle);
            }
        }
    }
    
    /**
     * Show selection area borders
     */
    private void showSelectionBorders(Player player, Location corner1, Location corner2) {
        if (corner1 == null || corner2 == null || !corner1.getWorld().equals(corner2.getWorld())) {
            return;
        }
        
        World world = corner1.getWorld();
        
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        
        Particle particle = Particle.VILLAGER_HAPPY;
        
        // Show corners
        spawnParticle(player, world, minX, minY, minZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, maxX, minY, minZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, minX, maxY, minZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, maxX, maxY, minZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, minX, minY, maxZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, maxX, minY, maxZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, minX, maxY, maxZ, Particle.HAPPY_VILLAGER);
        spawnParticle(player, world, maxX, maxY, maxZ, Particle.HAPPY_VILLAGER);
        
        // Show edges (sample points)
        int step = Math.max(1, (maxX - minX) / 10);
        for (int x = minX; x <= maxX; x += step) {
            spawnParticle(player, world, x, minY, minZ, particle);
            spawnParticle(player, world, x, minY, maxZ, particle);
            spawnParticle(player, world, x, maxY, minZ, particle);
            spawnParticle(player, world, x, maxY, maxZ, particle);
        }
        
        step = Math.max(1, (maxZ - minZ) / 10);
        for (int z = minZ; z <= maxZ; z += step) {
            spawnParticle(player, world, minX, minY, z, particle);
            spawnParticle(player, world, maxX, minY, z, particle);
            spawnParticle(player, world, minX, maxY, z, particle);
            spawnParticle(player, world, maxX, maxY, z, particle);
        }
    }
    
    /**
     * Spawn particle for player
     */
    private void spawnParticle(Player player, World world, int x, int y, int z, Particle particle) {
        Location loc = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        
        // Only show if player is within render distance
        if (player.getLocation().distance(loc) <= 100) {
            player.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * Get particle type based on territory type
     */
    private Particle getParticleType(TerritoryType type) {
        return switch (type) {
            case RESIDENTIAL -> Particle.HEART;
            case COMMERCIAL -> Particle.HAPPY_VILLAGER;
            case INDUSTRIAL -> Particle.SMOKE;
            case MILITARY -> Particle.CRIT;
            case AGRICULTURAL -> Particle.COMPOSTER;
            case RECREATIONAL -> Particle.NOTE;
            case GOVERNMENT -> Particle.ENCHANT;
            case WILDERNESS -> Particle.WATER_DROP;
        };
    }
    
    /**
     * Stop visualization for a player
     */
    public void stopVisualization(Player player) {
        BukkitRunnable task = activeVisualizations.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        playerVisibleChunks.remove(player.getUniqueId());
    }
    
    /**
     * Stop all visualizations
     */
    public void stopAllVisualizations() {
        for (BukkitRunnable task : activeVisualizations.values()) {
            task.cancel();
        }
        activeVisualizations.clear();
        playerVisibleChunks.clear();
    }
    
    /**
     * Check if player has active visualization
     */
    public boolean hasActiveVisualization(Player player) {
        return activeVisualizations.containsKey(player.getUniqueId());
    }
}