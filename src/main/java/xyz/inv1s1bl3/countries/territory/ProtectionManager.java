package xyz.inv1s1bl3.countries.territory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.territory.protection.AdvancedProtectionManager;

/**
 * Manages territory protection and restrictions (wrapper for advanced protection)
 */
@RequiredArgsConstructor
public final class ProtectionManager {
    
    private final CountriesPlugin plugin;
    private final AdvancedProtectionManager advancedProtectionManager;
    
    /**
     * Check if a player can break a block
     * @param player Player attempting to break
     * @param block Block to break
     * @return true if allowed
     */
    public boolean canBreakBlock(final Player player, final Block block) {
        return this.advancedProtectionManager.canBreakBlock(player, block);
    }
    
    /**
     * Check if a player can place a block
     * @param player Player attempting to place
     * @param location Location to place at
     * @return true if allowed
     */
    public boolean canPlaceBlock(final Player player, final Location location) {
        return this.advancedProtectionManager.canPlaceBlock(player, location);
    }
    
    /**
     * Check if a player can interact with a block
     * @param player Player attempting to interact
     * @param block Block to interact with
     * @param action Type of interaction
     * @return true if allowed
     */
    public boolean canInteract(final Player player, final Block block, final Action action) {
        return this.advancedProtectionManager.canInteractWithBlock(player, block, action);
    }
    
    /**
     * Check if PvP is allowed at a location
     * @param location Location to check
     * @return true if PvP is allowed
     */
    public boolean isPvpAllowed(final Location location) {
        return this.advancedProtectionManager.areExplosionsAllowed(location);
    }
    
    /**
     * Check if explosions are allowed at a location
     * @param location Location to check
     * @return true if explosions are allowed
     */
    public boolean areExplosionsAllowed(final Location location) {
        return this.advancedProtectionManager.areExplosionsAllowed(location);
    }
    
    /**
     * Check if fire spread is allowed at a location
     * @param location Location to check
     * @return true if fire spread is allowed
     */
    public boolean isFireSpreadAllowed(final Location location) {
        return this.advancedProtectionManager.isFireSpreadAllowed(location);
    }
    
    /**
     * Check if mob spawning is allowed at a location
     * @param location Location to check
     * @param entityType Entity type to spawn
     * @return true if spawning is allowed
     */
    public boolean isMobSpawningAllowed(final Location location, final org.bukkit.entity.EntityType entityType) {
        return this.advancedProtectionManager.isMobSpawningAllowed(location, entityType);
    }
    
    /**
     * Check if player can enter territory
     * @param player Player attempting to enter
     * @param location Location to enter
     * @return true if allowed
     */
    public boolean canEnterTerritory(final Player player, final Location location) {
        return this.advancedProtectionManager.canEnterTerritory(player, location);
    }
    
    /**
     * Get protection status message for a location
     * @param location Location to check
     * @return Protection status description
     */
    public String getProtectionStatus(final Location location) {
        // TODO: Implement detailed protection status with new system
        return "Protection status display not yet implemented with new system.";
    }
}