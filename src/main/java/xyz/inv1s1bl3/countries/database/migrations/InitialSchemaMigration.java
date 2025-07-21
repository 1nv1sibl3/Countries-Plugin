package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initial database schema migration
 */
public final class InitialSchemaMigration implements Migration {
    
    @Override
    public String getName() {
        return "001_initial_schema";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Enable foreign key constraints
            statement.execute("PRAGMA foreign_keys = ON");
            
            // Players table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    username TEXT NOT NULL,
                    country_id INTEGER,
                    role TEXT DEFAULT 'citizen',
                    balance REAL DEFAULT 0.0,
                    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_online BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE SET NULL
                )
                """);
            
            // Create indexes for players table
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_username ON players(username)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_country ON players(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_online ON players(is_online)");
        }
    }
}