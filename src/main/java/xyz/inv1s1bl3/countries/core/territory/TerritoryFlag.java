package xyz.inv1s1bl3.countries.core.territory;

/**
 * Represents different flags that can be set on territories to control behavior.
 */
public enum TerritoryFlag {
    // Player Action Flags
    BUILD("Build", "Allow building blocks", true),
    BREAK("Break", "Allow breaking blocks", true),
    INTERACT("Interact", "Allow interacting with blocks", true),
    CONTAINER_ACCESS("Container Access", "Allow opening containers", false),
    ITEM_PICKUP("Item Pickup", "Allow picking up items", true),
    ITEM_DROP("Item Drop", "Allow dropping items", true),
    
    // Combat Flags
    PVP("PvP", "Allow player vs player combat", false),
    DAMAGE_ANIMALS("Damage Animals", "Allow harming animals", true),
    DAMAGE_MONSTERS("Damage Monsters", "Allow harming monsters", true),
    
    // Environmental Flags
    MOB_SPAWNING("Mob Spawning", "Allow mob spawning", true),
    ANIMAL_SPAWNING("Animal Spawning", "Allow animal spawning", true),
    MONSTER_SPAWNING("Monster Spawning", "Allow monster spawning", true),
    FIRE_SPREAD("Fire Spread", "Allow fire to spread", false),
    EXPLOSION_DAMAGE("Explosion Damage", "Allow explosion damage", false),
    
    // Utility Flags
    TELEPORT("Teleport", "Allow teleportation", true),
    ENTER_MESSAGE("Enter Message", "Show enter messages", true),
    LEAVE_MESSAGE("Leave Message", "Show leave messages", true),
    FLY("Fly", "Allow flying", false),
    
    // Economic Flags
    SHOP_CREATE("Shop Create", "Allow creating shops", false),
    SHOP_USE("Shop Use", "Allow using shops", true);
    
    private final String displayName;
    private final String description;
    private final boolean defaultValue;
    
    TerritoryFlag(String displayName, String description, boolean defaultValue) {
        this.displayName = displayName;
        this.description = description;
        this.defaultValue = defaultValue;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * Get flag from string, case insensitive
     */
    public static TerritoryFlag fromString(String name) {
        for (TerritoryFlag flag : values()) {
            if (flag.name().equalsIgnoreCase(name) || 
                flag.displayName.equalsIgnoreCase(name)) {
                return flag;
            }
        }
        return BUILD; // Default
    }
}