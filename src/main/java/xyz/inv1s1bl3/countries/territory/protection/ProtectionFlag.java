package xyz.inv1s1bl3.countries.territory.protection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enumeration of protection flags for territories
 */
@Getter
@RequiredArgsConstructor
public enum ProtectionFlag {
    
    // Block interaction flags
    BLOCK_BREAK("block_break", "Block Breaking", "Allow breaking blocks", true),
    BLOCK_PLACE("block_place", "Block Placing", "Allow placing blocks", true),
    BLOCK_INTERACT("block_interact", "Block Interaction", "Allow interacting with blocks", true),
    
    // Entity flags
    ENTITY_DAMAGE("entity_damage", "Entity Damage", "Allow damaging entities", true),
    ENTITY_INTERACT("entity_interact", "Entity Interaction", "Allow interacting with entities", true),
    ANIMAL_DAMAGE("animal_damage", "Animal Damage", "Allow damaging animals", false),
    MONSTER_DAMAGE("monster_damage", "Monster Damage", "Allow damaging monsters", true),
    
    // PvP flags
    PVP("pvp", "Player vs Player", "Allow player combat", false),
    PVP_PROTECTION("pvp_protection", "PvP Protection", "Protect from PvP", true),
    
    // Environmental flags
    FIRE_SPREAD("fire_spread", "Fire Spread", "Allow fire to spread", false),
    EXPLOSION_DAMAGE("explosion_damage", "Explosion Damage", "Allow explosion damage", false),
    EXPLOSION_BLOCK_DAMAGE("explosion_block_damage", "Explosion Block Damage", "Allow explosions to damage blocks", false),
    
    // Mob flags
    MOB_SPAWNING("mob_spawning", "Mob Spawning", "Allow hostile mob spawning", false),
    ANIMAL_SPAWNING("animal_spawning", "Animal Spawning", "Allow animal spawning", true),
    
    // Item flags
    ITEM_PICKUP("item_pickup", "Item Pickup", "Allow picking up items", true),
    ITEM_DROP("item_drop", "Item Drop", "Allow dropping items", true),
    
    // Redstone flags
    REDSTONE("redstone", "Redstone", "Allow redstone usage", true),
    
    // Container flags
    CONTAINER_ACCESS("container_access", "Container Access", "Allow accessing containers", false),
    
    // Vehicle flags
    VEHICLE_USE("vehicle_use", "Vehicle Use", "Allow using vehicles", false),
    VEHICLE_DESTROY("vehicle_destroy", "Vehicle Destroy", "Allow destroying vehicles", false),
    
    // Crop flags
    CROP_TRAMPLE("crop_trample", "Crop Trampling", "Allow trampling crops", false),
    CROP_HARVEST("crop_harvest", "Crop Harvesting", "Allow harvesting crops", false),
    
    // Door flags
    DOOR_INTERACT("door_interact", "Door Interaction", "Allow using doors", false),
    
    // Button/Lever flags
    BUTTON_INTERACT("button_interact", "Button Interaction", "Allow using buttons/levers", false),
    
    // Fly flag
    FLY("fly", "Flying", "Allow flying in territory", true),
    
    // Entry/Exit flags
    LAND_ENTER("land_enter", "Land Entry", "Allow entering the territory", true),
    LAND_EXIT("land_exit", "Land Exit", "Allow leaving the territory", true),
    
    // Teleportation flags
    TELEPORT_HERE("teleport_here", "Teleport Here", "Allow teleporting to this territory", true),
    TELEPORT_FROM("teleport_from", "Teleport From", "Allow teleporting from this territory", true);
    
    private final String key;
    private final String displayName;
    private final String description;
    private final boolean defaultValue;
    
    /**
     * Get protection flag by key
     * @param key Flag key
     * @return Optional containing flag if found
     */
    public static Optional<ProtectionFlag> fromKey(final String key) {
        return Arrays.stream(values())
                .filter(flag -> flag.key.equalsIgnoreCase(key))
                .findFirst();
    }
    
    /**
     * Check if this flag is a block-related flag
     * @return true if block-related
     */
    public boolean isBlockFlag() {
        return this.key.startsWith("block_");
    }
    
    /**
     * Check if this flag is an entity-related flag
     * @return true if entity-related
     */
    public boolean isEntityFlag() {
        return this.key.contains("entity") || this.key.contains("animal") || this.key.contains("monster");
    }
    
    /**
     * Check if this flag is an environmental flag
     * @return true if environmental
     */
    public boolean isEnvironmentalFlag() {
        return this.key.contains("fire") || this.key.contains("explosion") || this.key.contains("spawning");
    }
    
    /**
     * Check if this flag is a PvP-related flag
     * @return true if PvP-related
     */
    public boolean isPvpFlag() {
        return this.key.contains("pvp");
    }
}