package xyz.inv1s1bl3.countries.territory;

import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;
import xyz.inv1s1bl3.countries.database.entities.Player;
import xyz.inv1s1bl3.countries.database.entities.Territory;
import xyz.inv1s1bl3.countries.database.repositories.TerritoryRepository;
import xyz.inv1s1bl3.countries.database.repositories.CountryRepository;
import xyz.inv1s1bl3.countries.database.repositories.PlayerRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for territory-related business logic
 */
@RequiredArgsConstructor
public final class TerritoryService {
    
    private final CountriesPlugin plugin;
    private final TerritoryRepository territoryRepository;
    private final CountryRepository countryRepository;
    private final PlayerRepository playerRepository;
    
    /**
     * Claim a chunk for a country
     * @param playerUuid Player claiming the chunk
     * @param chunk Chunk to claim
     * @param territoryType Type of territory
     * @return Claimed territory
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public Territory claimChunk(final UUID playerUuid, final Chunk chunk, final String territoryType) throws SQLException {
        // Validate player
        final Optional<Player> playerOpt = this.playerRepository.findById(playerUuid);
        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
        
        final Player player = playerOpt.get();
        if (!player.hasCountry()) {
            throw new IllegalArgumentException("Player must be in a country to claim territory");
        }
        
        // Validate country
        final Optional<Country> countryOpt = this.countryRepository.findById(player.getCountryId());
        if (countryOpt.isEmpty()) {
            throw new IllegalArgumentException("Country not found");
        }
        
        final Country country = countryOpt.get();
        
        // Check if chunk is already claimed
        final Optional<Territory> existingTerritory = this.territoryRepository.findByChunk(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ()
        );
        
        if (existingTerritory.isPresent()) {
            throw new IllegalArgumentException("Chunk is already claimed by another country");
        }
        
        // Check country chunk limit
        final List<Territory> countryTerritories = this.territoryRepository.findByCountryId(country.getId());
        final int maxChunks = this.plugin.getConfigManager().getMaxChunksPerCountry();
        
        if (countryTerritories.size() >= maxChunks) {
            throw new IllegalArgumentException("Country has reached maximum chunk limit (" + maxChunks + ")");
        }
        
        // Calculate claim cost
        final double baseCost = this.plugin.getConfigManager().getBaseChunkPrice();
        final double claimCost = this.calculateClaimCost(country, chunk, baseCost, type);
        
        // Check if country has enough money
        // Check if player has enough money (use Vault for actual balance)
        final CountriesPlugin plugin = this.plugin;
        if (plugin.hasVaultEconomy()) {
            if (!plugin.getEconomyManager().getVaultIntegration().hasBalance(playerUuid, claimCost)) {
                throw new IllegalArgumentException("You need $" + String.format("%.2f", claimCost) + " to claim this chunk");
            }
        } else {
            // Fallback to country treasury if no Vault
            if (!country.hasSufficientFunds(claimCost)) {
                throw new IllegalArgumentException("Country treasury has insufficient funds (need $" + claimCost + ")");
            }
        }
        
        // Validate territory type
        final Optional<TerritoryType> territoryTypeOpt = TerritoryType.fromKey(territoryType);
        if (territoryTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid territory type: " + territoryType);
        }
        
        final TerritoryType type = territoryTypeOpt.get();
        
        // Check if capital territory already exists
        if (type == TerritoryType.CAPITAL) {
            final boolean hasCapital = countryTerritories.stream()
                .anyMatch(t -> t.getTerritoryType().equals("capital"));
            
            if (hasCapital) {
                throw new IllegalArgumentException("Country already has a capital territory");
            }
        }
        
        // Create territory
        final Territory territory = Territory.builder()
            .countryId(country.getId())
            .worldName(chunk.getWorld().getName())
            .chunkX(chunk.getX())
            .chunkZ(chunk.getZ())
            .territoryType(territoryType)
            .claimedAt(LocalDateTime.now())
            .claimedByUuid(playerUuid)
            .claimCost(claimCost)
            .maintenanceCost(this.calculateMaintenanceCost(type, claimCost))
            .lastMaintenancePaid(LocalDateTime.now())
            .protectionFlags("{}")
            .build();
        
        final Territory savedTerritory = this.territoryRepository.save(territory);
        
        // Deduct cost from player or country treasury
        if (this.plugin.hasVaultEconomy()) {
            // Withdraw from player using Vault
            if (!this.plugin.getEconomyManager().getVaultIntegration().withdrawPlayer(playerUuid, claimCost)) {
                // If withdrawal fails, delete the territory and throw error
                this.territoryRepository.deleteById(savedTerritory.getId());
                throw new IllegalArgumentException("Failed to withdraw money from your account");
            }
            
            // Record transaction
            this.plugin.getEconomyManager().getTransactionManager().recordPlayerToCountryTransaction(
                playerUuid, country.getId(), claimCost, "Territory claim at " + 
                chunk.getX() + "," + chunk.getZ(), "territory", null
            );
        } else {
            // Fallback to country treasury
            country.removeFromTreasury(claimCost);
            this.countryRepository.update(country);
        }
        
        // Set as capital if it's the first territory or explicitly capital type
        if (type == TerritoryType.CAPITAL || countryTerritories.isEmpty()) {
            final int[] worldCoords = savedTerritory.getWorldCoordinates();
            country.setCapital(chunk.getWorld().getName(), worldCoords[0], worldCoords[1]);
            this.countryRepository.update(country);
        }
        
        return savedTerritory;
    }
    
    /**
     * Unclaim a chunk
     * @param playerUuid Player unclaiming the chunk
     * @param chunk Chunk to unclaim
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void unclaimChunk(final UUID playerUuid, final Chunk chunk) throws SQLException {
        // Find territory
        final Optional<Territory> territoryOpt = this.territoryRepository.findByChunk(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ()
        );
        
        if (territoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Chunk is not claimed");
        }
        
        final Territory territory = territoryOpt.get();
        
        // Validate player
        final Optional<Player> playerOpt = this.playerRepository.findById(playerUuid);
        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
        
        final Player player = playerOpt.get();
        
        // Check if player is in the same country
        if (!player.hasCountry() || !player.getCountryId().equals(territory.getCountryId())) {
            throw new IllegalArgumentException("You can only unclaim your own country's territory");
        }
        
        // Check if player has permission
        if (!player.hasAdminPermissions() && !territory.wasClaimedBy(playerUuid)) {
            throw new IllegalArgumentException("You don't have permission to unclaim this territory");
        }
        
        // Check if it's the capital
        final Optional<Country> countryOpt = this.countryRepository.findById(territory.getCountryId());
        if (countryOpt.isPresent()) {
            final Country country = countryOpt.get();
            if (country.hasCapital() && 
                country.getCapitalWorld().equals(territory.getWorldName()) &&
                country.getCapitalX().equals(territory.getWorldCoordinates()[0]) &&
                country.getCapitalZ().equals(territory.getWorldCoordinates()[1])) {
                
                // Check if there are other territories to set as capital
                final List<Territory> otherTerritories = this.territoryRepository.findByCountryId(territory.getCountryId())
                    .stream()
                    .filter(t -> !t.getId().equals(territory.getId()))
                    .toList();
                
                if (!otherTerritories.isEmpty()) {
                    // Set another territory as capital
                    final Territory newCapital = otherTerritories.get(0);
                    final int[] newCapitalCoords = newCapital.getWorldCoordinates();
                    country.setCapital(newCapital.getWorldName(), newCapitalCoords[0], newCapitalCoords[1]);
                    this.countryRepository.update(country);
                } else {
                    // Clear capital if no other territories
                    country.clearCapital();
                    this.countryRepository.update(country);
                }
            }
        }
        
        // Delete territory
        this.territoryRepository.deleteById(territory.getId());
    }
    
    /**
     * Get territory information at a location
     * @param location Location to check
     * @return Territory if claimed, empty otherwise
     * @throws SQLException if database operation fails
     */
    public Optional<Territory> getTerritoryAt(final Location location) throws SQLException {
        final Chunk chunk = location.getChunk();
        return this.territoryRepository.findByChunk(
            chunk.getWorld().getName(), chunk.getX(), chunk.getZ()
        );
    }
    
    /**
     * Get all territories for a country
     * @param countryId Country ID
     * @return List of territories
     * @throws SQLException if database operation fails
     */
    public List<Territory> getCountryTerritories(final Integer countryId) throws SQLException {
        return this.territoryRepository.findByCountryId(countryId);
    }
    
    /**
     * Change territory type
     * @param playerUuid Player changing the type
     * @param territory Territory to change
     * @param newType New territory type
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if validation fails
     */
    public void changeTerritoryType(final UUID playerUuid, final Territory territory, final String newType) throws SQLException {
        // Validate player permissions
        final Optional<Player> playerOpt = this.playerRepository.findById(playerUuid);
        if (playerOpt.isEmpty()) {
            throw new IllegalArgumentException("Player not found");
        }
        
        final Player player = playerOpt.get();
        if (!player.hasCountry() || !player.getCountryId().equals(territory.getCountryId())) {
            throw new IllegalArgumentException("You can only change your own country's territory types");
        }
        
        if (!player.hasAdminPermissions()) {
            throw new IllegalArgumentException("You don't have permission to change territory types");
        }
        
        // Validate new territory type
        final Optional<TerritoryType> newTypeOpt = TerritoryType.fromKey(newType);
        if (newTypeOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid territory type: " + newType);
        }
        
        final TerritoryType type = newTypeOpt.get();
        
        // Check capital restriction
        if (type == TerritoryType.CAPITAL) {
            final List<Territory> countryTerritories = this.territoryRepository.findByCountryId(territory.getCountryId());
            final boolean hasCapital = countryTerritories.stream()
                .filter(t -> !t.getId().equals(territory.getId()))
                .anyMatch(t -> t.getTerritoryType().equals("capital"));
            
            if (hasCapital) {
                throw new IllegalArgumentException("Country already has a capital territory");
            }
        }
        
        // Calculate type change cost
        final double changeCost = type.getCreationCost() * 0.5; // 50% of creation cost
        
        final Optional<Country> countryOpt = this.countryRepository.findById(territory.getCountryId());
        if (countryOpt.isPresent()) {
            final Country country = countryOpt.get();
            if (!country.hasSufficientFunds(changeCost)) {
                throw new IllegalArgumentException("Country treasury has insufficient funds (need $" + changeCost + ")");
            }
            
            // Deduct cost
            country.removeFromTreasury(changeCost);
            this.countryRepository.update(country);
        }
        
        // Update territory
        territory.setTerritoryType(newType);
        territory.setMaintenanceCost(this.calculateMaintenanceCost(type, territory.getClaimCost()));
        this.territoryRepository.update(territory);
        
        // Update capital if changed to capital
        if (type == TerritoryType.CAPITAL && countryOpt.isPresent()) {
            final Country country = countryOpt.get();
            final int[] coords = territory.getWorldCoordinates();
            country.setCapital(territory.getWorldName(), coords[0], coords[1]);
            this.countryRepository.update(country);
        }
    }
    
    /**
     * Check if a player can build at a location
     * @param playerUuid Player UUID
     * @param location Location to check
     * @return true if player can build
     * @throws SQLException if database operation fails
     */
    public boolean canPlayerBuild(final UUID playerUuid, final Location location) throws SQLException {
        final Optional<Territory> territoryOpt = this.getTerritoryAt(location);
        
        // If unclaimed, allow building
        if (territoryOpt.isEmpty()) {
            return true;
        }
        
        final Territory territory = territoryOpt.get();
        
        // Get player data
        final Optional<Player> playerOpt = this.playerRepository.findById(playerUuid);
        if (playerOpt.isEmpty()) {
            return false;
        }
        
        final Player player = playerOpt.get();
        
        // Check if player is in the same country
        if (player.hasCountry() && player.getCountryId().equals(territory.getCountryId())) {
            return true;
        }
        
        // Check bypass permissions
        final org.bukkit.entity.Player bukkitPlayer = this.plugin.getServer().getPlayer(playerUuid);
        if (bukkitPlayer != null && bukkitPlayer.hasPermission("countries.bypass")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculate claim cost for a chunk
     * @param country Country claiming
     * @param chunk Chunk to claim
     * @param baseCost Base cost per chunk
     * @param territoryType Territory type being claimed
     * @return Calculated cost
     */
    private double calculateClaimCost(final Country country, final Chunk chunk, final double baseCost, final TerritoryType territoryType) {
        double cost = baseCost * territoryType.getCreationCost() / 100.0; // Territory type multiplier
        
        // Distance multiplier from capital
        if (country.hasCapital()) {
            final World world = chunk.getWorld();
            final Location chunkCenter = new Location(world, chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
            final Location capital = new Location(
                this.plugin.getServer().getWorld(country.getCapitalWorld()),
                country.getCapitalX(),
                64,
                country.getCapitalZ()
            );
            
            if (capital.getWorld() != null && capital.getWorld().equals(world)) {
                final double distance = chunkCenter.distance(capital);
                final double distanceMultiplier = this.plugin.getConfigManager().getMainConfig()
                    .getDouble("territories.distance-multiplier", 1.1);
                
                cost *= Math.pow(distanceMultiplier, distance / 1000.0); // Per 1000 blocks
            }
        }
        
        // Country size multiplier (more territories = higher cost)
        final List<Territory> existingTerritories = this.territoryRepository.findByCountryId(country.getId());
        final double sizeMultiplier = 1.0 + (existingTerritories.size() * 0.05); // 5% increase per territory
        cost *= sizeMultiplier;
        
        // Economic health modifier
        final double economicHealth = this.plugin.getEconomyManager().getEconomyIntegration()
            .calculateCountryEconomicHealth(country.getId());
        
        if (economicHealth < 0.3) {
            cost *= 1.5; // 50% penalty for poor economic health
        } else if (economicHealth > 0.8) {
            cost *= 0.9; // 10% discount for excellent economic health
        }
        
        return Math.round(cost * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    /**
     * Calculate maintenance cost for a territory
     * @param type Territory type
     * @param claimCost Original claim cost
     * @return Daily maintenance cost
     */
    private double calculateMaintenanceCost(final TerritoryType type, final double claimCost) {
        final double baseMaintenanceCost = this.plugin.getConfigManager().getMainConfig()
            .getDouble("territories.base-maintenance-cost", 10.0);
        
        return baseMaintenanceCost * type.getMaintenanceMultiplier();
    }
}