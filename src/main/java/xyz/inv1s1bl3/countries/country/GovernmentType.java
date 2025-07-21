package xyz.inv1s1bl3.countries.country;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Enumeration of government types with their characteristics
 */
@Getter
@RequiredArgsConstructor
public enum GovernmentType {
    
    MONARCHY("monarchy", "Monarchy", Arrays.asList("monarch", "noble", "knight", "citizen", "peasant")),
    DEMOCRACY("democracy", "Democracy", Arrays.asList("president", "minister", "representative", "citizen")),
    REPUBLIC("republic", "Republic", Arrays.asList("consul", "senator", "magistrate", "citizen")),
    FEDERATION("federation", "Federation", Arrays.asList("chancellor", "governor", "delegate", "citizen")),
    OLIGARCHY("oligarchy", "Oligarchy", Arrays.asList("oligarch", "elite", "enforcer", "subject")),
    THEOCRACY("theocracy", "Theocracy", Arrays.asList("high_priest", "priest", "acolyte", "believer", "heretic")),
    AUTOCRACY("autocracy", "Autocracy", Arrays.asList("autocrat", "lieutenant", "loyalist", "subject")),
    ANARCHY("anarchy", "Anarchy", Arrays.asList("leader", "member"));
    
    private final String key;
    private final String displayName;
    private final List<String> roles;
    
    /**
     * Get government type by key
     * @param key Government type key
     * @return Optional containing government type if found
     */
    public static Optional<GovernmentType> fromKey(final String key) {
        return Arrays.stream(values())
                .filter(type -> type.key.equalsIgnoreCase(key))
                .findFirst();
    }
    
    /**
     * Get the leader role for this government type
     * @return Leader role name
     */
    public String getLeaderRole() {
        return this.roles.get(0);
    }
    
    /**
     * Get the default citizen role for this government type
     * @return Default citizen role name
     */
    public String getDefaultRole() {
        return this.roles.get(this.roles.size() - 1);
    }
    
    /**
     * Check if a role exists in this government type
     * @param role Role to check
     * @return true if role exists
     */
    public boolean hasRole(final String role) {
        return this.roles.contains(role.toLowerCase());
    }
    
    /**
     * Get role hierarchy level (0 = highest authority)
     * @param role Role to check
     * @return Role level, or -1 if role doesn't exist
     */
    public int getRoleLevel(final String role) {
        return this.roles.indexOf(role.toLowerCase());
    }
    
    /**
     * Check if first role has higher authority than second role
     * @param role1 First role
     * @param role2 Second role
     * @return true if role1 has higher authority
     */
    public boolean hasHigherAuthority(final String role1, final String role2) {
        final int level1 = this.getRoleLevel(role1);
        final int level2 = this.getRoleLevel(role2);
        
        if (level1 == -1 || level2 == -1) {
            return false;
        }
        
        return level1 < level2; // Lower index = higher authority
    }
    
    /**
     * Check if role has administrative permissions
     * @param role Role to check
     * @return true if role has admin permissions
     */
    public boolean hasAdminPermissions(final String role) {
        final int level = this.getRoleLevel(role);
        return level >= 0 && level <= 2; // Top 3 roles typically have admin permissions
    }
    
    /**
     * Check if government supports elections
     * @return true if elections are supported
     */
    public boolean supportsElections() {
        return this == DEMOCRACY || this == REPUBLIC || this == FEDERATION;
    }
    
    /**
     * Check if government has term limits
     * @return true if term limits apply
     */
    public boolean hasTermLimits() {
        return this.supportsElections();
    }
    
    /**
     * Check if government can be overthrown
     * @return true if overthrow is possible
     */
    public boolean canBeOverthrown() {
        return this != ANARCHY; // Anarchy can't be overthrown as it has no formal structure
    }
    
    /**
     * Get the succession type for this government
     * @return Succession type
     */
    public String getSuccessionType() {
        return switch (this) {
            case MONARCHY, AUTOCRACY -> "hereditary";
            case DEMOCRACY, REPUBLIC, FEDERATION -> "elective";
            case OLIGARCHY -> "appointive";
            case THEOCRACY -> "divine_appointment";
            case ANARCHY -> "consensus";
        };
    }
}