package xyz.inv1s1bl3.countries.core.country;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a country in the plugin.
 * Contains all country data including citizens, government, and settings.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Country {
    
    @Expose
    @EqualsAndHashCode.Include
    private final String name;
    
    @Expose
    private final UUID leaderId;
    
    @Expose
    private final LocalDateTime foundedDate;
    
    @Expose
    private GovernmentType governmentType;
    
    @Expose
    private double balance;
    
    @Expose
    private double taxRate;
    
    @Expose
    private String description;
    
    @Expose
    private String flag;
    
    @Expose
    private final Map<UUID, CitizenRole> citizens;
    
    @Expose
    private final Map<CitizenRole, Double> salaries;
    
    @Expose
    private final Set<String> territories;
    
    @Expose
    private final Map<String, Object> settings;
    
    @Expose
    private boolean isActive;
    
    @Expose
    private LocalDateTime lastActivity;
    
    /**
     * Create a new country.
     * 
     * @param name the country name
     * @param leaderId the leader's UUID
     * @param governmentType the government type
     * @param startingBalance the starting balance
     */
    public Country(@NotNull final String name, 
                   @NotNull final UUID leaderId, 
                   @NotNull final GovernmentType governmentType,
                   final double startingBalance) {
        this.name = name;
        this.leaderId = leaderId;
        this.governmentType = governmentType;
        this.balance = startingBalance;
        this.foundedDate = LocalDateTime.now();
        this.taxRate = 0.05; // 5% default tax rate
        this.description = "A newly founded country";
        this.flag = "🏴";
        this.citizens = new HashMap<>();
        this.salaries = new EnumMap<>(CitizenRole.class);
        this.territories = new HashSet<>();
        this.settings = new HashMap<>();
        this.isActive = true;
        this.lastActivity = LocalDateTime.now();
        
        // Add leader as citizen
        this.citizens.put(leaderId, CitizenRole.LEADER);
        
        // Set default salaries
        this.initializeDefaultSalaries();
    }
    
    /**
     * Initialize default salary amounts for each role.
     */
    private void initializeDefaultSalaries() {
        this.salaries.put(CitizenRole.LEADER, 500.0);
        this.salaries.put(CitizenRole.MINISTER, 300.0);
        this.salaries.put(CitizenRole.OFFICER, 200.0);
        this.salaries.put(CitizenRole.CITIZEN, 100.0);
    }
    
    /**
     * Add a citizen to the country.
     * 
     * @param playerId the player's UUID
     * @param role the citizen role
     * @return true if added successfully
     */
    public boolean addCitizen(@NotNull final UUID playerId, @NotNull final CitizenRole role) {
        if (this.citizens.containsKey(playerId)) {
            return false;
        }
        
        this.citizens.put(playerId, role);
        this.updateActivity();
        return true;
    }
    
    /**
     * Remove a citizen from the country.
     * 
     * @param playerId the player's UUID
     * @return true if removed successfully
     */
    public boolean removeCitizen(@NotNull final UUID playerId) {
        if (!this.citizens.containsKey(playerId) || playerId.equals(this.leaderId)) {
            return false;
        }
        
        this.citizens.remove(playerId);
        this.updateActivity();
        return true;
    }
    
    /**
     * Get a citizen's role.
     * 
     * @param playerId the player's UUID
     * @return the citizen role, or null if not a citizen
     */
    @Nullable
    public CitizenRole getCitizenRole(@NotNull final UUID playerId) {
        return this.citizens.get(playerId);
    }
    
    /**
     * Check if a player is a citizen.
     * 
     * @param playerId the player's UUID
     * @return true if the player is a citizen
     */
    public boolean isCitizen(@NotNull final UUID playerId) {
        return this.citizens.containsKey(playerId);
    }
    
    /**
     * Promote a citizen to a higher role.
     * 
     * @param playerId the player's UUID
     * @param newRole the new role
     * @return true if promoted successfully
     */
    public boolean promoteCitizen(@NotNull final UUID playerId, @NotNull final CitizenRole newRole) {
        final CitizenRole currentRole = this.citizens.get(playerId);
        if (currentRole == null || currentRole == CitizenRole.LEADER) {
            return false;
        }
        
        this.citizens.put(playerId, newRole);
        this.updateActivity();
        return true;
    }
    
    /**
     * Demote a citizen to a lower role.
     * 
     * @param playerId the player's UUID
     * @param newRole the new role
     * @return true if demoted successfully
     */
    public boolean demoteCitizen(@NotNull final UUID playerId, @NotNull final CitizenRole newRole) {
        final CitizenRole currentRole = this.citizens.get(playerId);
        if (currentRole == null || currentRole == CitizenRole.LEADER || playerId.equals(this.leaderId)) {
            return false;
        }
        
        this.citizens.put(playerId, newRole);
        this.updateActivity();
        return true;
    }
    
    /**
     * Add a territory to the country.
     * 
     * @param territoryName the territory name
     * @return true if added successfully
     */
    public boolean addTerritory(@NotNull final String territoryName) {
        final boolean added = this.territories.add(territoryName);
        if (added) {
            this.updateActivity();
        }
        return added;
    }
    
    /**
     * Remove a territory from the country.
     * 
     * @param territoryName the territory name
     * @return true if removed successfully
     */
    public boolean removeTerritory(@NotNull final String territoryName) {
        final boolean removed = this.territories.remove(territoryName);
        if (removed) {
            this.updateActivity();
        }
        return removed;
    }
    
    /**
     * Deposit money into the country treasury.
     * 
     * @param amount the amount to deposit
     * @return true if deposited successfully
     */
    public boolean deposit(final double amount) {
        if (amount <= 0) {
            return false;
        }
        
        this.balance += amount;
        this.updateActivity();
        return true;
    }
    
    /**
     * Withdraw money from the country treasury.
     * 
     * @param amount the amount to withdraw
     * @return true if withdrawn successfully
     */
    public boolean withdraw(final double amount) {
        if (amount <= 0 || this.balance < amount) {
            return false;
        }
        
        this.balance -= amount;
        this.updateActivity();
        return true;
    }
    
    /**
     * Set the tax rate for the country.
     * 
     * @param taxRate the new tax rate (0.0 to 1.0)
     * @return true if set successfully
     */
    public boolean setTaxRate(final double taxRate) {
        if (taxRate < 0.0 || taxRate > 1.0) {
            return false;
        }
        
        this.taxRate = taxRate;
        this.updateActivity();
        return true;
    }
    
    /**
     * Set salary for a specific role.
     * 
     * @param role the citizen role
     * @param salary the salary amount
     */
    public void setSalary(@NotNull final CitizenRole role, final double salary) {
        this.salaries.put(role, Math.max(0.0, salary));
        this.updateActivity();
    }
    
    /**
     * Get salary for a specific role.
     * 
     * @param role the citizen role
     * @return the salary amount
     */
    public double getSalary(@NotNull final CitizenRole role) {
        return this.salaries.getOrDefault(role, 0.0);
    }
    
    /**
     * Get the total number of citizens.
     * 
     * @return the citizen count
     */
    public int getCitizenCount() {
        return this.citizens.size();
    }
    
    /**
     * Get the number of territories.
     * 
     * @return the territory count
     */
    public int getTerritoryCount() {
        return this.territories.size();
    }
    
    /**
     * Update the last activity timestamp.
     */
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }
    
    /**
     * Check if the country has been inactive for a certain number of days.
     * 
     * @param days the number of days
     * @return true if inactive for the specified days
     */
    public boolean isInactiveFor(final int days) {
        return this.lastActivity.isBefore(LocalDateTime.now().minusDays(days));
    }
    
    /**
     * Get a setting value.
     * 
     * @param key the setting key
     * @param defaultValue the default value
     * @param <T> the value type
     * @return the setting value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(@NotNull final String key, @NotNull final T defaultValue) {
        final Object value = this.settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (final ClassCastException exception) {
            return defaultValue;
        }
    }
    
    /**
     * Set a setting value.
     * 
     * @param key the setting key
     * @param value the setting value
     */
    public void setSetting(@NotNull final String key, @NotNull final Object value) {
        this.settings.put(key, value);
        this.updateActivity();
    }
}