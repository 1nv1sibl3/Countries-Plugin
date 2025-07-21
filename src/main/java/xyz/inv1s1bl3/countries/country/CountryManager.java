package xyz.inv1s1bl3.countries.country;

import lombok.Getter;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;
import xyz.inv1s1bl3.countries.database.repositories.CountryRepository;
import xyz.inv1s1bl3.countries.database.repositories.PlayerRepository;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manager for country-related operations and caching
 */
@Getter
public final class CountryManager {
    
    private final CountriesPlugin plugin;
    private final CountryRepository countryRepository;
    private final PlayerRepository playerRepository;
    private final CountryService countryService;
    
    // Caches for performance
    private final Map<Integer, Country> countryCache = new ConcurrentHashMap<>();
    private final Map<UUID, Player> playerCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> countryNameCache = new ConcurrentHashMap<>();
    
    public CountryManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.countryRepository = new CountryRepository(plugin);
        this.playerRepository = new PlayerRepository(plugin);
        this.countryService = new CountryService(plugin, this.countryRepository, this.playerRepository);
    }
    
    /**
     * Initialize the country manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing Country Manager...");
        
        try {
            this.loadCaches();
            this.plugin.getLogger().info("Country Manager initialized successfully!");
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to initialize Country Manager!", exception);
            throw new RuntimeException("Country Manager initialization failed", exception);
        }
    }
    
    /**
     * Load data into caches
     */
    private void loadCaches() throws SQLException {
        // Load countries
        final List<Country> countries = this.countryRepository.findAll();
        for (final Country country : countries) {
            this.countryCache.put(country.getId(), country);
            this.countryNameCache.put(country.getName().toLowerCase(), country.getId());
        }
        
        // Load players
        final List<Player> players = this.playerRepository.findAll();
        for (final Player player : players) {
            this.playerCache.put(player.getUuid(), player);
        }
        
        this.plugin.getLogger().info("Loaded " + countries.size() + " countries and " + players.size() + " players into cache");
    }
    
    /**
     * Create a new country
     * @param creatorUuid Creator's UUID
     * @param countryName Country name
     * @param governmentType Government type
     * @return Created country
     */
    public Country createCountry(final UUID creatorUuid, final String countryName, final String governmentType) {
        try {
            final Country country = this.countryService.createCountry(creatorUuid, countryName, governmentType);
            
            // Update caches
            this.countryCache.put(country.getId(), country);
            this.countryNameCache.put(country.getName().toLowerCase(), country.getId());
            
            // Update player cache
            final Optional<Player> playerOpt = this.playerRepository.findById(creatorUuid);
            if (playerOpt.isPresent()) {
                this.playerCache.put(creatorUuid, playerOpt.get());
            }
            
            this.plugin.getLogger().info("Country '" + countryName + "' created by " + creatorUuid);
            return country;
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while creating country", exception);
            throw new RuntimeException("Failed to create country due to database error", exception);
        }
    }
    
    /**
     * Dissolve a country
     * @param countryId Country ID
     * @param dissolvingPlayerUuid UUID of player dissolving the country
     */
    public void dissolveCountry(final Integer countryId, final UUID dissolvingPlayerUuid) {
        try {
            this.countryService.dissolveCountry(countryId, dissolvingPlayerUuid);
            
            // Update cache
            final Country country = this.countryCache.get(countryId);
            if (country != null) {
                country.setActive(false);
                this.countryNameCache.remove(country.getName().toLowerCase());
            }
            
            // Update player caches for all members
            final List<Player> members = this.playerRepository.findByCountryId(countryId);
            for (final Player member : members) {
                this.playerCache.put(member.getUuid(), member);
            }
            
            this.plugin.getLogger().info("Country with ID " + countryId + " dissolved by " + dissolvingPlayerUuid);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while dissolving country", exception);
            throw new RuntimeException("Failed to dissolve country due to database error", exception);
        }
    }
    
    /**
     * Get country by ID
     * @param countryId Country ID
     * @return Country if found
     */
    public Optional<Country> getCountry(final Integer countryId) {
        // Check cache first
        final Country cachedCountry = this.countryCache.get(countryId);
        if (cachedCountry != null) {
            return Optional.of(cachedCountry);
        }
        
        // Load from database
        try {
            final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
            if (countryOpt.isPresent()) {
                final Country country = countryOpt.get();
                this.countryCache.put(countryId, country);
                this.countryNameCache.put(country.getName().toLowerCase(), countryId);
                return Optional.of(country);
            }
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting country", exception);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get country by name
     * @param countryName Country name
     * @return Country if found
     */
    public Optional<Country> getCountryByName(final String countryName) {
        // Check cache first
        final Integer countryId = this.countryNameCache.get(countryName.toLowerCase());
        if (countryId != null) {
            return this.getCountry(countryId);
        }
        
        // Load from database
        try {
            final Optional<Country> countryOpt = this.countryRepository.findByName(countryName);
            if (countryOpt.isPresent()) {
                final Country country = countryOpt.get();
                this.countryCache.put(country.getId(), country);
                this.countryNameCache.put(country.getName().toLowerCase(), country.getId());
                return Optional.of(country);
            }
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting country by name", exception);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get player data
     * @param playerUuid Player UUID
     * @return Player if found
     */
    public Optional<Player> getPlayer(final UUID playerUuid) {
        // Check cache first
        final Player cachedPlayer = this.playerCache.get(playerUuid);
        if (cachedPlayer != null) {
            return Optional.of(cachedPlayer);
        }
        
        // Load from database
        try {
            final Optional<Player> playerOpt = this.playerRepository.findById(playerUuid);
            if (playerOpt.isPresent()) {
                final Player player = playerOpt.get();
                this.playerCache.put(playerUuid, player);
                return Optional.of(player);
            }
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting player", exception);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get or create player data
     * @param playerUuid Player UUID
     * @param username Player username
     * @return Player data
     */
    public Player getOrCreatePlayer(final UUID playerUuid, final String username) {
        final Optional<Player> existingPlayer = this.getPlayer(playerUuid);
        if (existingPlayer.isPresent()) {
            final Player player = existingPlayer.get();
            // Update username if changed
            if (!player.getUsername().equals(username)) {
                player.setUsername(username);
                this.updatePlayer(player);
            }
            return player;
        }
        
        // Create new player
        final Player newPlayer = Player.builder()
                .uuid(playerUuid)
                .username(username)
                .role("citizen")
                .balance(this.plugin.getConfigManager().getStartingBalance())
                .joinedAt(java.time.LocalDateTime.now())
                .lastSeen(java.time.LocalDateTime.now())
                .isOnline(true)
                .build();
        
        try {
            final Player savedPlayer = this.playerRepository.save(newPlayer);
            this.playerCache.put(playerUuid, savedPlayer);
            return savedPlayer;
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while creating player", exception);
            throw new RuntimeException("Failed to create player due to database error", exception);
        }
    }
    
    /**
     * Update player data
     * @param player Player to update
     */
    public void updatePlayer(final Player player) {
        try {
            this.playerRepository.update(player);
            this.playerCache.put(player.getUuid(), player);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while updating player", exception);
            throw new RuntimeException("Failed to update player due to database error", exception);
        }
    }
    
    /**
     * Update country data
     * @param country Country to update
     */
    public void updateCountry(final Country country) {
        try {
            this.countryRepository.update(country);
            this.countryCache.put(country.getId(), country);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while updating country", exception);
            throw new RuntimeException("Failed to update country due to database error", exception);
        }
    }
    
    /**
     * Get all active countries
     * @return List of active countries
     */
    public List<Country> getAllActiveCountries() {
        try {
            return this.countryRepository.findActiveCountries();
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting active countries", exception);
            return List.of();
        }
    }
    
    /**
     * Get country members
     * @param countryId Country ID
     * @return List of country members
     */
    public List<Player> getCountryMembers(final Integer countryId) {
        try {
            return this.playerRepository.findByCountryId(countryId);
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while getting country members", exception);
            return List.of();
        }
    }
    
    /**
     * Set player online status
     * @param playerUuid Player UUID
     * @param online Online status
     */
    public void setPlayerOnlineStatus(final UUID playerUuid, final boolean online) {
        final Optional<Player> playerOpt = this.getPlayer(playerUuid);
        if (playerOpt.isPresent()) {
            final Player player = playerOpt.get();
            player.setOnlineStatus(online);
            this.updatePlayer(player);
        }
    }
    
    /**
     * Transfer country leadership
     * @param countryId Country ID
     * @param currentLeaderUuid Current leader's UUID
     * @param newLeaderUuid New leader's UUID
     */
    public void transferLeadership(final Integer countryId, final UUID currentLeaderUuid, final UUID newLeaderUuid) {
        try {
            this.countryService.transferLeadership(countryId, currentLeaderUuid, newLeaderUuid);
            
            // Update caches
            final Optional<Country> countryOpt = this.countryRepository.findById(countryId);
            if (countryOpt.isPresent()) {
                this.countryCache.put(countryId, countryOpt.get());
            }
            
            final Optional<Player> oldLeaderOpt = this.playerRepository.findById(currentLeaderUuid);
            if (oldLeaderOpt.isPresent()) {
                this.playerCache.put(currentLeaderUuid, oldLeaderOpt.get());
            }
            
            final Optional<Player> newLeaderOpt = this.playerRepository.findById(newLeaderUuid);
            if (newLeaderOpt.isPresent()) {
                this.playerCache.put(newLeaderUuid, newLeaderOpt.get());
            }
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while transferring leadership", exception);
            throw new RuntimeException("Failed to transfer leadership due to database error", exception);
        }
    }
    
    /**
     * Set player role
     * @param countryId Country ID
     * @param settingPlayerUuid UUID of player setting the role
     * @param targetPlayerUuid UUID of target player
     * @param newRole New role
     */
    public void setPlayerRole(final Integer countryId, final UUID settingPlayerUuid, final UUID targetPlayerUuid, final String newRole) {
        try {
            this.countryService.setPlayerRole(countryId, settingPlayerUuid, targetPlayerUuid, newRole);
            
            // Update cache
            final Optional<Player> playerOpt = this.playerRepository.findById(targetPlayerUuid);
            if (playerOpt.isPresent()) {
                this.playerCache.put(targetPlayerUuid, playerOpt.get());
            }
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Database error while setting player role", exception);
            throw new RuntimeException("Failed to set player role due to database error", exception);
        }
    }
    
    /**
     * Save all cached data to database
     */
    public void saveAllData() {
        this.plugin.getLogger().info("Saving all country data...");
        
        try {
            // Save countries
            for (final Country country : this.countryCache.values()) {
                this.countryRepository.update(country);
            }
            
            // Save players
            for (final Player player : this.playerCache.values()) {
                this.playerRepository.update(player);
            }
            
            this.plugin.getLogger().info("All country data saved successfully!");
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error saving country data", exception);
        }
    }
    
    /**
     * Reload country manager
     */
    public void reload() {
        this.plugin.getLogger().info("Reloading Country Manager...");
        
        try {
            // Clear caches
            this.countryCache.clear();
            this.playerCache.clear();
            this.countryNameCache.clear();
            
            // Reload caches
            this.loadCaches();
            
            this.plugin.getLogger().info("Country Manager reloaded successfully!");
            
        } catch (final SQLException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error reloading Country Manager", exception);
        }
    }
}