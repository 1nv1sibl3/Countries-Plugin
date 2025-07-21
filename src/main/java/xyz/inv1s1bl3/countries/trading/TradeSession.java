package xyz.inv1s1bl3.countries.trading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an active trading session between two players
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class TradeSession {
    
    private String sessionId;
    private UUID trader1Uuid;
    private UUID trader2Uuid;
    private Map<Integer, ItemStack> trader1Items;
    private Map<Integer, ItemStack> trader2Items;
    private double trader1Money;
    private double trader2Money;
    private boolean trader1Ready;
    private boolean trader2Ready;
    private boolean trader1Confirmed;
    private boolean trader2Confirmed;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status;
    
    /**
     * Initialize a new trade session
     */
    public void initialize() {
        this.trader1Items = new HashMap<>();
        this.trader2Items = new HashMap<>();
        this.trader1Money = 0.0;
        this.trader2Money = 0.0;
        this.trader1Ready = false;
        this.trader2Ready = false;
        this.trader1Confirmed = false;
        this.trader2Confirmed = false;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(10); // 10 minute timeout
        this.status = "active";
    }
    
    /**
     * Check if session has expired
     * @return true if expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
    
    /**
     * Check if session is active
     * @return true if active
     */
    public boolean isActive() {
        return "active".equals(this.status) && !this.isExpired();
    }
    
    /**
     * Check if both traders are ready
     * @return true if both ready
     */
    public boolean areBothReady() {
        return this.trader1Ready && this.trader2Ready;
    }
    
    /**
     * Check if both traders have confirmed
     * @return true if both confirmed
     */
    public boolean areBothConfirmed() {
        return this.trader1Confirmed && this.trader2Confirmed;
    }
    
    /**
     * Get the other trader's UUID
     * @param playerUuid One trader's UUID
     * @return Other trader's UUID
     */
    public UUID getOtherTrader(final UUID playerUuid) {
        if (this.trader1Uuid.equals(playerUuid)) {
            return this.trader2Uuid;
        } else if (this.trader2Uuid.equals(playerUuid)) {
            return this.trader1Uuid;
        }
        return null;
    }
    
    /**
     * Check if player is trader 1
     * @param playerUuid Player UUID
     * @return true if trader 1
     */
    public boolean isTrader1(final UUID playerUuid) {
        return this.trader1Uuid.equals(playerUuid);
    }
    
    /**
     * Check if player is trader 2
     * @param playerUuid Player UUID
     * @return true if trader 2
     */
    public boolean isTrader2(final UUID playerUuid) {
        return this.trader2Uuid.equals(playerUuid);
    }
    
    /**
     * Get player's items
     * @param playerUuid Player UUID
     * @return Player's items map
     */
    public Map<Integer, ItemStack> getPlayerItems(final UUID playerUuid) {
        if (this.isTrader1(playerUuid)) {
            return this.trader1Items;
        } else if (this.isTrader2(playerUuid)) {
            return this.trader2Items;
        }
        return new HashMap<>();
    }
    
    /**
     * Get other player's items
     * @param playerUuid Player UUID
     * @return Other player's items map
     */
    public Map<Integer, ItemStack> getOtherPlayerItems(final UUID playerUuid) {
        if (this.isTrader1(playerUuid)) {
            return this.trader2Items;
        } else if (this.isTrader2(playerUuid)) {
            return this.trader1Items;
        }
        return new HashMap<>();
    }
    
    /**
     * Get player's money offer
     * @param playerUuid Player UUID
     * @return Money amount
     */
    public double getPlayerMoney(final UUID playerUuid) {
        if (this.isTrader1(playerUuid)) {
            return this.trader1Money;
        } else if (this.isTrader2(playerUuid)) {
            return this.trader2Money;
        }
        return 0.0;
    }
    
    /**
     * Set player's money offer
     * @param playerUuid Player UUID
     * @param amount Money amount
     */
    public void setPlayerMoney(final UUID playerUuid, final double amount) {
        if (this.isTrader1(playerUuid)) {
            this.trader1Money = amount;
        } else if (this.isTrader2(playerUuid)) {
            this.trader2Money = amount;
        }
    }
    
    /**
     * Check if player is ready
     * @param playerUuid Player UUID
     * @return true if ready
     */
    public boolean isPlayerReady(final UUID playerUuid) {
        if (this.isTrader1(playerUuid)) {
            return this.trader1Ready;
        } else if (this.isTrader2(playerUuid)) {
            return this.trader2Ready;
        }
        return false;
    }
    
    /**
     * Set player ready status
     * @param playerUuid Player UUID
     * @param ready Ready status
     */
    public void setPlayerReady(final UUID playerUuid, final boolean ready) {
        if (this.isTrader1(playerUuid)) {
            this.trader1Ready = ready;
        } else if (this.isTrader2(playerUuid)) {
            this.trader2Ready = ready;
        }
    }
    
    /**
     * Check if player has confirmed
     * @param playerUuid Player UUID
     * @return true if confirmed
     */
    public boolean isPlayerConfirmed(final UUID playerUuid) {
        if (this.isTrader1(playerUuid)) {
            return this.trader1Confirmed;
        } else if (this.isTrader2(playerUuid)) {
            return this.trader2Confirmed;
        }
        return false;
    }
    
    /**
     * Set player confirmation status
     * @param playerUuid Player UUID
     * @param confirmed Confirmation status
     */
    public void setPlayerConfirmed(final UUID playerUuid, final boolean confirmed) {
        if (this.isTrader1(playerUuid)) {
            this.trader1Confirmed = confirmed;
        } else if (this.isTrader2(playerUuid)) {
            this.trader2Confirmed = confirmed;
        }
    }
    
    /**
     * Reset ready and confirmation status
     */
    public void resetReadyStatus() {
        this.trader1Ready = false;
        this.trader2Ready = false;
        this.trader1Confirmed = false;
        this.trader2Confirmed = false;
    }
    
    /**
     * Cancel the trade session
     */
    public void cancel() {
        this.status = "cancelled";
    }
    
    /**
     * Complete the trade session
     */
    public void complete() {
        this.status = "completed";
    }
    
    /**
     * Get online player by UUID
     * @param playerUuid Player UUID
     * @return Online player or null
     */
    public Player getOnlinePlayer(final UUID playerUuid) {
        return Bukkit.getPlayer(playerUuid);
    }
    
    /**
     * Get both traders as online players
     * @return Array of [trader1, trader2] or null if either offline
     */
    public Player[] getOnlineTraders() {
        final Player trader1 = this.getOnlinePlayer(this.trader1Uuid);
        final Player trader2 = this.getOnlinePlayer(this.trader2Uuid);
        
        if (trader1 != null && trader2 != null) {
            return new Player[]{trader1, trader2};
        }
        
        return null;
    }
    
    /**
     * Get minutes until expiration
     * @return Minutes until expiration
     */
    public long getMinutesUntilExpiration() {
        if (this.isExpired()) {
            return 0;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), this.expiresAt).toMinutes();
    }
}