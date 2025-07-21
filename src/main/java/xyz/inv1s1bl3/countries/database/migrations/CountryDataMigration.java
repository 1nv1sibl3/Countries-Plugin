package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Country data migration
 */
public final class CountryDataMigration implements Migration {
    
    @Override
    public String getName() {
        return "002_country_data";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Countries table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS countries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    display_name TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    flag_description TEXT DEFAULT '',
                    government_type TEXT DEFAULT 'democracy',
                    leader_uuid TEXT NOT NULL,
                    treasury_balance REAL DEFAULT 0.0,
                    tax_rate REAL DEFAULT 5.0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    capital_world TEXT,
                    capital_x INTEGER,
                    capital_z INTEGER,
                    is_active BOOLEAN DEFAULT TRUE,
                    FOREIGN KEY (leader_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Country members table (for tracking roles and permissions)
            statement.execute("""
                CREATE TABLE IF NOT EXISTS country_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'citizen',
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    salary REAL DEFAULT 0.0,
                    permissions TEXT DEFAULT '[]',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    UNIQUE(country_id, player_uuid)
                )
                """);
            
            // Country invitations table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS country_invitations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    inviter_uuid TEXT NOT NULL,
                    invitee_uuid TEXT NOT NULL,
                    role TEXT DEFAULT 'citizen',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    expires_at DATETIME NOT NULL,
                    status TEXT DEFAULT 'pending',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (inviter_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (invitee_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_countries_name ON countries(name)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_countries_leader ON countries(leader_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_countries_active ON countries(is_active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_country_members_country ON country_members(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_country_members_player ON country_members(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_invitations_country ON country_invitations(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_invitations_invitee ON country_invitations(invitee_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_invitations_status ON country_invitations(status)");
        }
    }
}