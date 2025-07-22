package xyz.inv1s1bl3.countries.territory.protection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents territory permissions for a specific player or role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class TerritoryPermissions {
    
    private Integer territoryId;
    private UUID playerUuid;
    private TerritoryRole role;
    private Map<ProtectionFlag, Boolean> flagOverrides;
    
    /**
     * Initialize with default role permissions
     */
    public void initializeWithDefaults() {
        if (this.flagOverrides == null) {
            this.flagOverrides = new EnumMap<>(ProtectionFlag.class);
        }
        
        if (this.role != null) {
            // Set all flags to role defaults
            for (final ProtectionFlag flag : ProtectionFlag.values()) {
                if (!this.flagOverrides.containsKey(flag)) {
                    this.flagOverrides.put(flag, this.role.hasFlag(flag));
                }
            }
        }
    }
    
    /**
     * Check if player has a specific flag permission
     * @param flag Protection flag to check
     * @return true if player has permission
     */
    public boolean hasFlag(final ProtectionFlag flag) {
        // Check for specific override first
        if (this.flagOverrides != null && this.flagOverrides.containsKey(flag)) {
            return this.flagOverrides.get(flag);
        }
        
        // Fall back to role default
        if (this.role != null) {
            return this.role.hasFlag(flag);
        }
        
        // Default to false if no role
        return false;
    }
    
    /**
     * Set flag permission
     * @param flag Protection flag
     * @param value Permission value
     */
    public void setFlag(final ProtectionFlag flag, final boolean value) {
        if (this.flagOverrides == null) {
            this.flagOverrides = new EnumMap<>(ProtectionFlag.class);
        }
        this.flagOverrides.put(flag, value);
    }
    
    /**
     * Remove flag override (revert to role default)
     * @param flag Protection flag
     */
    public void removeFlag(final ProtectionFlag flag) {
        if (this.flagOverrides != null) {
            this.flagOverrides.remove(flag);
        }
    }
    
    /**
     * Check if flag is overridden from role default
     * @param flag Protection flag
     * @return true if overridden
     */
    public boolean isFlagOverridden(final ProtectionFlag flag) {
        return this.flagOverrides != null && this.flagOverrides.containsKey(flag);
    }
    
    /**
     * Get effective role (considering overrides)
     * @return Effective role level
     */
    public TerritoryRole getEffectiveRole() {
        return this.role != null ? this.role : TerritoryRole.VISITOR;
    }
    
    /**
     * Check if permissions allow management actions
     * @return true if can manage territory
     */
    public boolean canManageTerritory() {
        return this.role == TerritoryRole.OWNER;
    }
    
    /**
     * Check if permissions allow inviting others
     * @return true if can invite
     */
    public boolean canInviteOthers() {
        return this.role == TerritoryRole.OWNER || this.role == TerritoryRole.MEMBER;
    }
    
    /**
     * Check if can modify role of another player
     * @param targetRole Target player's role
     * @return true if can modify
     */
    public boolean canModifyRole(final TerritoryRole targetRole) {
        return this.role != null && this.role.canManage(targetRole);
    }
}