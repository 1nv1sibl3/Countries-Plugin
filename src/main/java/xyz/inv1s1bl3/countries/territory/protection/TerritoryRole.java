package xyz.inv1s1bl3.countries.territory.protection;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Enumeration of territory roles with their default permissions
 */
@Getter
@RequiredArgsConstructor
public enum TerritoryRole {
    
    OWNER("owner", "Owner", "Full control over the territory", EnumSet.allOf(ProtectionFlag.class)),
    
    MEMBER("member", "Member", "Trusted member with most permissions", EnumSet.of(
        ProtectionFlag.BLOCK_BREAK, ProtectionFlag.BLOCK_PLACE, ProtectionFlag.BLOCK_INTERACT,
        ProtectionFlag.ENTITY_INTERACT, ProtectionFlag.CONTAINER_ACCESS, ProtectionFlag.DOOR_INTERACT,
        ProtectionFlag.BUTTON_INTERACT, ProtectionFlag.CROP_HARVEST, ProtectionFlag.ITEM_PICKUP,
        ProtectionFlag.ITEM_DROP, ProtectionFlag.REDSTONE, ProtectionFlag.VEHICLE_USE,
        ProtectionFlag.FLY, ProtectionFlag.LAND_ENTER, ProtectionFlag.LAND_EXIT,
        ProtectionFlag.TELEPORT_HERE, ProtectionFlag.TELEPORT_FROM
    )),
    
    ALLY("ally", "Ally", "Allied country member with limited permissions", EnumSet.of(
        ProtectionFlag.BLOCK_INTERACT, ProtectionFlag.ENTITY_INTERACT, ProtectionFlag.DOOR_INTERACT,
        ProtectionFlag.BUTTON_INTERACT, ProtectionFlag.ITEM_PICKUP, ProtectionFlag.ITEM_DROP,
        ProtectionFlag.FLY, ProtectionFlag.LAND_ENTER, ProtectionFlag.LAND_EXIT,
        ProtectionFlag.TELEPORT_HERE, ProtectionFlag.TELEPORT_FROM
    )),
    
    TENANT("tenant", "Tenant", "Renter with specific permissions", EnumSet.of(
        ProtectionFlag.BLOCK_BREAK, ProtectionFlag.BLOCK_PLACE, ProtectionFlag.BLOCK_INTERACT,
        ProtectionFlag.ENTITY_INTERACT, ProtectionFlag.CONTAINER_ACCESS, ProtectionFlag.DOOR_INTERACT,
        ProtectionFlag.ITEM_PICKUP, ProtectionFlag.ITEM_DROP, ProtectionFlag.FLY,
        ProtectionFlag.LAND_ENTER, ProtectionFlag.LAND_EXIT
    )),
    
    VISITOR("visitor", "Visitor", "Basic visitor permissions", EnumSet.of(
        ProtectionFlag.LAND_ENTER, ProtectionFlag.LAND_EXIT, ProtectionFlag.ITEM_PICKUP
    )),
    
    BANNED("banned", "Banned", "No permissions - banned from territory", EnumSet.noneOf(ProtectionFlag.class));
    
    private final String key;
    private final String displayName;
    private final String description;
    private final Set<ProtectionFlag> defaultFlags;
    
    /**
     * Get territory role by key
     * @param key Role key
     * @return Optional containing role if found
     */
    public static Optional<TerritoryRole> fromKey(final String key) {
        return Arrays.stream(values())
                .filter(role -> role.key.equalsIgnoreCase(key))
                .findFirst();
    }
    
    /**
     * Check if role has a specific flag by default
     * @param flag Protection flag
     * @return true if role has the flag
     */
    public boolean hasFlag(final ProtectionFlag flag) {
        return this.defaultFlags.contains(flag);
    }
    
    /**
     * Check if role has higher authority than another role
     * @param other Other role
     * @return true if this role has higher authority
     */
    public boolean hasHigherAuthorityThan(final TerritoryRole other) {
        return this.ordinal() < other.ordinal(); // Lower ordinal = higher authority
    }
    
    /**
     * Check if role can manage other role
     * @param other Other role
     * @return true if can manage
     */
    public boolean canManage(final TerritoryRole other) {
        return this.hasHigherAuthorityThan(other);
    }
}