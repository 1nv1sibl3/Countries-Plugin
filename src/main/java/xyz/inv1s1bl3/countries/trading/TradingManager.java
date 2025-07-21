package xyz.inv1s1bl3.countries.trading;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import xyz.inv1s1bl3.countries.CountriesPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manager for trading-related operations
 */
@Getter
public final class TradingManager {
    
    private final CountriesPlugin plugin;
    private final TradingService tradingService;
    
    // Cleanup task
    private BukkitTask cleanupTask;
    
    public TradingManager(final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.tradingService = new TradingService(plugin);
    }
    
    /**
     * Initialize the trading manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing Trading Manager...");
        
        // Start cleanup task for expired sessions
        this.startCleanupTask();
        
        this.plugin.getLogger().info("Trading Manager initialized successfully!");
    }
    
    /**
     * Start cleanup task for expired trade sessions
     */
    private void startCleanupTask() {
        final int cleanupInterval = 20 * 60; // 1 minute in ticks
        
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            this.plugin,
            () -> {
                try {
                    this.tradingService.cleanupExpiredSessions();
                } catch (final Exception exception) {
                    this.plugin.getLogger().log(Level.WARNING, "Error during trade session cleanup", exception);
                }
            },
            cleanupInterval,
            cleanupInterval
        );
        
        this.plugin.getLogger().info("Trade session cleanup task started");
    }
    
    /**
     * Create a new trade session
     * @param requesterUuid Requester UUID
     * @param targetUuid Target UUID
     * @return Trade session ID
     */
    public String createTradeSession(final UUID requesterUuid, final UUID targetUuid) {
        return this.tradingService.createTradeSession(requesterUuid, targetUuid);
    }
    
    /**
     * Get player's active trade session
     * @param playerUuid Player UUID
     * @return Trade session if active
     */
    public Optional<TradeSession> getPlayerTradeSession(final UUID playerUuid) {
        return this.tradingService.getPlayerTradeSession(playerUuid);
    }
    
    /**
     * Cancel trade for a player
     * @param playerUuid Player UUID
     */
    public void cancelTrade(final UUID playerUuid) {
        this.tradingService.cancelTrade(playerUuid);
    }
    
    /**
     * Set player ready status
     * @param playerUuid Player UUID
     * @param ready Ready status
     */
    public void setPlayerReady(final UUID playerUuid, final boolean ready) {
        this.tradingService.setPlayerReady(playerUuid, ready);
    }
    
    /**
     * Confirm trade for a player
     * @param playerUuid Player UUID
     */
    public void confirmTrade(final UUID playerUuid) {
        this.tradingService.confirmTrade(playerUuid);
    }
    
    /**
     * Get number of active trade sessions
     * @return Active session count
     */
    public int getActiveSessionCount() {
        return this.tradingService.getActiveSessionCount();
    }
    
    /**
     * Shutdown trading manager
     */
    public void shutdown() {
        this.plugin.getLogger().info("Shutting down Trading Manager...");
        
        // Cancel cleanup task
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        
        // Force cleanup all sessions
        this.tradingService.forceCleanupAllSessions();
        
        this.plugin.getLogger().info("Trading Manager shut down successfully!");
    }
}