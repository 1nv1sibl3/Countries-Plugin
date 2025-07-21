package xyz.inv1s1bl3.countries.core.country;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the different roles a citizen can have in a country.
 * Each role has different permissions and responsibilities.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Getter
public enum CitizenRole {
    
    LEADER("Leader", 4, "&c&l"),
    MINISTER("Minister", 3, "&6&l"),
    OFFICER("Officer", 2, "&e"),
    CITIZEN("Citizen", 1, "&f");
    
    private final String displayName;
    private final int priority;
    private final String colorCode;
    
    CitizenRole(@NotNull final String displayName, final int priority, @NotNull final String colorCode) {
        this.displayName = displayName;
        this.priority = priority;
        this.colorCode = colorCode;
    }
    
    /**
     * Get the formatted display name with color.
     * 
     * @return the formatted display name
     */
    @NotNull
    public String getFormattedName() {
        return this.colorCode + this.displayName;
    }
    
    /**
     * Check if this role has higher priority than another role.
     * 
     * @param other the other role
     * @return true if this role has higher priority
     */
    public boolean isHigherThan(@NotNull final CitizenRole other) {
        return this.priority > other.priority;
    }
    
    /**
     * Check if this role has lower priority than another role.
     * 
     * @param other the other role
     * @return true if this role has lower priority
     */
    public boolean isLowerThan(@NotNull final CitizenRole other) {
        return this.priority < other.priority;
    }
    
    /**
     * Check if this role can manage another role.
     * Leaders can manage all roles, Ministers can manage Officers and Citizens, etc.
     * 
     * @param other the other role
     * @return true if this role can manage the other role
     */
    public boolean canManage(@NotNull final CitizenRole other) {
        return this.priority > other.priority;
    }
    
    /**
     * Get a role by its display name.
     * 
     * @param name the display name
     * @return the role, or null if not found
     */
    @NotNull
    public static CitizenRole fromString(@NotNull final String name) {
        for (final CitizenRole role : values()) {
            if (role.displayName.equalsIgnoreCase(name) || role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        return CITIZEN; // Default to citizen if not found
    }
    
    /**
     * Get the next higher role.
     * 
     * @return the next higher role, or this role if already highest
     */
    @NotNull
    public CitizenRole getNextHigher() {
        switch (this) {
            case CITIZEN:
                return OFFICER;
            case OFFICER:
                return MINISTER;
            case MINISTER:
                return LEADER;
            case LEADER:
            default:
                return this;
        }
    }
    
    /**
     * Get the next lower role.
     * 
     * @return the next lower role, or this role if already lowest
     */
    @NotNull
    public CitizenRole getNextLower() {
        switch (this) {
            case LEADER:
                return MINISTER;
            case MINISTER:
                return OFFICER;
            case OFFICER:
                return CITIZEN;
            case CITIZEN:
            default:
                return this;
        }
    }
}