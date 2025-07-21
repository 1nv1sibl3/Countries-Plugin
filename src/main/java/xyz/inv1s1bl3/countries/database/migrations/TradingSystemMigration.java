package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Trading system migration
 */
public final class TradingSystemMigration implements Migration {
    
    @Override
    public String getName() {
        return "007_trading_system";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Trade sessions table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS trade_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_uuid TEXT NOT NULL UNIQUE,
                    trader1_uuid TEXT NOT NULL,
                    trader2_uuid TEXT NOT NULL,
                    trader1_ready BOOLEAN DEFAULT FALSE,
                    trader2_ready BOOLEAN DEFAULT FALSE,
                    trader1_confirmed BOOLEAN DEFAULT FALSE,
                    trader2_confirmed BOOLEAN DEFAULT FALSE,
                    status TEXT DEFAULT 'active',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    completed_at DATETIME,
                    cancelled_at DATETIME,
                    cancelled_by_uuid TEXT,
                    world_name TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    FOREIGN KEY (trader1_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (trader2_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (cancelled_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Trade items table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS trade_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id INTEGER NOT NULL,
                    owner_uuid TEXT NOT NULL,
                    slot_index INTEGER NOT NULL,
                    item_data TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    added_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (session_id) REFERENCES trade_sessions(id) ON DELETE CASCADE,
                    FOREIGN KEY (owner_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Trade history table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS trade_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_uuid TEXT NOT NULL,
                    trader1_uuid TEXT NOT NULL,
                    trader2_uuid TEXT NOT NULL,
                    trader1_items TEXT NOT NULL DEFAULT '[]',
                    trader2_items TEXT NOT NULL DEFAULT '[]',
                    money_exchanged REAL DEFAULT 0.0,
                    money_from_uuid TEXT,
                    money_to_uuid TEXT,
                    completed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    world_name TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    FOREIGN KEY (trader1_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (trader2_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (money_from_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (money_to_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Country trade agreements table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS country_trade_agreements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country1_id INTEGER NOT NULL,
                    country2_id INTEGER NOT NULL,
                    agreement_name TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    trade_bonus_percentage REAL DEFAULT 0.0,
                    tax_reduction_percentage REAL DEFAULT 0.0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_by_uuid TEXT NOT NULL,
                    expires_at DATETIME,
                    status TEXT DEFAULT 'active',
                    terms TEXT DEFAULT '{}',
                    FOREIGN KEY (country1_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (country2_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(country1_id, country2_id, agreement_name),
                    CHECK(country1_id != country2_id)
                )
                """);
            
            // Trade requests table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS trade_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    requester_uuid TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    message TEXT DEFAULT '',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    expires_at DATETIME NOT NULL,
                    status TEXT DEFAULT 'pending',
                    responded_at DATETIME,
                    FOREIGN KEY (requester_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (target_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_sessions_trader1 ON trade_sessions(trader1_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_sessions_trader2 ON trade_sessions(trader2_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_sessions_status ON trade_sessions(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_items_session ON trade_items(session_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_items_owner ON trade_items(owner_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_history_trader1 ON trade_history(trader1_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_history_trader2 ON trade_history(trader2_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_country_trade_agreements_country1 ON country_trade_agreements(country1_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_country_trade_agreements_country2 ON country_trade_agreements(country2_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_country_trade_agreements_status ON country_trade_agreements(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_requests_requester ON trade_requests(requester_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_requests_target ON trade_requests(target_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_trade_requests_status ON trade_requests(status)");
        }
    }
}