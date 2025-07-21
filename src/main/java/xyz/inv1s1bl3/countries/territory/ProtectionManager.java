package xyz.inv1s1bl3.countries.territory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;

import java.util.Optional;
import java.util.Set;

/**
 * Manages territory protection and restrictions
 */
@RequiredArgsConstructor
public final class ProtectionManager {
    
    private final CountriesPlugin plugin;
    private final ChunkManager chunkManager;
    
    // Materials that require interaction permission
    private static final Set<Material> INTERACTION_MATERIALS = Set.of(
        Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.BREWING_STAND, Material.ENCHANTING_TABLE,
        Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
        Material.CRAFTING_TABLE, Material.CARTOGRAPHY_TABLE,
        Material.FLETCHING_TABLE, Material.SMITHING_TABLE,
        Material.STONECUTTER, Material.LOOM, Material.COMPOSTER,
        Material.BARREL, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
        Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
        Material.REDSTONE_WIRE, Material.REPEATER, Material.COMPARATOR,
        Material.DAYLIGHT_DETECTOR, Material.OBSERVER,
        Material.IRON_DOOR, Material.OAK_DOOR, Material.SPRUCE_DOOR,
        Material.BIRCH_DOOR, Material.JUNGLE_DOOR, Material.ACACIA_DOOR,
        Material.DARK_OAK_DOOR, Material.CRIMSON_DOOR, Material.WARPED_DOOR,
        Material.IRON_TRAPDOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR,
        Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR,
        Material.DARK_OAK_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.WARPED_TRAPDOOR,
        Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
        Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
        Material.CRIMSON_FENCE_GATE, Material.WARPED_FENCE_GATE
    );
    
    /**
     * Check if a player can break a block
     * @param player Player attempting to break
     * @param block Block to break
     * @return true if allowed
     */
    public boolean canBreakBlock(final Player player, final Block block) {
        if (player.hasPermission("countries.bypass")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(block.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        if (typeOpt.isEmpty()) {
            return false;
        }
        
        final TerritoryType type = typeOpt.get();
        
        // Check if territory type allows breaking
        if (!type.canBreak()) {
            return false;
        }
        
        // Check if player is a member of the country
        return this.chunkManager.canPlayerBuild(player, block.getLocation());
    }
    
    /**
     * Check if a player can place a block
     * @param player Player attempting to place
     * @param location Location to place at
     * @return true if allowed
     */
    public boolean canPlaceBlock(final Player player, final Location location) {
        if (player.hasPermission("countries.bypass")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        if (typeOpt.isEmpty()) {
            return false;
        }
        
        final TerritoryType type = typeOpt.get();
        
        // Check if territory type allows building
        if (!type.canBuild()) {
            return false;
        }
        
        // Check if player is a member of the country
        return this.chunkManager.canPlayerBuild(player, location);
    }
    
    /**
     * Check if a player can interact with a block
     * @param player Player attempting to interact
     * @param block Block to interact with
     * @param action Type of interaction
     * @return true if allowed
     */
    public boolean canInteract(final Player player, final Block block, final Action action) {
        if (player.hasPermission("countries.bypass")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(block.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        
        // Check if the block requires interaction permission
        if (!INTERACTION_MATERIALS.contains(block.getType())) {
            return true; // Not a protected interaction
        }
        
        // Check if player is a member of the country
        return this.chunkManager.canPlayerBuild(player, block.getLocation());
    }
    
    /**
     * Check if PvP is allowed at a location
     * @param location Location to check
     * @return true if PvP is allowed
     */
    public boolean isPvpAllowed(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory - use world settings
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        return typeOpt.map(TerritoryType::allowPvp).orElse(false);
    }
    
    /**
     * Check if explosions are allowed at a location
     * @param location Location to check
     * @return true if explosions are allowed
     */
    public boolean areExplosionsAllowed(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        return typeOpt.map(TerritoryType::allowExplosions).orElse(false);
    }
    
    /**
     * Check if fire spread is allowed at a location
     * @param location Location to check
     * @return true if fire spread is allowed
     */
    public boolean isFireSpreadAllowed(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        return typeOpt.map(TerritoryType::allowFireSpread).orElse(false);
    }
    
    /**
     * Check if mob spawning is allowed at a location
     * @param location Location to check
     * @param isHostile Whether the mob is hostile
     * @return true if spawning is allowed
     */
    public boolean isMobSpawningAllowed(final Location location, final boolean isHostile) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        if (typeOpt.isEmpty()) {
            return true;
        }
        
        final TerritoryType type = typeOpt.get();
        
        if (isHostile) {
            return type.allowsMobSpawning();
        } else {
            return type.allowsAnimalSpawning();
        }
    }
    
    /**
     * Check if a player can damage an entity
     * @param player Player attempting to damage
     * @param entity Entity to damage
     * @return true if allowed
     */
    public boolean canDamageEntity(final Player player, final Entity entity) {
        if (player.hasPermission("countries.bypass")) {
            return true;
        }
        
        final Location location = entity.getLocation();
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        // If it's another player, check PvP rules
        if (entity instanceof Player) {
            return this.isPvpAllowed(location);
        }
        
        // For other entities, check if player is a member of the country
        return this.chunkManager.canPlayerBuild(player, location);
    }
    
    /**
     * Get protection status message for a location
     * @param location Location to check
     * @return Protection status description
     */
    public String getProtectionStatus(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        
        if (territoryOpt.isEmpty()) {
            return "This area is unclaimed and unprotected.";
        }
        
        final Territory territory = territoryOpt.get();
        final Optional<xyz.inv1s1bl3.countries.database.entities.Country> countryOpt = 
            this.plugin.getCountryManager().getCountry(territory.getCountryId());
        
        if (countryOpt.isEmpty()) {
            return "This area has unknown protection status.";
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Country country = countryOpt.get();
        final Optional<TerritoryType> typeOpt = TerritoryType.fromKey(territory.getTerritoryType());
        
        final StringBuilder status = new StringBuilder();
        status.append("This ").append(typeOpt.map(TerritoryType::getDisplayName).orElse("territory"))
              .append(" belongs to ").append(country.getFormattedName()).append(".");
        
        if (typeOpt.isPresent()) {
            final TerritoryType type = typeOpt.get();
            status.append("\nProtections: ");
            
            if (!type.canBuild()) status.append("No Building ");
            if (!type.canBreak()) status.append("No Breaking ");
            if (!type.allowPvp()) status.append("No PvP ");
            if (!type.allowExplosions()) status.append("No Explosions ");
            if (!type.allowFireSpread()) status.append("No Fire Spread ");
        }
        
        return status.toString();
    }
}