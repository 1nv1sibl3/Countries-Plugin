package xyz.inv1s1bl3.countries.trading;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.database.entities.Transaction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Service class for trading business logic
 */
@RequiredArgsConstructor
public final class TradingService {
    
    private final CountriesPlugin plugin;
    
    // Active trade sessions
    private final Map<String, TradeSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerSessions = new ConcurrentHashMap<>();
    
    /**
     * Create a new trade session between two players
     * @param requesterUuid Requester UUID
     * @param targetUuid Target UUID
     * @return Trade session ID
     * @throws IllegalArgumentException if trade cannot be created
     */
    public String createTradeSession(final UUID requesterUuid, final UUID targetUuid) {
        // Validate players
        final Player requester = Bukkit.getPlayer(requesterUuid);
        final Player target = Bukkit.getPlayer(targetUuid);
        
        if (requester == null || target == null) {
            throw new IllegalArgumentException("Both players must be online to trade");
        }
        
        if (requesterUuid.equals(targetUuid)) {
            throw new IllegalArgumentException("Cannot trade with yourself");
        }
        
        // Check if players are already in trades
        if (this.playerSessions.containsKey(requesterUuid)) {
            throw new IllegalArgumentException("You are already in a trade");
        }
        
        if (this.playerSessions.containsKey(targetUuid)) {
            throw new IllegalArgumentException("Target player is already in a trade");
        }
        
        // Check permissions
        if (!requester.hasPermission("countries.trade.initiate")) {
            throw new IllegalArgumentException("You don't have permission to initiate trades");
        }
        
        if (!target.hasPermission("countries.trade.participate")) {
            throw new IllegalArgumentException("Target player cannot participate in trades");
        }
        
        // Create session
        final String sessionId = UUID.randomUUID().toString();
        final TradeSession session = TradeSession.builder()
                .sessionId(sessionId)
                .trader1Uuid(requesterUuid)
                .trader2Uuid(targetUuid)
                .build();
        
        session.initialize();
        
        // Store session
        this.activeSessions.put(sessionId, session);
        this.playerSessions.put(requesterUuid, sessionId);
        this.playerSessions.put(targetUuid, sessionId);
        
        this.plugin.getLogger().info("Trade session created between " + requester.getName() + " and " + target.getName());
        
        return sessionId;
    }
    
    /**
     * Get active trade session for a player
     * @param playerUuid Player UUID
     * @return Trade session if active
     */
    public Optional<TradeSession> getPlayerTradeSession(final UUID playerUuid) {
        final String sessionId = this.playerSessions.get(playerUuid);
        if (sessionId != null) {
            final TradeSession session = this.activeSessions.get(sessionId);
            if (session != null && session.isActive()) {
                return Optional.of(session);
            } else {
                // Clean up expired session
                this.cleanupSession(sessionId);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Add item to trade
     * @param playerUuid Player UUID
     * @param slot Slot number
     * @param item Item to add
     * @throws IllegalArgumentException if operation fails
     */
    public void addItemToTrade(final UUID playerUuid, final int slot, final ItemStack item) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        final TradeSession session = sessionOpt.get();
        final Map<Integer, ItemStack> playerItems = session.getPlayerItems(playerUuid);
        
        // Validate slot
        if (slot < 0 || slot >= 27) { // 27 slots in trade GUI
            throw new IllegalArgumentException("Invalid slot number");
        }
        
        // Add item
        playerItems.put(slot, item.clone());
        
        // Reset ready status when items change
        session.resetReadyStatus();
        
        // Notify other player
        this.notifyTradeUpdate(session, playerUuid);
    }
    
    /**
     * Remove item from trade
     * @param playerUuid Player UUID
     * @param slot Slot number
     * @throws IllegalArgumentException if operation fails
     */
    public void removeItemFromTrade(final UUID playerUuid, final int slot) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        final TradeSession session = sessionOpt.get();
        final Map<Integer, ItemStack> playerItems = session.getPlayerItems(playerUuid);
        
        // Remove item
        playerItems.remove(slot);
        
        // Reset ready status when items change
        session.resetReadyStatus();
        
        // Notify other player
        this.notifyTradeUpdate(session, playerUuid);
    }
    
    /**
     * Set money offer in trade
     * @param playerUuid Player UUID
     * @param amount Money amount
     * @throws IllegalArgumentException if operation fails
     */
    public void setMoneyOffer(final UUID playerUuid, final double amount) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }
        
        // Check if player has enough money
        if (amount > 0) {
            final double balance = this.plugin.getEconomyManager().getPlayerBalance(playerUuid);
            if (balance < amount) {
                throw new IllegalArgumentException("Insufficient funds");
            }
        }
        
        final TradeSession session = sessionOpt.get();
        session.setPlayerMoney(playerUuid, amount);
        
        // Reset ready status when money changes
        session.resetReadyStatus();
        
        // Notify other player
        this.notifyTradeUpdate(session, playerUuid);
    }
    
    /**
     * Set player ready status
     * @param playerUuid Player UUID
     * @param ready Ready status
     * @throws IllegalArgumentException if operation fails
     */
    public void setPlayerReady(final UUID playerUuid, final boolean ready) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        final TradeSession session = sessionOpt.get();
        session.setPlayerReady(playerUuid, ready);
        
        // Reset confirmation when ready status changes
        session.setPlayerConfirmed(playerUuid, false);
        
        // Notify other player
        this.notifyTradeUpdate(session, playerUuid);
    }
    
    /**
     * Confirm trade
     * @param playerUuid Player UUID
     * @throws IllegalArgumentException if operation fails
     */
    public void confirmTrade(final UUID playerUuid) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        final TradeSession session = sessionOpt.get();
        
        if (!session.areBothReady()) {
            throw new IllegalArgumentException("Both players must be ready before confirming");
        }
        
        session.setPlayerConfirmed(playerUuid, true);
        
        // Check if both players have confirmed
        if (session.areBothConfirmed()) {
            this.executeTrade(session);
        } else {
            // Notify other player
            this.notifyTradeUpdate(session, playerUuid);
        }
    }
    
    /**
     * Cancel trade session
     * @param playerUuid Player UUID
     * @throws IllegalArgumentException if operation fails
     */
    public void cancelTrade(final UUID playerUuid) {
        final Optional<TradeSession> sessionOpt = this.getPlayerTradeSession(playerUuid);
        if (sessionOpt.isEmpty()) {
            throw new IllegalArgumentException("You are not in a trade session");
        }
        
        final TradeSession session = sessionOpt.get();
        session.cancel();
        
        // Notify both players
        final Player[] traders = session.getOnlineTraders();
        if (traders != null) {
            for (final Player trader : traders) {
                xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(trader, "trading.trade-cancelled");
            }
        }
        
        // Clean up session
        this.cleanupSession(session.getSessionId());
    }
    
    /**
     * Execute the trade
     * @param session Trade session
     */
    private void executeTrade(final TradeSession session) {
        try {
            final Player[] traders = session.getOnlineTraders();
            if (traders == null) {
                throw new IllegalStateException("One or both traders are offline");
            }
            
            final Player trader1 = traders[0];
            final Player trader2 = traders[1];
            
            // Validate inventories and balances
            if (!this.validateTradeExecution(session)) {
                throw new IllegalStateException("Trade validation failed");
            }
            
            // Execute money transfers
            if (session.getTrader1Money() > 0) {
                this.plugin.getEconomyManager().transferPlayerToPlayer(
                    session.getTrader1Uuid(), session.getTrader2Uuid(), 
                    session.getTrader1Money(), "Trade with " + trader2.getName()
                );
            }
            
            if (session.getTrader2Money() > 0) {
                this.plugin.getEconomyManager().transferPlayerToPlayer(
                    session.getTrader2Uuid(), session.getTrader1Uuid(), 
                    session.getTrader2Money(), "Trade with " + trader1.getName()
                );
            }
            
            // Execute item transfers
            this.transferItems(trader1, trader2, session.getTrader1Items(), session.getTrader2Items());
            this.transferItems(trader2, trader1, session.getTrader2Items(), session.getTrader1Items());
            
            // Record trade completion
            this.recordTradeCompletion(session);
            
            // Mark session as completed
            session.complete();
            
            // Notify players
            xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(trader1, "trading.trade-completed");
            xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(trader2, "trading.trade-completed");
            
            this.plugin.getLogger().info("Trade completed between " + trader1.getName() + " and " + trader2.getName());
            
        } catch (final Exception exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Error executing trade", exception);
            
            // Notify players of failure
            final Player[] traders = session.getOnlineTraders();
            if (traders != null) {
                for (final Player trader : traders) {
                    xyz.inv1s1bl3.countries.utils.MessageUtil.sendError(trader, "Trade failed: " + exception.getMessage());
                }
            }
            
            session.cancel();
        } finally {
            // Clean up session
            this.cleanupSession(session.getSessionId());
        }
    }
    
    /**
     * Validate trade execution
     * @param session Trade session
     * @return true if valid
     */
    private boolean validateTradeExecution(final TradeSession session) {
        final Player[] traders = session.getOnlineTraders();
        if (traders == null) {
            return false;
        }
        
        final Player trader1 = traders[0];
        final Player trader2 = traders[1];
        
        // Check money balances
        if (session.getTrader1Money() > 0) {
            final double balance1 = this.plugin.getEconomyManager().getPlayerBalance(session.getTrader1Uuid());
            if (balance1 < session.getTrader1Money()) {
                return false;
            }
        }
        
        if (session.getTrader2Money() > 0) {
            final double balance2 = this.plugin.getEconomyManager().getPlayerBalance(session.getTrader2Uuid());
            if (balance2 < session.getTrader2Money()) {
                return false;
            }
        }
        
        // Check inventory space
        if (!this.hasInventorySpace(trader1, session.getTrader2Items())) {
            return false;
        }
        
        if (!this.hasInventorySpace(trader2, session.getTrader1Items())) {
            return false;
        }
        
        // Verify items still exist in inventories
        return this.verifyItemsExist(trader1, session.getTrader1Items()) &&
               this.verifyItemsExist(trader2, session.getTrader2Items());
    }
    
    /**
     * Check if player has inventory space for items
     * @param player Player
     * @param items Items to check
     * @return true if has space
     */
    private boolean hasInventorySpace(final Player player, final Map<Integer, ItemStack> items) {
        int requiredSlots = 0;
        for (final ItemStack item : items.values()) {
            if (item != null) {
                requiredSlots++;
            }
        }
        
        int emptySlots = 0;
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                emptySlots++;
            }
        }
        
        return emptySlots >= requiredSlots;
    }
    
    /**
     * Verify items exist in player's inventory
     * @param player Player
     * @param items Items to verify
     * @return true if all items exist
     */
    private boolean verifyItemsExist(final Player player, final Map<Integer, ItemStack> items) {
        for (final ItemStack item : items.values()) {
            if (item != null && !player.getInventory().containsAtLeast(item, item.getAmount())) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Transfer items between players
     * @param from Source player
     * @param to Target player
     * @param fromItems Items to remove from source
     * @param toItems Items to give to target
     */
    private void transferItems(final Player from, final Player to, 
                              final Map<Integer, ItemStack> fromItems, 
                              final Map<Integer, ItemStack> toItems) {
        // Remove items from source player
        for (final ItemStack item : fromItems.values()) {
            if (item != null) {
                from.getInventory().removeItem(item);
            }
        }
        
        // Add items to target player
        for (final ItemStack item : toItems.values()) {
            if (item != null) {
                to.getInventory().addItem(item);
            }
        }
    }
    
    /**
     * Record trade completion
     * @param session Trade session
     */
    private void recordTradeCompletion(final TradeSession session) {
        // Record transaction for money exchange
        if (session.getTrader1Money() > 0 || session.getTrader2Money() > 0) {
            final double netAmount = Math.abs(session.getTrader1Money() - session.getTrader2Money());
            final UUID fromUuid = session.getTrader1Money() > session.getTrader2Money() ? 
                session.getTrader1Uuid() : session.getTrader2Uuid();
            final UUID toUuid = session.getTrader1Money() > session.getTrader2Money() ? 
                session.getTrader2Uuid() : session.getTrader1Uuid();
            
            this.plugin.getEconomyManager().getTransactionManager().recordPlayerToPlayerTransaction(
                fromUuid, toUuid, netAmount, "Trade transaction", "trade", null
            );
        }
        
        // TODO: Record item trade in database when trade history system is implemented
    }
    
    /**
     * Notify trade update to other player
     * @param session Trade session
     * @param updatingPlayerUuid Player who made the update
     */
    private void notifyTradeUpdate(final TradeSession session, final UUID updatingPlayerUuid) {
        final UUID otherPlayerUuid = session.getOtherTrader(updatingPlayerUuid);
        final Player otherPlayer = Bukkit.getPlayer(otherPlayerUuid);
        
        if (otherPlayer != null) {
            // TODO: Update trade GUI when GUI system is implemented
            // For now, just send a message
            final Player updatingPlayer = Bukkit.getPlayer(updatingPlayerUuid);
            if (updatingPlayer != null) {
                xyz.inv1s1bl3.countries.utils.MessageUtil.sendInfo(otherPlayer, 
                    updatingPlayer.getName() + " updated their trade offer");
            }
        }
    }
    
    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        final var iterator = this.activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final TradeSession session = entry.getValue();
            
            if (session.isExpired()) {
                // Notify players
                final Player[] traders = session.getOnlineTraders();
                if (traders != null) {
                    for (final Player trader : traders) {
                        xyz.inv1s1bl3.countries.utils.MessageUtil.sendMessage(trader, "trading.trade-timeout", 
                            java.util.Map.of("minutes", "10"));
                    }
                }
                
                // Clean up
                this.cleanupSession(entry.getKey());
                iterator.remove();
            }
        }
    }
    
    /**
     * Clean up a specific session
     * @param sessionId Session ID
     */
    private void cleanupSession(final String sessionId) {
        final TradeSession session = this.activeSessions.remove(sessionId);
        if (session != null) {
            this.playerSessions.remove(session.getTrader1Uuid());
            this.playerSessions.remove(session.getTrader2Uuid());
        }
    }
    
    /**
     * Get all active sessions count
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return this.activeSessions.size();
    }
    
    /**
     * Force cleanup all sessions (for plugin shutdown)
     */
    public void forceCleanupAllSessions() {
        this.activeSessions.clear();
        this.playerSessions.clear();
    }
}