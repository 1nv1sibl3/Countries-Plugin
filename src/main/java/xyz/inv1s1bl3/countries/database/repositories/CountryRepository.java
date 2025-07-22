package xyz.inv1s1bl3.countries.database.repositories;

import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Country;

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
 * Repository for country data operations
 */
public final class CountryRepository implements Repository<Country, Integer> {
    
    private final CountriesPlugin plugin;
    
    public CountryRepository(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Country save(final Country country) throws SQLException {
        final String sql = """
            INSERT INTO countries (name, display_name, description, flag_description, government_type,
                                 leader_uuid, treasury_balance, tax_rate, created_at, capital_world,
                                 capital_x, capital_z, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, country.getName());
                statement.setString(2, country.getDisplayName());
                statement.setString(3, country.getDescription());
                statement.setString(4, country.getFlagDescription());
                statement.setString(5, country.getGovernmentType());
                statement.setString(6, country.getLeaderUuid().toString());
                statement.setDouble(7, country.getTreasuryBalance());
                statement.setDouble(8, country.getTaxRate());
                statement.setString(9, country.getCreatedAt().toString());
                statement.setString(10, country.getCapitalWorld());
                if (country.getCapitalX() != null) {
                    statement.setInt(11, country.getCapitalX());
                } else {
                    statement.setNull(11, java.sql.Types.INTEGER);
                }
                if (country.getCapitalZ() != null) {
                    statement.setInt(12, country.getCapitalZ());
                } else {
                    statement.setNull(12, java.sql.Types.INTEGER);
                }
                statement.setBoolean(13, country.isActive());
                
                statement.executeUpdate();
                
                try (final ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        country.setId(generatedKeys.getInt(1));
                    }
                }
                
                return country;
            }
        });
    }
    
    @Override
    public Country update(final Country country) throws SQLException {
        final String sql = """
            UPDATE countries SET display_name = ?, description = ?, flag_description = ?,
                               government_type = ?, leader_uuid = ?, treasury_balance = ?,
                               tax_rate = ?, capital_world = ?, capital_x = ?, capital_z = ?,
                               is_active = ?
            WHERE id = ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, country.getDisplayName());
                statement.setString(2, country.getDescription());
                statement.setString(3, country.getFlagDescription());
                statement.setString(4, country.getGovernmentType());
                statement.setString(5, country.getLeaderUuid().toString());
                statement.setDouble(6, country.getTreasuryBalance());
                statement.setDouble(7, country.getTaxRate());
                statement.setString(8, country.getCapitalWorld());
                if (country.getCapitalX() != null) {
                    statement.setInt(9, country.getCapitalX());
                } else {
                    statement.setNull(9, java.sql.Types.INTEGER);
                }
                if (country.getCapitalZ() != null) {
                    statement.setInt(10, country.getCapitalZ());
                } else {
                    statement.setNull(10, java.sql.Types.INTEGER);
                }
                statement.setBoolean(11, country.isActive());
                statement.setInt(12, country.getId());
                
                statement.executeUpdate();
                return country;
            }
        });
    }
    
    @Override
    public Optional<Country> findById(final Integer id) throws SQLException {
        final String sql = "SELECT * FROM countries WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToCountry(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public List<Country> findAll() throws SQLException {
        final String sql = "SELECT * FROM countries ORDER BY name";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Country> countries = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    countries.add(this.mapResultSetToCountry(resultSet));
                }
            }
            
            return countries;
        });
    }
    
    @Override
    public boolean deleteById(final Integer id) throws SQLException {
        final String sql = "DELETE FROM countries WHERE id = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }
    
    @Override
    public boolean existsById(final Integer id) throws SQLException {
        final String sql = "SELECT 1 FROM countries WHERE id = ? LIMIT 1";
        
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
        final String sql = "SELECT COUNT(*) FROM countries";
        
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
     * Find country by name (case-insensitive)
     * @param name Country name
     * @return Optional containing country if found
     * @throws SQLException if query fails
     */
    public Optional<Country> findByName(final String name) throws SQLException {
        final String sql = "SELECT * FROM countries WHERE LOWER(name) = LOWER(?)";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToCountry(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    /**
     * Find country by leader UUID
     * @param leaderUuid Leader UUID
     * @return Optional containing country if found
     * @throws SQLException if query fails
     */
    public Optional<Country> findByLeader(final UUID leaderUuid) throws SQLException {
        final String sql = "SELECT * FROM countries WHERE leader_uuid = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, leaderUuid.toString());
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToCountry(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    /**
     * Find all active countries
     * @return List of active countries
     * @throws SQLException if query fails
     */
    public List<Country> findActiveCountries() throws SQLException {
        final String sql = "SELECT * FROM countries WHERE is_active = TRUE ORDER BY name";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Country> countries = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    countries.add(this.mapResultSetToCountry(resultSet));
                }
            }
            
            return countries;
        });
    }
    
    /**
     * Update country treasury balance
     * @param countryId Country ID
     * @param newBalance New treasury balance
     * @throws SQLException if update fails
     */
    public void updateTreasuryBalance(final Integer countryId, final double newBalance) throws SQLException {
        final String sql = "UPDATE countries SET treasury_balance = ? WHERE id = ?";
        
        this.plugin.getDatabaseManager().executeVoidOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, newBalance);
                statement.setInt(2, countryId);
                statement.executeUpdate();
            }
        });
    }
    
    /**
     * Update country leader
     * @param countryId Country ID
     * @param newLeaderUuid New leader UUID
     * @throws SQLException if update fails
     */
    public void updateLeader(final Integer countryId, final UUID newLeaderUuid) throws SQLException {
        final String sql = "UPDATE countries SET leader_uuid = ? WHERE id = ?";
        
        this.plugin.getDatabaseManager().executeVoidOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, newLeaderUuid.toString());
                statement.setInt(2, countryId);
                statement.executeUpdate();
            }
        });
    }
    
    /**
     * Check if country name exists (case-insensitive)
     * @param name Country name
     * @return true if name exists
     * @throws SQLException if query fails
     */
    public boolean existsByName(final String name) throws SQLException {
        final String sql = "SELECT 1 FROM countries WHERE LOWER(name) = LOWER(?) LIMIT 1";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, name);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }
    
    /**
     * Map ResultSet to Country entity
     */
    private Country mapResultSetToCountry(final ResultSet resultSet) throws SQLException {
        final String capitalXStr = resultSet.getString("capital_x");
        final String capitalZStr = resultSet.getString("capital_z");
        
        return Country.builder()
                .id(resultSet.getInt("id"))
                .name(resultSet.getString("name"))
                .displayName(resultSet.getString("display_name"))
                .description(resultSet.getString("description"))
                .flagDescription(resultSet.getString("flag_description"))
                .governmentType(resultSet.getString("government_type"))
                .leaderUuid(UUID.fromString(resultSet.getString("leader_uuid")))
                .treasuryBalance(resultSet.getDouble("treasury_balance"))
                .taxRate(resultSet.getDouble("tax_rate"))
                .createdAt(LocalDateTime.parse(resultSet.getString("created_at")))
                .capitalWorld(resultSet.getString("capital_world"))
                .capitalX(capitalXStr != null ? Integer.parseInt(capitalXStr) : null)
                .capitalZ(capitalZStr != null ? Integer.parseInt(capitalZStr) : null)
                .isActive(resultSet.getBoolean("is_active"))
                .build();
    }
}