package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Economy system migration
 */
public final class EconomySystemMigration implements Migration {
    
    @Override
    public String getName() {
        return "004_economy_system";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Transactions table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_type TEXT NOT NULL,
                    from_player_uuid TEXT,
                    to_player_uuid TEXT,
                    from_country_id INTEGER,
                    to_country_id INTEGER,
                    amount REAL NOT NULL,
                    description TEXT DEFAULT '',
                    category TEXT DEFAULT 'general',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    world_name TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    FOREIGN KEY (from_player_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (to_player_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (from_country_id) REFERENCES countries(id) ON DELETE SET NULL,
                    FOREIGN KEY (to_country_id) REFERENCES countries(id) ON DELETE SET NULL
                )
                """);
            
            // Tax records table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS tax_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    amount REAL NOT NULL,
                    tax_rate REAL NOT NULL,
                    period_start DATE NOT NULL,
                    period_end DATE NOT NULL,
                    collected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    status TEXT DEFAULT 'collected',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Salary payments table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS salary_payments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL,
                    role TEXT NOT NULL,
                    amount REAL NOT NULL,
                    period_start DATE NOT NULL,
                    period_end DATE NOT NULL,
                    paid_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    status TEXT DEFAULT 'paid',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Economic statistics table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS economic_stats (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER,
                    player_uuid TEXT,
                    stat_type TEXT NOT NULL,
                    stat_value REAL NOT NULL,
                    period_type TEXT NOT NULL,
                    period_start DATE NOT NULL,
                    period_end DATE NOT NULL,
                    recorded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_from_player ON transactions(from_player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_to_player ON transactions(to_player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_from_country ON transactions(from_country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_to_country ON transactions(to_country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(created_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_tax_records_country ON tax_records(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_tax_records_player ON tax_records(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_salary_payments_country ON salary_payments(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_salary_payments_player ON salary_payments(player_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_economic_stats_country ON economic_stats(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_economic_stats_player ON economic_stats(player_uuid)");
        }
    }
}