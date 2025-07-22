package xyz.inv1s1bl3.countries.territory.protection;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;
import xyz.inv1s1bl3.countries.territory.ChunkManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced protection manager with role-based permissions and flags
 */
@RequiredArgsConstructor
public final class AdvancedProtectionManager {
    
    private final CountriesPlugin plugin;
    private final ChunkManager chunkManager;
    
    // Cache for territory permissions
    private final Map<String, Map<UUID, TerritoryPermissions>> territoryPermissionsCache = new ConcurrentHashMap<>();
    
    // Materials that require container access
    private static final Set<Material> CONTAINER_MATERIALS = Set.of(
        Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
        Material.BARREL, Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.BREWING_STAND, Material.ENCHANTING_TABLE, Material.ANVIL,
        Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL
    );
    
    // Materials that require button/lever interaction
    private static final Set<Material> BUTTON_MATERIALS = Set.of(
        Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
        Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON,
        Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON,
        Material.WARPED_BUTTON, Material.POLISHED_BLACKSTONE_BUTTON,
        Material.STONE_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
        Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
        Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
        Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
        Material.WARPED_PRESSURE_PLATE, Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
        Material.HEAVY_WEIGHTED_PRESSURE_PLATE, Material.LIGHT_WEIGHTED_PRESSURE_PLATE
    );
    
    // Door materials
    private static final Set<Material> DOOR_MATERIALS = Set.of(
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
     * Check if player can break a block
     * @param player Player attempting to break
     * @param block Block to break
     * @return true if allowed
     */
    public boolean canBreakBlock(final Player player, final Block block) {
        if (player.hasPermission("countries.bypass.block_break")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(block.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        return permissions.hasFlag(ProtectionFlag.BLOCK_BREAK);
    }
    
    /**
     * Check if player can place a block
     * @param player Player attempting to place
     * @param location Location to place at
     * @return true if allowed
     */
    public boolean canPlaceBlock(final Player player, final Location location) {
        if (player.hasPermission("countries.bypass.block_place")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        return permissions.hasFlag(ProtectionFlag.BLOCK_PLACE);
    }
    
    /**
     * Check if player can interact with a block
     * @param player Player attempting to interact
     * @param block Block to interact with
     * @param action Type of interaction
     * @return true if allowed
     */
    public boolean canInteractWithBlock(final Player player, final Block block, final Action action) {
        if (player.hasPermission("countries.bypass.block_interact")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(block.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        final Material material = block.getType();
        
        // Check specific interaction types
        if (CONTAINER_MATERIALS.contains(material)) {
            return permissions.hasFlag(ProtectionFlag.CONTAINER_ACCESS);
        }
        
        if (BUTTON_MATERIALS.contains(material)) {
            return permissions.hasFlag(ProtectionFlag.BUTTON_INTERACT);
        }
        
        if (DOOR_MATERIALS.contains(material)) {
            return permissions.hasFlag(ProtectionFlag.DOOR_INTERACT);
        }
        
        // General block interaction
        return permissions.hasFlag(ProtectionFlag.BLOCK_INTERACT);
    }
    
    /**
     * Check if player can damage an entity
     * @param player Player attempting to damage
     * @param entity Entity to damage
     * @return true if allowed
     */
    public boolean canDamageEntity(final Player player, final Entity entity) {
        if (player.hasPermission("countries.bypass.entity_damage")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(entity.getLocation().getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        
        // Check for PvP
        if (entity instanceof Player) {
            return permissions.hasFlag(ProtectionFlag.PVP);
        }
        
        // Check for animal damage
        if (this.isAnimal(entity.getType())) {
            return permissions.hasFlag(ProtectionFlag.ANIMAL_DAMAGE);
        }
        
        // Check for monster damage
        if (this.isMonster(entity.getType())) {
            return permissions.hasFlag(ProtectionFlag.MONSTER_DAMAGE);
        }
        
        // General entity damage
        return permissions.hasFlag(ProtectionFlag.ENTITY_DAMAGE);
    }
    
    /**
     * Check if player can interact with an entity
     * @param player Player attempting to interact
     * @param entity Entity to interact with
     * @return true if allowed
     */
    public boolean canInteractWithEntity(final Player player, final Entity entity) {
        if (player.hasPermission("countries.bypass.entity_interact")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(entity.getLocation().getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        return permissions.hasFlag(ProtectionFlag.ENTITY_INTERACT);
    }
    
    /**
     * Check if player can enter territory
     * @param player Player attempting to enter
     * @param location Location to enter
     * @return true if allowed
     */
    public boolean canEnterTerritory(final Player player, final Location location) {
        if (player.hasPermission("countries.bypass.land_enter")) {
            return true;
        }
        
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final TerritoryPermissions permissions = this.getPlayerPermissions(player.getUniqueId(), territoryOpt.get());
        return permissions.hasFlag(ProtectionFlag.LAND_ENTER);
    }
    
    /**
     * Check if explosions are allowed at location
     * @param location Location to check
     * @return true if explosions allowed
     */
    public boolean areExplosionsAllowed(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final TerritoryPermissions defaultPermissions = this.getDefaultTerritoryPermissions(territory);
        return defaultPermissions.hasFlag(ProtectionFlag.EXPLOSION_DAMAGE);
    }
    
    /**
     * Check if fire spread is allowed at location
     * @param location Location to check
     * @return true if fire spread allowed
     */
    public boolean isFireSpreadAllowed(final Location location) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final TerritoryPermissions defaultPermissions = this.getDefaultTerritoryPermissions(territory);
        return defaultPermissions.hasFlag(ProtectionFlag.FIRE_SPREAD);
    }
    
    /**
     * Check if mob spawning is allowed at location
     * @param location Location to check
     * @param entityType Entity type to spawn
     * @return true if spawning allowed
     */
    public boolean isMobSpawningAllowed(final Location location, final EntityType entityType) {
        final Optional<Territory> territoryOpt = this.chunkManager.getTerritoryAt(location.getChunk());
        if (territoryOpt.isEmpty()) {
            return true; // Unclaimed territory
        }
        
        final Territory territory = territoryOpt.get();
        final TerritoryPermissions defaultPermissions = this.getDefaultTerritoryPermissions(territory);
        
        if (this.isAnimal(entityType)) {
            return defaultPermissions.hasFlag(ProtectionFlag.ANIMAL_SPAWNING);
        } else if (this.isMonster(entityType)) {
            return defaultPermissions.hasFlag(ProtectionFlag.MOB_SPAWNING);
        }
        
        return true; // Allow other entities by default
    }
    
    /**
     * Get player permissions for a territory
     * @param playerUuid Player UUID
     * @param territory Territory
     * @return Territory permissions
     */
    public TerritoryPermissions getPlayerPermissions(final UUID playerUuid, final Territory territory) {
        final String territoryKey = territory.getId().toString();
        
        // Check cache first
        final Map<UUID, TerritoryPermissions> territoryCache = this.territoryPermissionsCache.get(territoryKey);
        if (territoryCache != null) {
            final TerritoryPermissions cached = territoryCache.get(playerUuid);
            if (cached != null) {
                return cached;
            }
        }
        
        // Calculate permissions
        final TerritoryPermissions permissions = this.calculatePlayerPermissions(playerUuid, territory);
        
        // Cache the result
        this.territoryPermissionsCache.computeIfAbsent(territoryKey, k -> new ConcurrentHashMap<>())
            .put(playerUuid, permissions);
        
        return permissions;
    }
    
    /**
     * Calculate player permissions for a territory
     * @param playerUuid Player UUID
     * @param territory Territory
     * @return Calculated permissions
     */
    private TerritoryPermissions calculatePlayerPermissions(final UUID playerUuid, final Territory territory) {
        // Get player data
        final Optional<xyz.inv1s1bl3.countries.database.entities.Player> playerDataOpt = 
            this.plugin.getCountryManager().getPlayer(playerUuid);
        
        if (playerDataOpt.isEmpty()) {
            // Unknown player - visitor permissions
            return TerritoryPermissions.builder()
                .territoryId(territory.getId())
                .playerUuid(playerUuid)
                .role(TerritoryRole.VISITOR)
                .build();
        }
        
        final xyz.inv1s1bl3.countries.database.entities.Player playerData = playerDataOpt.get();
        
        // Check if player is territory claimer
        if (territory.wasClaimedBy(playerUuid)) {
            final TerritoryPermissions permissions = TerritoryPermissions.builder()
                .territoryId(territory.getId())
                .playerUuid(playerUuid)
                .role(TerritoryRole.OWNER)
                .build();
            permissions.initializeWithDefaults();
            return permissions;
        }
        
        // Check if player is in the same country
        if (playerData.hasCountry() && playerData.getCountryId().equals(territory.getCountryId())) {
            final TerritoryRole role = playerData.hasAdminPermissions() ? TerritoryRole.MEMBER : TerritoryRole.MEMBER;
            final TerritoryPermissions permissions = TerritoryPermissions.builder()
                .territoryId(territory.getId())
                .playerUuid(playerUuid)
                .role(role)
                .build();
            permissions.initializeWithDefaults();
            return permissions;
        }
        
        // Check if player is from allied country
        // TODO: Implement when diplomacy system is ready
        
        // Default to visitor
        final TerritoryPermissions permissions = TerritoryPermissions.builder()
            .territoryId(territory.getId())
            .playerUuid(playerUuid)
            .role(TerritoryRole.VISITOR)
            .build();
        permissions.initializeWithDefaults();
        return permissions;
    }
    
    /**
     * Get default territory permissions (for environmental checks)
     * @param territory Territory
     * @return Default permissions
     */
    private TerritoryPermissions getDefaultTerritoryPermissions(final Territory territory) {
        final TerritoryPermissions permissions = TerritoryPermissions.builder()
            .territoryId(territory.getId())
            .role(TerritoryRole.VISITOR)
            .build();
        permissions.initializeWithDefaults();
        
        // Apply territory type specific defaults
        // TODO: Load from territory configuration
        
        return permissions;
    }
    
    /**
     * Set player role in territory
     * @param territoryId Territory ID
     * @param playerUuid Player UUID
     * @param role New role
     * @param setterUuid Player setting the role
     * @return true if successful
     */
    public boolean setPlayerRole(final Integer territoryId, final UUID playerUuid, 
                                final TerritoryRole role, final UUID setterUuid) {
        // Get territory
        final Optional<Territory> territoryOpt = this.plugin.getTerritoryManager()
            .getTerritoryRepository().findById(territoryId);
        
        if (territoryOpt.isEmpty()) {
            return false;
        }
        
        final Territory territory = territoryOpt.get();
        
        // Check if setter has permission
        final TerritoryPermissions setterPermissions = this.getPlayerPermissions(setterUuid, territory);
        if (!setterPermissions.canModifyRole(role)) {
            return false;
        }
        
        // Update permissions
        final TerritoryPermissions permissions = TerritoryPermissions.builder()
            .territoryId(territoryId)
            .playerUuid(playerUuid)
            .role(role)
            .build();
        permissions.initializeWithDefaults();
        
        // Cache the new permissions
        final String territoryKey = territoryId.toString();
        this.territoryPermissionsCache.computeIfAbsent(territoryKey, k -> new ConcurrentHashMap<>())
            .put(playerUuid, permissions);
        
        // TODO: Save to database when territory permissions table is implemented
        
        return true;
    }
    
    /**
     * Clear territory permissions cache
     * @param territoryId Territory ID
     */
    public void clearTerritoryCache(final Integer territoryId) {
        this.territoryPermissionsCache.remove(territoryId.toString());
    }
    
    /**
     * Clear all permissions cache
     */
    public void clearAllCache() {
        this.territoryPermissionsCache.clear();
    }
    
    /**
     * Check if entity type is an animal
     * @param entityType Entity type
     * @return true if animal
     */
    private boolean isAnimal(final EntityType entityType) {
        return switch (entityType) {
            case COW, PIG, SHEEP, CHICKEN, HORSE, DONKEY, MULE, LLAMA, TRADER_LLAMA,
                 CAT, WOLF, PARROT, RABBIT, TURTLE, PANDA, FOX, BEE, STRIDER,
                 HOGLIN, AXOLOTL, GOAT, FROG, TADPOLE, ALLAY, CAMEL, SNIFFER -> true;
            default -> false;
        };
    }
    
    /**
     * Check if entity type is a monster
     * @param entityType Entity type
     * @return true if monster
     */
    private boolean isMonster(final EntityType entityType) {
        return switch (entityType) {
            case ZOMBIE, SKELETON, CREEPER, SPIDER, ENDERMAN, WITCH, SLIME,
                 MAGMA_CUBE, BLAZE, GHAST, WITHER_SKELETON, ZOMBIE_PIGMAN,
                 ENDER_DRAGON, WITHER, GUARDIAN, ELDER_GUARDIAN, SHULKER,
                 VEX, VINDICATOR, EVOKER, ILLUSIONER, RAVAGER, PILLAGER,
                 ZOMBIFIED_PIGLIN, PIGLIN, PIGLIN_BRUTE, ZOGLIN, WARDEN -> true;
            default -> false;
        };
    }
}