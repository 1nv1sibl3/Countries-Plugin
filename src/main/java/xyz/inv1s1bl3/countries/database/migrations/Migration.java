package xyz.inv1s1bl3.countries.database.migrations;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for database migrations
 */
public interface Migration {
    
    /**
     * Get the migration name
     * @return Migration name
     */
    String getName();
    
    /**
     * Apply the migration
     * @param connection Database connection
     * @throws SQLException if migration fails
     */
    void up(Connection connection) throws SQLException;
    
    /**
     * Rollback the migration (optional)
     * @param connection Database connection
     * @throws SQLException if rollback fails
     */
    default void down(Connection connection) throws SQLException {
        throw new UnsupportedOperationException("Migration rollback not implemented");
    }
}