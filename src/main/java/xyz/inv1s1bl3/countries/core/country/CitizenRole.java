package xyz.inv1s1bl3.countries.core.country;

/**
 * Represents different roles a citizen can have within a country.
 */
public enum CitizenRole {
    OWNER("Owner", 100, true, true, true, true, true),
    ADMIN("Admin", 90, true, true, true, true, false),
    MINISTER("Minister", 80, true, true, true, false, false),
    OFFICER("Officer", 70, true, true, false, false, false),
    CITIZEN("Citizen", 50, true, false, false, false, false),
    RESIDENT("Resident", 30, false, false, false, false, false),
    GUEST("Guest", 10, false, false, false, false, false);
    
    private final String displayName;
    private final int priority;
    private final boolean canBuild;
    private final boolean canInvite;
    private final boolean canKick;
    private final boolean canManageEconomy;
    private final boolean canDisband;
    
    CitizenRole(String displayName, int priority, boolean canBuild, 
                boolean canInvite, boolean canKick, boolean canManageEconomy, 
                boolean canDisband) {
        this.displayName = displayName;
        this.priority = priority;
        this.canBuild = canBuild;
        this.canInvite = canInvite;
        this.canKick = canKick;
        this.canManageEconomy = canManageEconomy;
        this.canDisband = canDisband;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean canBuild() {
        return canBuild;
    }
    
    public boolean canInvite() {
        return canInvite;
    }
    
    public boolean canKick() {
        return canKick;
    }
    
    public boolean canManageEconomy() {
        return canManageEconomy;
    }
    
    public boolean canDisband() {
        return canDisband;
    }
    
    /**
     * Check if this role can promote/demote another role
     */
    public boolean canManageRole(CitizenRole otherRole) {
        return this.priority > otherRole.priority;
    }
    
    /**
     * Get role from string, case insensitive
     */
    public static CitizenRole fromString(String name) {
        for (CitizenRole role : values()) {
            if (role.name().equalsIgnoreCase(name) || 
                role.displayName.equalsIgnoreCase(name)) {
                return role;
            }
        }
        return CITIZEN; // Default
    }
    
    /**
     * Get the next higher role (for promotions)
     */
    public CitizenRole getHigherRole() {
        CitizenRole[] roles = values();
        for (int i = 0; i < roles.length - 1; i++) {
            if (roles[i] == this) {
                return roles[i + 1];
            }
        }
        return this; // Already highest
    }
    
    /**
     * Get the next lower role (for demotions)
     */
    public CitizenRole getLowerRole() {
        CitizenRole[] roles = values();
        for (int i = 1; i < roles.length; i++) {
            if (roles[i] == this) {
                return roles[i - 1];
            }
        }
        return this; // Already lowest
    }
}