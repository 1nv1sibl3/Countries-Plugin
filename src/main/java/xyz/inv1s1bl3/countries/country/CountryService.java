package xyz.inv1s1bl3.countries.country;

import lombok.RequiredArgsConstructor;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.country.models.CountryInvitation;
import xyz.inv1s1bl3.countries.country.models.CountryMember;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;
import xyz.inv1s1bl3.countries.database.repositories.CountryRepository;
import xyz.inv1s1bl3.countries.database.repositories.PlayerRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service class for country-related business logic
 */
@RequiredArgsConstructor
public final class CountryService {
    
    private final CountriesPlugin plugin;
    private final CountryRepository countryRepository;
    private final PlayerRepository playerRepository;
    
    /**
     * Create a new country
     * @param creatorUuid Creator's UUID
     * @param countryName Country name
     * @param governmentType Government type
     * @return Created country
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public Country createCountry(final UUID creatorUuid, final String countryName, final String governmentType) throws SQLException {
        // Validate country name
        this.validateCountryName(countryName);
        
        // Check if name already exists
        if (this.countryRepository.existsByName(countryName)) {
            throw new IllegalArgumentException("A country with that name already exists");
        }
        
        // Check if player already has a country
        final Optional<Player> playerOpt = this.playerRepository.findById(creatorUuid);
        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
        
        final Player player = playerOpt.get();
        if (player.hasCountry()) {
            throw new IllegalArgumentException("Player is already a member of a country");
        }
        
        // Validate government type
        final Optional<GovernmentType> govTypeOpt = GovernmentType.fromKey(governmentType);
        if (govTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid government type");
        }
        
        final GovernmentType govType = govTypeOpt.get();
        
        // Check if player has enough money
        final double creationCost = this.plugin.getConfigManager().getCountryCreationCost();
        if (player.getBalance() < creationCost) {
            throw new IllegalArgumentException("Insufficient funds to create country");
        }
        
        // Create country
        final Country country = Country.builder()
                .name(countryName)
                .displayName(countryName)
                .description("")
                .flagDescription("")
                .governmentType(governmentType)
                .leaderUuid(creatorUuid)
                .treasuryBalance(this.plugin.getConfigManager().getMainConfig().getDouble("economy.treasury-starting-balance", 5000.0))
                .taxRate(this.plugin.getConfigManager().getMainConfig().getDouble("economy.default-tax-rate", 5.0))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
        
        final Country savedCountry = this.countryRepository.save(country);
        
        // Update player's country and role
        player.setCountryId(savedCountry.getId());
        player.setRole(govType.getLeaderRole());
        player.setBalance(player.getBalance() - creationCost);
        this.playerRepository.update(player);
        
        return savedCountry;
    }
    
    /**
     * Dissolve a country
     * @param countryId Country ID
     * @param dissolvingPlayerUuid UUID of player dissolving the country
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void dissolveCountry(final Integer countryId, final UUID dissolvingPlayerUuid) throws SQLException {
        final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
        if (countryOpt.isEmpty()) {
            throw new IllegalArgumentException("Country not found");
        }
        
        final Country country = countryOpt.get();
        
        // Check if player is the leader
        if (!country.isLeader(dissolvingPlayerUuid)) {
            throw new IllegalArgumentException("Only the country leader can dissolve the country");
        }
        
        // Remove all players from the country
        final List<Player> members = this.playerRepository.findByCountryId(countryId);
        for (final Player member : members) {
            member.setCountryId(null);
            member.setRole("citizen");
            this.playerRepository.update(member);
        }
        
        // Mark country as inactive
        country.setActive(false);
        this.countryRepository.update(country);
    }
    
    /**
     * Get country information
     * @param countryId Country ID
     * @return Country information
     * @throws SQLException if database operation fails
     */
    public Optional<Country> getCountryInfo(final Integer countryId) throws SQLException {
        return this.countryRepository.findById(countryId);
    }
    
    /**
     * Get country by name
     * @param countryName Country name
     * @return Country information
     * @throws SQLException if database operation fails
     */
    public Optional<Country> getCountryByName(final String countryName) throws SQLException {
        return this.countryRepository.findByName(countryName);
    }
    
    /**
     * Get all active countries
     * @return List of active countries
     * @throws SQLException if database operation fails
     */
    public List<Country> getAllActiveCountries() throws SQLException {
        return this.countryRepository.findActiveCountries();
    }
    
    /**
     * Get country members
     * @param countryId Country ID
     * @return List of country members
     * @throws SQLException if database operation fails
     */
    public List<Player> getCountryMembers(final Integer countryId) throws SQLException {
        return this.playerRepository.findByCountryId(countryId);
    }
    
    /**
     * Update country information
     * @param countryId Country ID
     * @param updatingPlayerUuid UUID of player making the update
     * @param displayName New display name (optional)
     * @param description New description (optional)
     * @param flagDescription New flag description (optional)
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void updateCountryInfo(final Integer countryId, final UUID updatingPlayerUuid, 
                                 final String displayName, final String description, 
                                 final String flagDescription) throws SQLException {
        final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
        if (countryOpt.isEmpty()) {
            throw new IllegalArgumentException("Country not found");
        }
        
        final Country country = countryOpt.get();
        
        // Check if player has permission to update
        final Optional<Player> playerOpt = this.playerRepository.findById(updatingPlayerUuid);
        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
        
        final Player player = playerOpt.get();
        if (!player.getCountryId().equals(countryId) || !player.hasAdminPermissions()) {
            throw new IllegalArgumentException("You don't have permission to update country information");
        }
        
        // Update country information
        if (displayName != null && !displayName.trim().isEmpty()) {
            country.setDisplayName(displayName.trim());
        }
        
        if (description != null) {
            country.setDescription(description.trim());
        }
        
        if (flagDescription != null) {
            country.setFlagDescription(flagDescription.trim());
        }
        
        this.countryRepository.update(country);
    }
    
    /**
     * Transfer country leadership
     * @param countryId Country ID
     * @param currentLeaderUuid Current leader's UUID
     * @param newLeaderUuid New leader's UUID
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void transferLeadership(final Integer countryId, final UUID currentLeaderUuid, final UUID newLeaderUuid) throws SQLException {
        final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
        if (countryOpt.isEmpty()) {
            throw new IllegalArgumentException("Country not found");
        }
        
        final Country country = countryOpt.get();
        
        // Check if current player is the leader
        if (!country.isLeader(currentLeaderUuid)) {
            throw new IllegalArgumentException("Only the current leader can transfer leadership");
        }
        
        // Check if new leader is a member of the country
        final Optional<Player> newLeaderOpt = this.playerRepository.findById(newLeaderUuid);
        if (newLeaderOpt.isEmpty()) {
            throw new IllegalArgumentException("New leader not found");
        }
        
        final Player newLeader = newLeaderOpt.get();
        if (!newLeader.getCountryId().equals(countryId)) {
            throw new IllegalArgumentException("New leader must be a member of the country");
        }
        
        // Get government type
        final Optional<GovernmentType> govTypeOpt = GovernmentType.fromKey(country.getGovernmentType());
        if (govTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid government type");
        }
        
        final GovernmentType govType = govTypeOpt.get();
        
        // Update country leader
        country.setLeaderUuid(newLeaderUuid);
        this.countryRepository.update(country);
        
        // Update player roles
        final Optional<Player> oldLeaderOpt = this.playerRepository.findById(currentLeaderUuid);
        if (oldLeaderOpt.isPresent()) {
            final Player oldLeader = oldLeaderOpt.get();
            oldLeader.setRole(govType.getDefaultRole());
            this.playerRepository.update(oldLeader);
        }
        
        newLeader.setRole(govType.getLeaderRole());
        this.playerRepository.update(newLeader);
    }
    
    /**
     * Set player role in country
     * @param countryId Country ID
     * @param settingPlayerUuid UUID of player setting the role
     * @param targetPlayerUuid UUID of target player
     * @param newRole New role
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void setPlayerRole(final Integer countryId, final UUID settingPlayerUuid, 
                             final UUID targetPlayerUuid, final String newRole) throws SQLException {
        final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
        if (countryOpt.isEmpty()) {
            throw new IllegalArgumentException("Country not found");
        }
        
        final Country country = countryOpt.get();
        
        // Get government type
        final Optional<GovernmentType> govTypeOpt = GovernmentType.fromKey(country.getGovernmentType());
        if (govTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid government type");
        }
        
        final GovernmentType govType = govTypeOpt.get();
        
        // Validate new role
        if (!govType.hasRole(newRole)) {
            throw new IllegalArgumentException("Invalid role for this government type");
        }
        
        // Check permissions
        final Optional<Player> settingPlayerOpt = this.playerRepository.findById(settingPlayerUuid);
        if (settingPlayerOpt.isEmpty()) {
            throw new IllegalArgumentException("Setting player not found");
        }
        
        final Player settingPlayer = settingPlayerOpt.get();
        if (!settingPlayer.getCountryId().equals(countryId) || !settingPlayer.hasAdminPermissions()) {
            throw new IllegalArgumentException("You don't have permission to set roles");
        }
        
        // Get target player
        final Optional<Player> targetPlayerOpt = this.playerRepository.findById(targetPlayerUuid);
        if (targetPlayerOpt.isEmpty()) {
            throw new IllegalArgumentException("Target player not found");
        }
        
        final Player targetPlayer = targetPlayerOpt.get();
        if (!targetPlayer.getCountryId().equals(countryId)) {
            throw new IllegalArgumentException("Target player is not a member of this country");
        }
        
        // Check if trying to set leader role
        if (newRole.equals(govType.getLeaderRole())) {
            throw new IllegalArgumentException("Use transfer leadership command to change leaders");
        }
        
        // Update player role
        targetPlayer.setRole(newRole);
        this.playerRepository.update(targetPlayer);
    }
    
    /**
     * Validate country name
     * @param name Country name to validate
     * @throws IllegalArgumentException if name is invalid
     */
    private void validateCountryName(final String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Country name cannot be empty");
        }
        
        final String trimmedName = name.trim();
        final int minLength = this.plugin.getConfigManager().getMainConfig().getInt("countries.min-name-length", 3);
        final int maxLength = this.plugin.getConfigManager().getMainConfig().getInt("countries.max-name-length", 20);
        
        if (trimmedName.length() < minLength) {
            throw new IllegalArgumentException("Country name must be at least " + minLength + " characters long");
        }
        
        if (trimmedName.length() > maxLength) {
            throw new IllegalArgumentException("Country name cannot be longer than " + maxLength + " characters");
        }
        
        final String allowedPattern = this.plugin.getConfigManager().getMainConfig().getString("countries.allowed-name-characters", "^[a-zA-Z0-9_\\-\\s]+$");
        if (!Pattern.matches(allowedPattern, trimmedName)) {
            throw new IllegalArgumentException("Country name contains invalid characters");
        }
    }
}