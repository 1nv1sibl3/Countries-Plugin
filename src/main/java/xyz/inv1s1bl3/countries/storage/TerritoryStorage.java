package xyz.inv1s1bl3.countries.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import xyz.inv1s1bl3.countries.core.territory.ChunkCoordinate;
import xyz.inv1s1bl3.countries.core.territory.Territory;
import xyz.inv1s1bl3.countries.core.territory.TerritoryType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Handles database operations for territories.
 */
public class TerritoryStorage {
    
    private final DataManager dataManager;
    
    public TerritoryStorage(DataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    /**
     * Save a territory to the database
     */
    public void saveTerritory(Territory territory) {
        try (Connection connection = dataManager.getConnection()) {
            String sql = """
                INSERT OR REPLACE INTO territories 
                (name, country_id, world_name, chunks, territory_type, claimed_date, data, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                // Get country ID
                int countryId = getCountryId(connection, territory.getCountryName());
                if (countryId == -1) {
                    throw new SQLException("Country not found: " + territory.getCountryName());
                }
                
                // Serialize chunks
                JsonArray chunksArray = new JsonArray();
                for (ChunkCoordinate chunk : territory.getChunks()) {
                    chunksArray.add(chunk.serialize());
                }
                
                // Create data object
                JsonObject data = new JsonObject();
                data.addProperty("description", territory.getDescription());
                data.addProperty("allowPublicAccess", territory.allowsPublicAccess());
                data.addProperty("allowBuilding", territory.allowsBuilding());
                data.addProperty("allowPvP", territory.allowsPvP());
                data.addProperty("upkeepCost", territory.getUpkeepCost());
                data.addProperty("lastActive", territory.getLastActive());
                data.addProperty("totalVisitors", territory.getTotalVisitors());
                
                // Serialize allowed players
                JsonArray allowedPlayers = new JsonArray();
                for (UUID playerUUID : territory.getAllowedPlayers()) {
                    allowedPlayers.add(playerUUID.toString());
                }
                data.add("allowedPlayers", allowedPlayers);
                
                // Serialize allowed countries
                JsonArray allowedCountries = new JsonArray();
                for (String countryName : territory.getAllowedCountries()) {
                    allowedCountries.add(countryName);
                }
                data.add("allowedCountries", allowedCountries);
                
                stmt.setString(1, territory.getName());
                stmt.setInt(2, countryId);
                stmt.setString(3, territory.getWorldName());
                stmt.setString(4, dataManager.getGson().toJson(chunksArray));
                stmt.setString(5, territory.getType().name());
                stmt.setLong(6, territory.getClaimedDate());
                stmt.setString(7, dataManager.getGson().toJson(data));
                stmt.setLong(8, System.currentTimeMillis());
                
                stmt.executeUpdate();
            }
            
            dataManager.getPlugin().debug("Saved territory: " + territory.getName());
            
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error saving territory: " + territory.getName(), e);
        }
    }
    
    /**
     * Load a territory from the database
     */
    public Territory loadTerritory(String name, String countryName) {
        try (Connection connection = dataManager.getConnection()) {
            String sql = """
                SELECT t.*, c.name as country_name 
                FROM territories t 
                JOIN countries c ON t.country_id = c.id 
                WHERE t.name = ? AND c.name = ?
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, countryName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return createTerritoryFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error loading territory: " + name, e);
        }
        
        return null;
    }
    
    /**
     * Load all territories for a country
     */
    public List<Territory> loadCountryTerritories(String countryName) {
        List<Territory> territories = new ArrayList<>();
        
        try (Connection connection = dataManager.getConnection()) {
            String sql = """
                SELECT t.*, c.name as country_name 
                FROM territories t 
                JOIN countries c ON t.country_id = c.id 
                WHERE c.name = ?
                ORDER BY t.name
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, countryName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Territory territory = createTerritoryFromResultSet(rs);
                        if (territory != null) {
                            territories.add(territory);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error loading territories for country: " + countryName, e);
        }
        
        return territories;
    }
    
    /**
     * Load all territories from the database
     */
    public List<Territory> loadAllTerritories() {
        List<Territory> territories = new ArrayList<>();
        
        try (Connection connection = dataManager.getConnection()) {
            String sql = """
                SELECT t.*, c.name as country_name 
                FROM territories t 
                JOIN countries c ON t.country_id = c.id 
                ORDER BY c.name, t.name
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Territory territory = createTerritoryFromResultSet(rs);
                        if (territory != null) {
                            territories.add(territory);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error loading all territories", e);
        }
        
        return territories;
    }
    
    /**
     * Delete a territory from the database
     */
    public void deleteTerritory(String name, String countryName) {
        try (Connection connection = dataManager.getConnection()) {
            String sql = """
                DELETE FROM territories 
                WHERE name = ? AND country_id = (
                    SELECT id FROM countries WHERE name = ?
                )
            """;
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, countryName);
                int deleted = stmt.executeUpdate();
                
                if (deleted > 0) {
                    dataManager.getPlugin().debug("Deleted territory: " + name);
                }
            }
        } catch (SQLException e) {
            dataManager.getPlugin().getLogger().log(Level.SEVERE, 
                    "Error deleting territory: " + name, e);
        }
    }
    
    /**
     * Save all territories (batch operation)
     */
    public void saveAll() {
        dataManager.getPlugin().debug("Territory storage save-all completed");
    }
    
    /**
     * Create a Territory object from database result set
     */
    private Territory createTerritoryFromResultSet(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String countryName = rs.getString("country_name");
        String worldName = rs.getString("world_name");
        String chunksJson = rs.getString("chunks");
        TerritoryType type = TerritoryType.valueOf(rs.getString("territory_type"));
        long claimedDate = rs.getLong("claimed_date");
        String dataJson = rs.getString("data");
        
        // Create territory
        Territory territory = new Territory(name, countryName, worldName, type);
        
        // Load chunks
        if (chunksJson != null && !chunksJson.isEmpty()) {
            try {
                JsonArray chunksArray = dataManager.getGson().fromJson(chunksJson, JsonArray.class);
                for (int i = 0; i < chunksArray.size(); i++) {
                    String chunkStr = chunksArray.get(i).getAsString();
                    ChunkCoordinate chunk = ChunkCoordinate.deserialize(chunkStr);
                    territory.addChunk(chunk);
                }
            } catch (Exception e) {
                dataManager.getPlugin().getLogger().log(Level.WARNING, 
                        "Error parsing chunks for territory: " + name, e);
            }
        }
        
        // Parse additional data
        if (dataJson != null && !dataJson.isEmpty()) {
            try {
                JsonObject data = dataManager.getGson().fromJson(dataJson, JsonObject.class);
                
                if (data.has("description")) {
                    territory.setDescription(data.get("description").getAsString());
                }
                if (data.has("allowPublicAccess")) {
                    territory.setAllowPublicAccess(data.get("allowPublicAccess").getAsBoolean());
                }
                if (data.has("allowBuilding")) {
                    territory.setAllowBuilding(data.get("allowBuilding").getAsBoolean());
                }
                if (data.has("allowPvP")) {
                    territory.setAllowPvP(data.get("allowPvP").getAsBoolean());
                }
                if (data.has("upkeepCost")) {
                    territory.setUpkeepCost(data.get("upkeepCost").getAsDouble());
                }
                
                // Load allowed players
                if (data.has("allowedPlayers")) {
                    JsonArray allowedPlayers = data.getAsJsonArray("allowedPlayers");
                    for (int i = 0; i < allowedPlayers.size(); i++) {
                        UUID playerUUID = UUID.fromString(allowedPlayers.get(i).getAsString());
                        territory.addAllowedPlayer(playerUUID);
                    }
                }
                
                // Load allowed countries
                if (data.has("allowedCountries")) {
                    JsonArray allowedCountries = data.getAsJsonArray("allowedCountries");
                    for (int i = 0; i < allowedCountries.size(); i++) {
                        String countryNameAllowed = allowedCountries.get(i).getAsString();
                        territory.addAllowedCountry(countryNameAllowed);
                    }
                }
                
            } catch (Exception e) {
                dataManager.getPlugin().getLogger().log(Level.WARNING, 
                        "Error parsing territory data for: " + name, e);
            }
        }
        
        return territory;
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
}