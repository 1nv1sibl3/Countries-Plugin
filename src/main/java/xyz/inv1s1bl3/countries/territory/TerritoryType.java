package xyz.inv1s1bl3.countries.territory;

import xyz.inv1s1bl3.countries.country.GovernmentType;
import org.bukkit.ChatColor;
import java.util.Set;
import java.util.EnumSet;
import java.util.Optional;

/**
 * Represents different types of territories with their characteristics and permissions.
 */
public enum TerritoryType {
    WILDERNESS("Wilderness", "Unclaimed wild territory", ChatColor.GREEN, 
               EnumSet.allOf(GovernmentType.class), true, true, true, true, true, 
               "wilderness", 0.0, 0.0, true, true),
    
    CITY("City", "Urban residential and commercial area", ChatColor.BLUE, 
         EnumSet.of(GovernmentType.DEMOCRACY, GovernmentType.REPUBLIC, GovernmentType.MONARCHY), 
         false, true, false, false, false,
         "city", 800.0, 1.8, false, true),
    
    INDUSTRIAL("Industrial", "Manufacturing and production zone", ChatColor.GRAY, 
               EnumSet.of(GovernmentType.DEMOCRACY, GovernmentType.REPUBLIC), 
               true, true, false, false, false,
               "industrial", 1200.0, 2.2, false, false),
    
    RESIDENTIAL("Residential", "Housing and living areas", ChatColor.LIGHT_PURPLE, 
                EnumSet.of(GovernmentType.DEMOCRACY, GovernmentType.REPUBLIC, GovernmentType.MONARCHY), 
                false, true, false, false, false,
                "residential", 100.0, 0.8, true, true),
    
    COMMERCIAL("Commercial", "Business and trade district", ChatColor.GOLD, 
               EnumSet.of(GovernmentType.DEMOCRACY, GovernmentType.REPUBLIC), 
               false, true, false, false, false,
               "commercial", 500.0, 1.5, false, true),
    
    MILITARY("Military", "Restricted military installation", ChatColor.RED, 
             EnumSet.of(GovernmentType.MONARCHY), 
             false, false, false, false, false,
             "military", 2000.0, 3.5, false, false),
    
    AGRICULTURAL("Agricultural", "Farming and food production", ChatColor.YELLOW, 
                 EnumSet.allOf(GovernmentType.class), true, true, false, false, false,
                 "agricultural", 150.0, 0.6, true, true),
    
    PROTECTED("Protected", "Environmentally protected area", ChatColor.DARK_GREEN, 
              EnumSet.of(GovernmentType.DEMOCRACY, GovernmentType.REPUBLIC), 
              false, false, false, false, false,
              "protected", 300.0, 0.4, true, true),
    
    BORDER("Border", "Territory boundary zone", ChatColor.DARK_RED, 
           EnumSet.of(GovernmentType.MONARCHY), 
           false, false, true, true, false,
           "border", 1500.0, 2.8, false, false),
    
    CAPITAL("Capital", "Capital city territory", ChatColor.GOLD, 
            EnumSet.allOf(GovernmentType.class), 
            false, true, false, false, false,
            "capital", 5000.0, 4.0, true, true);

    private final String displayName;
    private final String description;
    private final ChatColor color;
    private final Set<GovernmentType> allowedGovernmentTypes;
    private final boolean canBreak;
    private final boolean canBuild;
    private final boolean allowPvp;
    private final boolean allowExplosions;
    private final boolean allowFireSpread;
    private final String key;
    private final double creationCost;
    private final double maintenanceMultiplier;
    private final boolean allowsMobSpawning;
    private final boolean allowsAnimalSpawning;

    /**
     * Constructor for TerritoryType enum.
     *
     * @param displayName The display name of the territory type
     * @param description A description of the territory type
     * @param color The color associated with this territory type
     * @param allowedGovernmentTypes Set of government types allowed in this territory
     * @param canBreak Whether block breaking is allowed
     * @param canBuild Whether block building is allowed
     * @param allowPvp Whether PvP is allowed
     * @param allowExplosions Whether explosions are allowed
     * @param allowFireSpread Whether fire spread is allowed
     * @param key The unique key identifier for this territory type
     * @param creationCost The cost to create this territory type
     * @param maintenanceMultiplier The maintenance cost multiplier
     * @param allowsMobSpawning Whether mob spawning is allowed
     * @param allowsAnimalSpawning Whether animal spawning is allowed
     */
    TerritoryType(String displayName, String description, ChatColor color, 
                  Set<GovernmentType> allowedGovernmentTypes, boolean canBreak, 
                  boolean canBuild, boolean allowPvp, boolean allowExplosions, 
                  boolean allowFireSpread, String key, double creationCost, 
                  double maintenanceMultiplier, boolean allowsMobSpawning, 
                  boolean allowsAnimalSpawning) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.allowedGovernmentTypes = allowedGovernmentTypes;
        this.canBreak = canBreak;
        this.canBuild = canBuild;
        this.allowPvp = allowPvp;
        this.allowExplosions = allowExplosions;
        this.allowFireSpread = allowFireSpread;
        this.key = key;
        this.creationCost = creationCost;
        this.maintenanceMultiplier = maintenanceMultiplier;
        this.allowsMobSpawning = allowsMobSpawning;
        this.allowsAnimalSpawning = allowsAnimalSpawning;
    }

    /**
     * Gets the display name of this territory type.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the description of this territory type.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the color associated with this territory type.
     *
     * @return The ChatColor
     */
    public ChatColor getColor() {
        return color;
    }

    /**
     * Gets the set of government types allowed in this territory.
     *
     * @return Set of allowed government types
     */
    public Set<GovernmentType> getAllowedGovernmentTypes() {
        return allowedGovernmentTypes;
    }

    /**
     * Checks if the given government type is allowed in this territory.
     *
     * @param governmentType The government type to check
     * @return true if allowed, false otherwise
     */
    public boolean isGovernmentTypeAllowed(GovernmentType governmentType) {
        return allowedGovernmentTypes.contains(governmentType);
    }

    /**
     * Checks if block breaking is allowed in this territory type.
     *
     * @return true if block breaking is allowed, false otherwise
     */
    public boolean canBreak() {
        return canBreak;
    }

    /**
     * Checks if block building is allowed in this territory type.
     *
     * @return true if block building is allowed, false otherwise
     */
    public boolean canBuild() {
        return canBuild;
    }

    /**
     * Checks if PvP is allowed in this territory type.
     *
     * @return true if PvP is allowed, false otherwise
     */
    public boolean allowPvp() {
        return allowPvp;
    }

    /**
     * Checks if explosions are allowed in this territory type.
     *
     * @return true if explosions are allowed, false otherwise
     */
    public boolean allowExplosions() {
        return allowExplosions;
    }

    /**
     * Checks if fire spread is allowed in this territory type.
     *
     * @return true if fire spread is allowed, false otherwise
     */
    public boolean allowFireSpread() {
        return allowFireSpread;
    }

    /**
     * Gets the unique key identifier for this territory type.
     *
     * @return The key identifier
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the creation cost for this territory type.
     *
     * @return The creation cost
     */
    public double getCreationCost() {
        return creationCost;
    }

    /**
     * Gets the maintenance cost multiplier for this territory type.
     *
     * @return The maintenance multiplier
     */
    public double getMaintenanceMultiplier() {
        return maintenanceMultiplier;
    }

    /**
     * Checks if mob spawning is allowed in this territory type.
     *
     * @return true if mob spawning is allowed, false otherwise
     */
    public boolean allowsMobSpawning() {
        return allowsMobSpawning;
    }

    /**
     * Checks if animal spawning is allowed in this territory type.
     *
     * @return true if animal spawning is allowed, false otherwise
     */
    public boolean allowsAnimalSpawning() {
        return allowsAnimalSpawning;
    }

    /**
     * Gets a territory type by its display name.
     *
     * @param displayName The display name to search for
     * @return The matching TerritoryType or null if not found
     */
    public static TerritoryType getByDisplayName(String displayName) {
        for (TerritoryType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Gets a territory type by its key identifier.
     *
     * @param key The key to search for
     * @return Optional containing the matching TerritoryType, or empty if not found
     */
    public static Optional<TerritoryType> fromKey(String key) {
        for (TerritoryType type : values()) {
            if (type.getKey().equalsIgnoreCase(key)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets the formatted display name with color.
     *
     * @return Colored display name
     */
    public String getFormattedName() {
        return color + displayName + ChatColor.RESET;
    }

    @Override
    public String toString() {
        return displayName;
    }
}