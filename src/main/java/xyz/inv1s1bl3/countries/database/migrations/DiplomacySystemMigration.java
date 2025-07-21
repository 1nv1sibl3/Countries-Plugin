package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Diplomacy system migration
 */
public final class DiplomacySystemMigration implements Migration {
    
    @Override
    public String getName() {
        return "005_diplomacy_system";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Diplomatic relations table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS diplomatic_relations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country1_id INTEGER NOT NULL,
                    country2_id INTEGER NOT NULL,
                    relation_type TEXT NOT NULL DEFAULT 'neutral',
                    established_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    established_by_uuid TEXT NOT NULL,
                    last_changed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    last_changed_by_uuid TEXT NOT NULL,
                    notes TEXT DEFAULT '',
                    FOREIGN KEY (country1_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (country2_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (established_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (last_changed_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(country1_id, country2_id),
                    CHECK(country1_id != country2_id)
                )
                """);
            
            // Wars table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS wars (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    aggressor_country_id INTEGER NOT NULL,
                    defender_country_id INTEGER NOT NULL,
                    declared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    declared_by_uuid TEXT NOT NULL,
                    ended_at DATETIME,
                    ended_by_uuid TEXT,
                    war_reason TEXT DEFAULT '',
                    status TEXT DEFAULT 'active',
                    winner_country_id INTEGER,
                    FOREIGN KEY (aggressor_country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (defender_country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (declared_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (ended_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (winner_country_id) REFERENCES countries(id) ON DELETE SET NULL,
                    CHECK(aggressor_country_id != defender_country_id)
                )
                """);
            
            // Alliances table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS alliances (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT DEFAULT '',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_by_uuid TEXT NOT NULL,
                    leader_country_id INTEGER NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    alliance_type TEXT DEFAULT 'defensive',
                    FOREIGN KEY (created_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (leader_country_id) REFERENCES countries(id) ON DELETE CASCADE
                )
                """);
            
            // Alliance members table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS alliance_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    alliance_id INTEGER NOT NULL,
                    country_id INTEGER NOT NULL,
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    invited_by_uuid TEXT NOT NULL,
                    role TEXT DEFAULT 'member',
                    contribution REAL DEFAULT 0.0,
                    FOREIGN KEY (alliance_id) REFERENCES alliances(id) ON DELETE CASCADE,
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (invited_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(alliance_id, country_id)
                )
                """);
            
            // Treaties table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS treaties (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    treaty_type TEXT NOT NULL,
                    country1_id INTEGER NOT NULL,
                    country2_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    signed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    signed_by1_uuid TEXT NOT NULL,
                    signed_by2_uuid TEXT NOT NULL,
                    expires_at DATETIME,
                    status TEXT DEFAULT 'active',
                    FOREIGN KEY (country1_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (country2_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (signed_by1_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (signed_by2_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    CHECK(country1_id != country2_id)
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_diplomatic_relations_country1 ON diplomatic_relations(country1_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_diplomatic_relations_country2 ON diplomatic_relations(country2_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_diplomatic_relations_type ON diplomatic_relations(relation_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_wars_aggressor ON wars(aggressor_country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_wars_defender ON wars(defender_country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_wars_status ON wars(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_alliances_leader ON alliances(leader_country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_alliances_active ON alliances(is_active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_alliance_members_alliance ON alliance_members(alliance_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_alliance_members_country ON alliance_members(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_treaties_country1 ON treaties(country1_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_treaties_country2 ON treaties(country2_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_treaties_type ON treaties(treaty_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_treaties_status ON treaties(status)");
        }
    }
}