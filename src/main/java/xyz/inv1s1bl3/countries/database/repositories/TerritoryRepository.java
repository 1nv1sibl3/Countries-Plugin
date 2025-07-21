package xyz.inv1s1bl3.countries.database.repositories;

import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Territory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for territory data operations
 */
public final class TerritoryRepository implements Repository<Territory, Integer> {
    
    private final CountriesPlugin plugin;
    
    public TerritoryRepository(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Territory save(final Territory territory) throws SQLException {
        final String sql = """
            INSERT INTO territories (country_id, world_name, chunk_x, chunk_z, territory_type,
                                   claimed_at, claimed_by_uuid, claim_cost, maintenance_cost,
                                   last_maintenance_paid, protection_flags)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, territory.getCountryId());
                statement.setString(2, territory.getWorldName());
                statement.setInt(3, territory.getChunkX());
                statement.setInt(4, territory.getChunkZ());
                statement.setString(5, territory.getTerritoryType());
                statement.setString(6, territory.getClaimedAt().toString());
                statement.setString(7, territory.getClaimedByUuid().toString());
                statement.setDouble(8, territory.getClaimCost());
                statement.setDouble(9, territory.getMaintenanceCost());
                statement.setString(10, territory.getLastMaintenancePaid().toString());
                statement.setString(11, territory.getProtectionFlags());
                
                statement.executeUpdate();
                
                try (final ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        territory.setId(generatedKeys.getInt(1));
                    }
                }
                
                return territory;
            }
        });
    }
    
    @Override
    public Territory update(final Territory territory) throws SQLException {
        final String sql = """
            UPDATE territories SET territory_type = ?, maintenance_cost = ?,
                                 last_maintenance_paid = ?, protection_flags = ?
            WHERE id = ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, territory.getTerritoryType());
                statement.setDouble(2, territory.getMaintenanceCost());
                statement.setString(3, territory.getLastMaintenancePaid().toString());
                statement.setString(4, territory.getProtectionFlags());
                statement.setInt(5, territory.getId());
                
                statement.executeUpdate();
                return territory;
            }
        });
    }
    
    @Override
    public Optional<Territory> findById(final Integer id) throws SQLException {
        final String sql = "SELECT * FROM territories WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToTerritory(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public List<Territory> findAll() throws SQLException {
        final String sql = "SELECT * FROM territories ORDER BY country_id, world_name, chunk_x, chunk_z";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Territory> territories = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    territories.add(this.mapResultSetToTerritory(resultSet));
                }
            }
            
            return territories;
        });
    }
    
    @Override
    public boolean deleteById(final Integer id) throws SQLException {
        final String sql = "DELETE FROM territories WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }
    
    @Override
    public boolean existsById(final Integer id) throws SQLException {
        final String sql = "SELECT 1 FROM territories WHERE id = ? LIMIT 1";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }
    
    @Override
    public long count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM territories";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            }
        });
    }
    
    /**
     * Find territory by chunk coordinates
     * @param worldName World name
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @return Optional containing territory if found
     * @throws SQLException if query fails
     */
    public Optional<Territory> findByChunk(final String worldName, final int chunkX, final int chunkZ) throws SQLException {
        final String sql = "SELECT * FROM territories WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, worldName);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToTerritory(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    /**
     * Find all territories for a country
     * @param countryId Country ID
     * @return List of territories
     * @throws SQLException if query fails
     */
    public List<Territory> findByCountryId(final Integer countryId) throws SQLException {
        final String sql = "SELECT * FROM territories WHERE country_id = ? ORDER BY world_name, chunk_x, chunk_z";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Territory> territories = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        territories.add(this.mapResultSetToTerritory(resultSet));
                    }
                }
            }
            
            return territories;
        });
    }
    
    /**
     * Find territories by type
     * @param territoryType Territory type
     * @return List of territories
     * @throws SQLException if query fails
     */
    public List<Territory> findByType(final String territoryType) throws SQLException {
        final String sql = "SELECT * FROM territories WHERE territory_type = ? ORDER BY country_id, world_name, chunk_x, chunk_z";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Territory> territories = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, territoryType);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        territories.add(this.mapResultSetToTerritory(resultSet));
                    }
                }
            }
            
            return territories;
        });
    }
    
    /**
     * Find territories in a world
     * @param worldName World name
     * @return List of territories
     * @throws SQLException if query fails
     */
    public List<Territory> findByWorld(final String worldName) throws SQLException {
        final String sql = "SELECT * FROM territories WHERE world_name = ? ORDER BY chunk_x, chunk_z";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Territory> territories = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, worldName);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        territories.add(this.mapResultSetToTerritory(resultSet));
                    }
                }
            }
            
            return territories;
        });
    }
    
    /**
     * Find territories that need maintenance
     * @param maintenanceInterval Maintenance interval in hours
     * @return List of territories needing maintenance
     * @throws SQLException if query fails
     */
    public List<Territory> findTerritoriesNeedingMaintenance(final int maintenanceInterval) throws SQLException {
        final String sql = """
            SELECT * FROM territories 
            WHERE datetime(last_maintenance_paid, '+' || ? || ' hours') < datetime('now')
            ORDER BY last_maintenance_paid
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Territory> territories = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, maintenanceInterval);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        territories.add(this.mapResultSetToTerritory(resultSet));
                    }
                }
            }
            
            return territories;
        });
    }
    
    /**
     * Count territories by country
     * @param countryId Country ID
     * @return Number of territories
     * @throws SQLException if query fails
     */
    public long countByCountryId(final Integer countryId) throws SQLException {
        final String sql = "SELECT COUNT(*) FROM territories WHERE country_id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong(1);
                    }
                    return 0L;
                }
            }
        });
    }
    
    /**
     * Map ResultSet to Territory entity
     */
    private Territory mapResultSetToTerritory(final ResultSet resultSet) throws SQLException {
        return Territory.builder()
                .id(resultSet.getInt("id"))
                .countryId(resultSet.getInt("country_id"))
                .worldName(resultSet.getString("world_name"))
                .chunkX(resultSet.getInt("chunk_x"))
                .chunkZ(resultSet.getInt("chunk_z"))
                .territoryType(resultSet.getString("territory_type"))
                .claimedAt(LocalDateTime.parse(resultSet.getString("claimed_at")))
                .claimedByUuid(UUID.fromString(resultSet.getString("claimed_by_uuid")))
                .claimCost(resultSet.getDouble("claim_cost"))
                .maintenanceCost(resultSet.getDouble("maintenance_cost"))
                .lastMaintenancePaid(LocalDateTime.parse(resultSet.getString("last_maintenance_paid")))
                .protectionFlags(resultSet.getString("protection_flags"))
                .build();
    }
}