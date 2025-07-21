package xyz.inv1s1bl3.countries.database.repositories;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Base repository interface for database operations
 */
public interface Repository<T, ID> {
    
    /**
     * Save an entity
     * @param entity Entity to save
     * @return Saved entity with generated ID
     * @throws SQLException if save fails
     */
    T save(T entity) throws SQLException;
    
    /**
     * Update an entity
     * @param entity Entity to update
     * @return Updated entity
     * @throws SQLException if update fails
     */
    T update(T entity) throws SQLException;
    
    /**
     * Find entity by ID
     * @param id Entity ID
     * @return Optional containing entity if found
     * @throws SQLException if query fails
     */
    Optional<T> findById(ID id) throws SQLException;
    
    /**
     * Find all entities
     * @return List of all entities
     * @throws SQLException if query fails
     */
    List<T> findAll() throws SQLException;
    
    /**
     * Delete entity by ID
     * @param id Entity ID
     * @return true if deleted, false if not found
     * @throws SQLException if delete fails
     */
    boolean deleteById(ID id) throws SQLException;
    
    /**
     * Check if entity exists by ID
     * @param id Entity ID
     * @return true if exists
     * @throws SQLException if query fails
     */
    boolean existsById(ID id) throws SQLException;
    
    /**
     * Count all entities
     * @return Total count
     * @throws SQLException if query fails
     */
    long count() throws SQLException;
}