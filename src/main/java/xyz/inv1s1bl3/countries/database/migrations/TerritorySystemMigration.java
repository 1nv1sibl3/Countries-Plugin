package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Territory system migration
 */
public final class TerritorySystemMigration implements Migration {
    
    @Override
    public String getName() {
        return "003_territory_system";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Territories table (chunk-based claims)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS territories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    world_name TEXT NOT NULL,
                    chunk_x INTEGER NOT NULL,
                    chunk_z INTEGER NOT NULL,
                    territory_type TEXT DEFAULT 'residential',
                    claimed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    claimed_by_uuid TEXT NOT NULL,
                    claim_cost REAL DEFAULT 0.0,
                    maintenance_cost REAL DEFAULT 0.0,
                    last_maintenance_paid DATETIME DEFAULT CURRENT_TIMESTAMP,
                    protection_flags TEXT DEFAULT '{}',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (claimed_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(world_name, chunk_x, chunk_z)
                )
                """);
            
            // Territory protection settings
            statement.execute("""
                CREATE TABLE IF NOT EXISTS territory_protection (
                    territory_id INTEGER PRIMARY KEY,
                    block_break BOOLEAN DEFAULT TRUE,
                    block_place BOOLEAN DEFAULT TRUE,
                    entity_damage BOOLEAN DEFAULT TRUE,
                    interaction BOOLEAN DEFAULT TRUE,
                    fire_spread BOOLEAN DEFAULT FALSE,
                    explosion_damage BOOLEAN DEFAULT TRUE,
                    mob_spawning BOOLEAN DEFAULT TRUE,
                    pvp_enabled BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (territory_id) REFERENCES territories(id) ON DELETE CASCADE
                )
                """);
            
            // Territory permissions (who can do what in specific territories)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS territory_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    territory_id INTEGER NOT NULL,
                    player_uuid TEXT,
                    role TEXT,
                    permission_type TEXT NOT NULL,
                    granted BOOLEAN DEFAULT TRUE,
                    granted_by_uuid TEXT NOT NULL,
                    granted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (territory_id) REFERENCES territories(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (granted_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territories_country ON territories(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territories_world ON territories(world_name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territories_chunk ON territories(world_name, chunk_x, chunk_z)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territories_type ON territories(territory_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territory_permissions_territory ON territory_permissions(territory_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_territory_permissions_player ON territory_permissions(player_uuid)");
        }
    }
}