package xyz.inv1s1bl3.countries.database.repositories;

import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Player;

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
 * Repository for player data operations
 */
public final class PlayerRepository implements Repository<Player, UUID> {
    
    private final CountriesPlugin plugin;
    
    public PlayerRepository(final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Player save(final Player player) throws SQLException {
        final String sql = """
            INSERT INTO players (uuid, username, country_id, role, balance, joined_at, last_seen, is_online)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.getUuid().toString());
                statement.setString(2, player.getUsername());
                if (player.getCountryId() != null) {
                    statement.setInt(3, player.getCountryId());
                } else {
                    statement.setNull(3, java.sql.Types.INTEGER);
                }
                statement.setString(4, player.getRole());
                statement.setDouble(5, player.getBalance());
                statement.setString(6, player.getJoinedAt().toString());
                statement.setString(7, player.getLastSeen().toString());
                statement.setBoolean(8, player.isOnline());
                
                statement.executeUpdate();
                return player;
            }
        });
    }
    
    @Override
    public Player update(final Player player) throws SQLException {
        final String sql = """
            UPDATE players SET username = ?, country_id = ?, role = ?, balance = ?, 
                              last_seen = ?, is_online = ?
            WHERE uuid = ?
            """;
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, player.getUsername());
                if (player.getCountryId() != null) {
                    statement.setInt(2, player.getCountryId());
                } else {
                    statement.setNull(2, java.sql.Types.INTEGER);
                }
                statement.setString(3, player.getRole());
                statement.setDouble(4, player.getBalance());
                statement.setString(5, player.getLastSeen().toString());
                statement.setBoolean(6, player.isOnline());
                statement.setString(7, player.getUuid().toString());
                
                statement.executeUpdate();
                return player;
            }
        });
    }
    
    @Override
    public Optional<Player> findById(final UUID uuid) throws SQLException {
        final String sql = "SELECT * FROM players WHERE uuid = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToPlayer(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    @Override
    public List<Player> findAll() throws SQLException {
        final String sql = "SELECT * FROM players ORDER BY username";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Player> players = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    players.add(this.mapResultSetToPlayer(resultSet));
                }
            }
            
            return players;
        });
    }
    
    @Override
    public boolean deleteById(final UUID uuid) throws SQLException {
        final String sql = "DELETE FROM players WHERE uuid = ?";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                return statement.executeUpdate() > 0;
            }
        });
    }
    
    @Override
    public boolean existsById(final UUID uuid) throws SQLException {
        final String sql = "SELECT 1 FROM players WHERE uuid = ? LIMIT 1";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }
    
    @Override
    public long count() throws SQLException {
        final String sql = "SELECT COUNT(*) FROM players";
        
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
     * Find player by username
     * @param username Player username
     * @return Optional containing player if found
     * @throws SQLException if query fails
     */
    public Optional<Player> findByUsername(final String username) throws SQLException {
        final String sql = "SELECT * FROM players WHERE LOWER(username) = LOWER(?)";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, username);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return Optional.of(this.mapResultSetToPlayer(resultSet));
                    }
                    return Optional.empty();
                }
            }
        });
    }
    
    /**
     * Find all players in a country
     * @param countryId Country ID
     * @return List of players in the country
     * @throws SQLException if query fails
     */
    public List<Player> findByCountryId(final Integer countryId) throws SQLException {
        final String sql = "SELECT * FROM players WHERE country_id = ? ORDER BY role, username";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Player> players = new ArrayList<>();
            
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, countryId);
                
                try (final ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        players.add(this.mapResultSetToPlayer(resultSet));
                    }
                }
            }
            
            return players;
        });
    }
    
    /**
     * Find all online players
     * @return List of online players
     * @throws SQLException if query fails
     */
    public List<Player> findOnlinePlayers() throws SQLException {
        final String sql = "SELECT * FROM players WHERE is_online = TRUE ORDER BY username";
        
        return this.plugin.getDatabaseManager().executeOperation(connection -> {
            final List<Player> players = new ArrayList<>();
            
            try (final Statement statement = connection.createStatement();
                 final ResultSet resultSet = statement.executeQuery(sql)) {
                
                while (resultSet.next()) {
                    players.add(this.mapResultSetToPlayer(resultSet));
                }
            }
            
            return players;
        });
    }
    
    /**
     * Update player balance
     * @param uuid Player UUID
     * @param newBalance New balance
     * @throws SQLException if update fails
     */
    public void updateBalance(final UUID uuid, final double newBalance) throws SQLException {
        final String sql = "UPDATE players SET balance = ? WHERE uuid = ?";
        
        this.plugin.getDatabaseManager().executeVoidOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setDouble(1, newBalance);
                statement.setString(2, uuid.toString());
                statement.executeUpdate();
            }
        });
    }
    
    /**
     * Set player country and role
     * @param uuid Player UUID
     * @param countryId Country ID (null to remove from country)
     * @param role Player role
     * @throws SQLException if update fails
     */
    public void setCountryAndRole(final UUID uuid, final Integer countryId, final String role) throws SQLException {
        final String sql = "UPDATE players SET country_id = ?, role = ? WHERE uuid = ?";
        
        this.plugin.getDatabaseManager().executeVoidOperation(connection -> {
            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                if (countryId != null) {
                    statement.setInt(1, countryId);
                } else {
                    statement.setNull(1, java.sql.Types.INTEGER);
                }
                statement.setString(2, role);
                statement.setString(3, uuid.toString());
                statement.executeUpdate();
            }
        });
    }
    
    /**
     * Map ResultSet to Player entity
     */
    private Player mapResultSetToPlayer(final ResultSet resultSet) throws SQLException {
        return Player.builder()
                .uuid(UUID.fromString(resultSet.getString("uuid")))
                .username(resultSet.getString("username"))
                .countryId(resultSet.getObject("country_id", Integer.class))
                .role(resultSet.getString("role"))
                .balance(resultSet.getDouble("balance"))
                .joinedAt(LocalDateTime.parse(resultSet.getString("joined_at")))
                .lastSeen(LocalDateTime.parse(resultSet.getString("last_seen")))
                .isOnline(resultSet.getBoolean("is_online"))
                .build();
    }
}