package xyz.inv1s1bl3.countries.storage;

import com.google.gson.JsonObject;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.core.country.CitizenRole;
import xyz.inv1s1bl3.countries.core.country.GovernmentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles database operations for countries.
 */
public class CountryStorage {
    
    private final DataManager dataManager;
    
    public CountryStorage(DataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    /**
     * Save a country to the database
     */
    public void saveCountry(Country country) {
        try (Connection connection = dataManager.getConnection()) {
            // Save country data
            String countrySQL = """
                INSERT OR REPLACE INTO countries 
                (name, owner_uuid, founded_date, government_type, balance, tax_rate, data, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(countrySQL)) {
                JsonObject data = new JsonObject();
                data.addProperty("description", country.getDescription());
                data.addProperty("flag", country.getFlag());
                data.addProperty("openBorders", country.hasOpenBorders());
                data.addProperty("allowForeigners", country.allowsForeigners());
                data.addProperty("maxCitizens", country.getMaxCitizens());
                data.addProperty("lastActive", country.getLastActive());
                data.addProperty("totalTerritories", country.getTotalTerritories());
                
                stmt.setString(1, country.getName());
                stmt.setString(2, country.getOwnerUUID().toString());
                stmt.setLong(3, country.getFoundedDate());
                stmt.setString(4, country.getGovernmentType().name());
                stmt.setDouble(5, country.getBalance());
                stmt.setDouble(6, country.getTaxRate());
                stmt.setString(7, dataManager.getGson().toJson(data));
                stmt.setLong(8, System.currentTimeMillis());
                
                stmt.executeUpdate();
            }
            
            // Get country ID
            int countryId = getCountryId(connection, country.getName());
            if (countryId == -1) {
                throw new SQLException("Failed to get country ID for: " + country.getName());
            }
            
            // Clear existing citizens
            String deleteCitizensSQL = "DELETE FROM citizens WHERE country_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteCitizensSQL)) {
                stmt.setInt(1, countryId);
                stmt.executeUpdate();
            }
            
            // Save citizens
            String citizenSQL = """
                INSERT INTO citizens 
                (country_id, player_uuid, role, joined_date, salary)
                VALUES (?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(citizenSQL)) {
                for (Citizen citizen : country.getCitizens()) {
                    stmt.setInt(1, countryId);
                    stmt.setString(2, citizen.getPlayerUUID().toString());
                    stmt.setString(3, citizen.getRole().name());
                    stmt.setLong(4, citizen.getJoinedDate());
                    stmt.setDouble(5, citizen.getSalary());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            dataManager.getPlugin().debug("Saved country: " + country.getName());
            
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error saving country: " + country.getName(), e);
        }
    }
    
    /**
     * Load a country from the database
     */
    public Country loadCountry(String name) {
        try (Connection connection = dataManager.getConnection()) {
            String sql = "SELECT * FROM countries WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return createCountryFromResultSet(rs, connection);
                    }
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error loading country: " + name, e);
        }
        
        return null;
    }
    
    /**
     * Load all countries from the database
     */
    public List<Country> loadAllCountries() {
        List<Country> countries = new ArrayList<>();
        
        try (Connection connection = dataManager.getConnection()) {
            String sql = "SELECT * FROM countries ORDER BY name";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Country country = createCountryFromResultSet(rs, connection);
                        if (country != null) {
                            countries.add(country);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error loading all countries", e);
        }
        
        return countries;
    }
    
    /**
     * Delete a country from the database
     */
    public void deleteCountry(String name) {
        try (Connection connection = dataManager.getConnection()) {
            String sql = "DELETE FROM countries WHERE name = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                int deleted = stmt.executeUpdate();
                
                if (deleted > 0) {
                    dataManager.getPlugin().debug("Deleted country: " + name);
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error deleting country: " + name, e);
        }
    }
    
    /**
     * Save all countries (batch operation)
     */
    public void saveAll() {
        // This will be called by the main save operation
        dataManager.getPlugin().debug("Country storage save-all completed");
    }
    
    /**
     * Create a Country object from database result set
     */
    private Country createCountryFromResultSet(ResultSet rs, Connection connection) throws SQLException {
        String name = rs.getString("name");
        UUID ownerUUID = UUID.fromString(rs.getString("owner_uuid"));
        long foundedDate = rs.getLong("founded_date");
        GovernmentType governmentType = GovernmentType.valueOf(rs.getString("government_type"));
        double balance = rs.getDouble("balance");
        double taxRate = rs.getDouble("tax_rate");
        String dataJson = rs.getString("data");
        
        // Create country
        Country country = new Country(name, ownerUUID, governmentType);
        country.setBalance(balance);
        country.setTaxRate(taxRate);
        
        // Parse additional data
        if (dataJson != null && !dataJson.isEmpty()) {
            try {
                JsonObject data = dataManager.getGson().fromJson(dataJson, JsonObject.class);
                
                if (data.has("description")) {
                    country.setDescription(data.get("description").getAsString());
                }
                if (data.has("flag")) {
                    country.setFlag(data.get("flag").getAsString());
                }
                if (data.has("openBorders")) {
                    country.setOpenBorders(data.get("openBorders").getAsBoolean());
                }
                if (data.has("allowForeigners")) {
                    country.setAllowForeigners(data.get("allowForeigners").getAsBoolean());
                }
                if (data.has("maxCitizens")) {
                    country.setMaxCitizens(data.get("maxCitizens").getAsInt());
                }
                if (data.has("totalTerritories")) {
                    country.setTotalTerritories(data.get("totalTerritories").getAsInt());
                }
            } catch (Exception e) {
                dataManager.getPlugin().getLogger().log(Level.WARNING, 
                        "Error parsing country data for: " + name, e);
            }
        }
        
        // Load citizens
        loadCitizens(country, rs.getInt("id"), connection);
        
        return country;
    }
    
    /**
     * Load citizens for a country
     */
    private void loadCitizens(Country country, int countryId, Connection connection) throws SQLException {
        String sql = "SELECT * FROM citizens WHERE country_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, countryId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                    CitizenRole role = CitizenRole.valueOf(rs.getString("role"));
                    long joinedDate = rs.getLong("joined_date");
                    double salary = rs.getDouble("salary");
                    long lastSalaryPaid = rs.getLong("created_at"); // Use created_at as placeholder
                    
                    // Get player name (this could be cached or looked up)
                    String playerName = getPlayerName(playerUUID);
                    
                    Citizen citizen = new Citizen(playerUUID, playerName, role, joinedDate, salary, lastSalaryPaid);
                    country.addCitizen(playerUUID, playerName, role);
                    
                    // Update the citizen with loaded data
                    Citizen loadedCitizen = country.getCitizen(playerUUID);
                    if (loadedCitizen != null) {
                        loadedCitizen.setSalary(salary);
                        loadedCitizen.setLastSalaryPaid(lastSalaryPaid);
                    }
                }
            }
        }
    }
    
    /**
     * Get country ID from database
     */
    private int getCountryId(Connection connection, String countryName) throws SQLException {
        String sql = "SELECT id FROM countries WHERE name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, countryName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        
        return -1;
    }
    
    /**
     * Get player name by UUID (placeholder implementation)
     */
    private String getPlayerName(UUID playerUUID) {
        // In a real implementation, this could:
        // 1. Check online players first
        // 2. Query a player cache/database
        // 3. Use Bukkit's OfflinePlayer (though this can be slow)
        
        var player = dataManager.getPlugin().getServer().getPlayer(playerUUID);
        if (player != null) {
            return player.getName();
        }
        
        var offlinePlayer = dataManager.getPlugin().getServer().getOfflinePlayer(playerUUID);
        String name = offlinePlayer.getName();
        return name != null ? name : "Unknown";
    }
}