package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Legal system migration
 */
public final class LegalSystemMigration implements Migration {
    
    @Override
    public String getName() {
        return "006_legal_system";
    }
    
    @Override
    public void up(final Connection connection) throws SQLException {
        try (final Statement statement = connection.createStatement()) {
            
            // Laws table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS laws (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    penalty_type TEXT NOT NULL DEFAULT 'fine',
                    penalty_amount REAL DEFAULT 0.0,
                    jail_time INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    created_by_uuid TEXT NOT NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    severity_level INTEGER DEFAULT 1,
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (created_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(country_id, name)
                )
                """);
            
            // Crimes table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS crimes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    law_id INTEGER,
                    criminal_uuid TEXT NOT NULL,
                    crime_type TEXT NOT NULL,
                    description TEXT DEFAULT '',
                    committed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    reported_by_uuid TEXT,
                    world_name TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    evidence TEXT DEFAULT '{}',
                    status TEXT DEFAULT 'reported',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (law_id) REFERENCES laws(id) ON DELETE SET NULL,
                    FOREIGN KEY (criminal_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (reported_by_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Arrests table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS arrests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crime_id INTEGER NOT NULL,
                    arrested_uuid TEXT NOT NULL,
                    arresting_officer_uuid TEXT NOT NULL,
                    arrested_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    jail_time_minutes INTEGER DEFAULT 0,
                    release_time DATETIME,
                    status TEXT DEFAULT 'active',
                    bail_amount REAL DEFAULT 0.0,
                    FOREIGN KEY (crime_id) REFERENCES crimes(id) ON DELETE CASCADE,
                    FOREIGN KEY (arrested_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (arresting_officer_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Fines table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS fines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    crime_id INTEGER NOT NULL,
                    fined_uuid TEXT NOT NULL,
                    issuing_officer_uuid TEXT NOT NULL,
                    amount REAL NOT NULL,
                    issued_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    due_date DATETIME NOT NULL,
                    paid_at DATETIME,
                    status TEXT DEFAULT 'unpaid',
                    FOREIGN KEY (crime_id) REFERENCES crimes(id) ON DELETE CASCADE,
                    FOREIGN KEY (fined_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (issuing_officer_uuid) REFERENCES players(uuid) ON DELETE SET NULL
                )
                """);
            
            // Court cases table
            statement.execute("""
                CREATE TABLE IF NOT EXISTS court_cases (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    country_id INTEGER NOT NULL,
                    case_number TEXT NOT NULL,
                    case_type TEXT NOT NULL DEFAULT 'criminal',
                    plaintiff_uuid TEXT,
                    defendant_uuid TEXT NOT NULL,
                    judge_uuid TEXT,
                    description TEXT NOT NULL,
                    filed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    scheduled_at DATETIME,
                    verdict TEXT,
                    verdict_at DATETIME,
                    status TEXT DEFAULT 'filed',
                    FOREIGN KEY (country_id) REFERENCES countries(id) ON DELETE CASCADE,
                    FOREIGN KEY (plaintiff_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    FOREIGN KEY (defendant_uuid) REFERENCES players(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (judge_uuid) REFERENCES players(uuid) ON DELETE SET NULL,
                    UNIQUE(country_id, case_number)
                )
                """);
            
            // Create indexes
            statement.execute("CREATE INDEX IF NOT EXISTS idx_laws_country ON laws(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_laws_active ON laws(is_active)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_crimes_country ON crimes(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_crimes_criminal ON crimes(criminal_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_crimes_law ON crimes(law_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_crimes_status ON crimes(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_arrests_crime ON arrests(crime_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_arrests_arrested ON arrests(arrested_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_arrests_status ON arrests(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_fines_crime ON fines(crime_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_fines_fined ON fines(fined_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_fines_status ON fines(status)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_court_cases_country ON court_cases(country_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_court_cases_defendant ON court_cases(defendant_uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_court_cases_status ON court_cases(status)");
        }
    }
}