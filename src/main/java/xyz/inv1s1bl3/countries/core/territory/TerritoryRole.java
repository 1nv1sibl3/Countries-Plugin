package xyz.inv1s1bl3.countries.core.territory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Represents different roles that players can have in territories.
 */
public enum TerritoryRole {
    OWNER("Owner", 100),
    MANAGER("Manager", 90),
    MEMBER("Member", 70),
    TENANT("Tenant", 50),
    ALLY("Ally", 30),
    VISITOR("Visitor", 10);
    
    private final String displayName;
    private final int priority;
    private final Map<TerritoryFlag, Boolean> defaultFlags;
    
    TerritoryRole(String displayName, int priority) {
        this.displayName = displayName;
        this.priority = priority;
        this.defaultFlags = new EnumMap<>(TerritoryFlag.class);
        initializeDefaultFlags();
    }
    
    private void initializeDefaultFlags() {
        switch (this) {
            case OWNER -> {
                // Owners can do everything
                for (TerritoryFlag flag : TerritoryFlag.values()) {
                    defaultFlags.put(flag, true);
                }
            }
            case MANAGER -> {
                // Managers can do most things except some admin functions
                for (TerritoryFlag flag : TerritoryFlag.values()) {
                    defaultFlags.put(flag, true);
                }
                defaultFlags.put(TerritoryFlag.SHOP_CREATE, false);
            }
            case MEMBER -> {
                // Members have basic permissions
                defaultFlags.put(TerritoryFlag.BUILD, true);
                defaultFlags.put(TerritoryFlag.BREAK, true);
                defaultFlags.put(TerritoryFlag.INTERACT, true);
                defaultFlags.put(TerritoryFlag.CONTAINER_ACCESS, true);
                defaultFlags.put(TerritoryFlag.ITEM_PICKUP, true);
                defaultFlags.put(TerritoryFlag.ITEM_DROP, true);
                defaultFlags.put(TerritoryFlag.DAMAGE_ANIMALS, true);
                defaultFlags.put(TerritoryFlag.DAMAGE_MONSTERS, true);
                defaultFlags.put(TerritoryFlag.TELEPORT, true);
                defaultFlags.put(TerritoryFlag.SHOP_USE, true);
            }
            case TENANT -> {
                // Tenants have limited permissions
                defaultFlags.put(TerritoryFlag.BUILD, true);
                defaultFlags.put(TerritoryFlag.BREAK, true);
                defaultFlags.put(TerritoryFlag.INTERACT, true);
                defaultFlags.put(TerritoryFlag.CONTAINER_ACCESS, false);
                defaultFlags.put(TerritoryFlag.ITEM_PICKUP, true);
                defaultFlags.put(TerritoryFlag.ITEM_DROP, true);
                defaultFlags.put(TerritoryFlag.TELEPORT, true);
                defaultFlags.put(TerritoryFlag.SHOP_USE, true);
            }
            case ALLY -> {
                // Allies have visitor permissions plus some extras
                defaultFlags.put(TerritoryFlag.INTERACT, true);
                defaultFlags.put(TerritoryFlag.ITEM_PICKUP, true);
                defaultFlags.put(TerritoryFlag.ITEM_DROP, true);
                defaultFlags.put(TerritoryFlag.TELEPORT, true);
                defaultFlags.put(TerritoryFlag.SHOP_USE, true);
            }
            case VISITOR -> {
                // Visitors have minimal permissions
                defaultFlags.put(TerritoryFlag.INTERACT, false);
                defaultFlags.put(TerritoryFlag.ITEM_PICKUP, false);
                defaultFlags.put(TerritoryFlag.TELEPORT, true);
                defaultFlags.put(TerritoryFlag.SHOP_USE, true);
            }
        }
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public Map<TerritoryFlag, Boolean> getDefaultFlags() {
        return new EnumMap<>(defaultFlags);
    }
    
    public boolean hasFlag(TerritoryFlag flag) {
        return defaultFlags.getOrDefault(flag, false);
    }
    
    /**
     * Check if this role can manage another role
     */
    public boolean canManage(TerritoryRole other) {
        return this.priority > other.priority;
    }
    
    /**
     * Get role from string, case insensitive
     */
    public static TerritoryRole fromString(String name) {
        for (TerritoryRole role : values()) {
            if (role.name().equalsIgnoreCase(name) || 
                role.displayName.equalsIgnoreCase(name)) {
                return role;
            }
        }
        return VISITOR; // Default
    }
}