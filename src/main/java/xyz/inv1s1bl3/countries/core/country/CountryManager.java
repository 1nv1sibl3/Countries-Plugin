package xyz.inv1s1bl3.countries.core.country;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages all countries in the plugin.
 * Handles country creation, deletion, and citizen management.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class CountryManager {
    
    private final CountriesPlugin plugin;
    
    @Getter
    private final Map<String, Country> countries;
    private final Map<UUID, String> playerCountries;
    
    public CountryManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.countries = new ConcurrentHashMap<>();
        this.playerCountries = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new country.
     * 
     * @param name the country name
     * @param leaderId the leader's UUID
     * @param governmentType the government type
     * @return the created country, or null if creation failed
     */
    @Nullable
    public Country createCountry(@NotNull final String name, 
                                @NotNull final UUID leaderId, 
                                @NotNull final GovernmentType governmentType) {
        // Validate country name
        if (!this.isValidCountryName(name)) {
            return null;
        }
        
        // Check if country already exists
        if (this.countries.containsKey(name.toLowerCase())) {
            return null;
        }
        
        // Check if player is already in a country
        if (this.playerCountries.containsKey(leaderId)) {
            return null;
        }
        
        // Get starting balance from config
        final double startingBalance = this.plugin.getConfigManager()
            .getConfigValue("country.starting-balance", 10000.0);
        
        // Create the country
        final Country country = new Country(name, leaderId, governmentType, startingBalance);
        
        // Add to maps
        this.countries.put(name.toLowerCase(), country);
        this.playerCountries.put(leaderId, name.toLowerCase());
        
        // Save data
        this.plugin.getDataManager().saveCountryData();
        
        return country;
    }
    
    /**
     * Delete a country.
     * 
     * @param name the country name
     * @return true if deleted successfully
     */
    public boolean deleteCountry(@NotNull final String name) {
        final Country country = this.countries.get(name.toLowerCase());
        if (country == null) {
            return false;
        }
        
        // Remove all citizens from player mapping
        for (final UUID citizenId : country.getCitizens().keySet()) {
            this.playerCountries.remove(citizenId);
        }
        
        // Remove country
        this.countries.remove(name.toLowerCase());
        
        // Save data
        this.plugin.getDataManager().saveCountryData();
        
        return true;
    }
    
    /**
     * Get a country by name.
     * 
     * @param name the country name
     * @return the country, or null if not found
     */
    @Nullable
    public Country getCountry(@NotNull final String name) {
        return this.countries.get(name.toLowerCase());
    }
    
    /**
     * Get a country by player UUID.
     * 
     * @param playerId the player's UUID
     * @return the country, or null if player is not in a country
     */
    @Nullable
    public Country getPlayerCountry(@NotNull final UUID playerId) {
        final String countryName = this.playerCountries.get(playerId);
        if (countryName == null) {
            return null;
        }
        return this.countries.get(countryName);
    }
    
    /**
     * Add a player to a country.
     * 
     * @param playerId the player's UUID
     * @param countryName the country name
     * @param role the citizen role
     * @return true if added successfully
     */
    public boolean addPlayerToCountry(@NotNull final UUID playerId, 
                                     @NotNull final String countryName, 
                                     @NotNull final CitizenRole role) {
        final Country country = this.countries.get(countryName.toLowerCase());
        if (country == null) {
            return false;
        }
        
        // Check if player is already in a country
        if (this.playerCountries.containsKey(playerId)) {
            return false;
        }
        
        // Check citizen limit
        final int maxCitizens = this.plugin.getConfigManager()
            .getConfigValue("country.max-citizens", 50);
        if (country.getCitizenCount() >= maxCitizens) {
            return false;
        }
        
        // Add to country and mapping
        if (country.addCitizen(playerId, role)) {
            this.playerCountries.put(playerId, countryName.toLowerCase());
            this.plugin.getDataManager().saveCountryData();
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a player from their country.
     * 
     * @param playerId the player's UUID
     * @return true if removed successfully
     */
    public boolean removePlayerFromCountry(@NotNull final UUID playerId) {
        final String countryName = this.playerCountries.get(playerId);
        if (countryName == null) {
            return false;
        }
        
        final Country country = this.countries.get(countryName);
        if (country == null) {
            return false;
        }
        
        // Cannot remove leader
        if (playerId.equals(country.getLeaderId())) {
            return false;
        }
        
        // Remove from country and mapping
        if (country.removeCitizen(playerId)) {
            this.playerCountries.remove(playerId);
            this.plugin.getDataManager().saveCountryData();
            return true;
        }
        
        return false;
    }
    
    /**
     * Transfer leadership of a country.
     * 
     * @param currentLeaderId the current leader's UUID
     * @param newLeaderId the new leader's UUID
     * @return true if transferred successfully
     */
    public boolean transferLeadership(@NotNull final UUID currentLeaderId, @NotNull final UUID newLeaderId) {
        final Country country = this.getPlayerCountry(currentLeaderId);
        if (country == null || !currentLeaderId.equals(country.getLeaderId())) {
            return false;
        }
        
        // Check if new leader is a citizen
        if (!country.isCitizen(newLeaderId)) {
            return false;
        }
        
        // Transfer leadership
        country.getCitizens().put(currentLeaderId, CitizenRole.MINISTER);
        country.getCitizens().put(newLeaderId, CitizenRole.LEADER);
        
        this.plugin.getDataManager().saveCountryData();
        return true;
    }
    
    /**
     * Get all countries.
     * 
     * @return a collection of all countries
     */
    @NotNull
    public Collection<Country> getAllCountries() {
        return this.countries.values();
    }
    
    /**
     * Get countries sorted by citizen count.
     * 
     * @return a list of countries sorted by citizen count (descending)
     */
    @NotNull
    public List<Country> getCountriesByCitizenCount() {
        return this.countries.values().stream()
            .sorted((c1, c2) -> Integer.compare(c2.getCitizenCount(), c1.getCitizenCount()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get countries sorted by balance.
     * 
     * @return a list of countries sorted by balance (descending)
     */
    @NotNull
    public List<Country> getCountriesByBalance() {
        return this.countries.values().stream()
            .sorted((c1, c2) -> Double.compare(c2.getBalance(), c1.getBalance()))
            .collect(Collectors.toList());
    }
    
    /**
     * Search for countries by name.
     * 
     * @param query the search query
     * @return a list of matching countries
     */
    @NotNull
    public List<Country> searchCountries(@NotNull final String query) {
        final String lowerQuery = query.toLowerCase();
        return this.countries.values().stream()
            .filter(country -> country.getName().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a country name is valid.
     * 
     * @param name the country name
     * @return true if valid
     */
    public boolean isValidCountryName(@NotNull final String name) {
        final int minLength = this.plugin.getConfigManager()
            .getConfigValue("country.name-length-min", 3);
        final int maxLength = this.plugin.getConfigManager()
            .getConfigValue("country.name-length-max", 20);
        
        if (name.length() < minLength || name.length() > maxLength) {
            return false;
        }
        
        // Check for valid characters (letters, numbers, spaces, hyphens, underscores)
        return name.matches("^[a-zA-Z0-9 _-]+$");
    }
    
    /**
     * Get inactive countries.
     * 
     * @param days the number of days to consider inactive
     * @return a list of inactive countries
     */
    @NotNull
    public List<Country> getInactiveCountries(final int days) {
        return this.countries.values().stream()
            .filter(country -> country.isInactiveFor(days))
            .collect(Collectors.toList());
    }
    
    /**
     * Clean up inactive countries.
     * 
     * @return the number of countries deleted
     */
    public int cleanupInactiveCountries() {
        final int inactivityDays = this.plugin.getConfigManager()
            .getConfigValue("general.country-inactivity-deletion", 30);
        
        if (inactivityDays <= 0) {
            return 0;
        }
        
        final List<Country> inactiveCountries = this.getInactiveCountries(inactivityDays);
        int deletedCount = 0;
        
        for (final Country country : inactiveCountries) {
            if (this.deleteCountry(country.getName())) {
                deletedCount++;
            }
        }
        
        return deletedCount;
    }
    
    /**
     * Get country statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_countries", this.countries.size());
        stats.put("total_citizens", this.playerCountries.size());
        stats.put("average_citizens_per_country", 
            this.countries.isEmpty() ? 0.0 : (double) this.playerCountries.size() / this.countries.size());
        
        final OptionalDouble avgBalance = this.countries.values().stream()
            .mapToDouble(Country::getBalance)
            .average();
        stats.put("average_country_balance", avgBalance.orElse(0.0));
        
        final Map<GovernmentType, Long> governmentCounts = this.countries.values().stream()
            .collect(Collectors.groupingBy(Country::getGovernmentType, Collectors.counting()));
        stats.put("government_distribution", governmentCounts);
        
        return stats;
    }
    
    /**
     * Load country data from storage.
     * 
     * @param countryData the country data map
     */
    public void loadCountryData(@NotNull final Map<String, Country> countryData) {
        this.countries.clear();
        this.playerCountries.clear();
        
        for (final Map.Entry<String, Country> entry : countryData.entrySet()) {
            final Country country = entry.getValue();
            this.countries.put(entry.getKey(), country);
            
            // Rebuild player mapping
            for (final UUID citizenId : country.getCitizens().keySet()) {
                this.playerCountries.put(citizenId, entry.getKey());
            }
        }
    }
    
    /**
     * Get player's display name.
     * 
     * @param playerId the player's UUID
     * @return the player's display name
     */
    @NotNull
    public String getPlayerDisplayName(@NotNull final UUID playerId) {
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        final String name = offlinePlayer.getName();
        return name != null ? name : "Unknown Player";
    }
}