package xyz.inv1s1bl3.countries.trading;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a trade confirmation with all trade details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class TradeConfirmation {
    
    private String sessionId;
    private UUID trader1Uuid;
    private UUID trader2Uuid;
    private String trader1Name;
    private String trader2Name;
    private Map<Integer, ItemStack> trader1Items;
    private Map<Integer, ItemStack> trader2Items;
    private double trader1Money;
    private double trader2Money;
    private LocalDateTime confirmedAt;
    private String worldName;
    private int x;
    private int y;
    private int z;
    
    /**
     * Check if trade involves money exchange
     * @return true if money is being exchanged
     */
    public boolean involvesMoney() {
        return this.trader1Money > 0 || this.trader2Money > 0;
    }
    
    /**
     * Check if trade involves items
     * @return true if items are being exchanged
     */
    public boolean involvesItems() {
        return !this.trader1Items.isEmpty() || !this.trader2Items.isEmpty();
    }
    
    /**
     * Get total number of items being traded
     * @return Total item count
     */
    public int getTotalItemCount() {
        int count = 0;
        
        for (final ItemStack item : this.trader1Items.values()) {
            if (item != null) {
                count += item.getAmount();
            }
        }
        
        for (final ItemStack item : this.trader2Items.values()) {
            if (item != null) {
                count += item.getAmount();
            }
        }
        
        return count;
    }
    
    /**
     * Get net money exchange for a player
     * @param playerUuid Player UUID
     * @return Net money change (positive = receiving, negative = giving)
     */
    public double getNetMoneyExchange(final UUID playerUuid) {
        if (this.trader1Uuid.equals(playerUuid)) {
            return this.trader2Money - this.trader1Money;
        } else if (this.trader2Uuid.equals(playerUuid)) {
            return this.trader1Money - this.trader2Money;
        }
        return 0.0;
    }
    
    /**
     * Get items being received by a player
     * @param playerUuid Player UUID
     * @return Items being received
     */
    public Map<Integer, ItemStack> getItemsReceived(final UUID playerUuid) {
        if (this.trader1Uuid.equals(playerUuid)) {
            return this.trader2Items;
        } else if (this.trader2Uuid.equals(playerUuid)) {
            return this.trader1Items;
        }
        return Map.of();
    }
    
    /**
     * Get items being given by a player
     * @param playerUuid Player UUID
     * @return Items being given
     */
    public Map<Integer, ItemStack> getItemsGiven(final UUID playerUuid) {
        if (this.trader1Uuid.equals(playerUuid)) {
            return this.trader1Items;
        } else if (this.trader2Uuid.equals(playerUuid)) {
            return this.trader2Items;
        }
        return Map.of();
    }
    
    /**
     * Get formatted trade summary
     * @return Trade summary string
     */
    public String getFormattedSummary() {
        final StringBuilder summary = new StringBuilder();
        
        summary.append("Trade between ").append(this.trader1Name).append(" and ").append(this.trader2Name);
        
        if (this.involvesMoney()) {
            summary.append("\nMoney: ");
            if (this.trader1Money > 0) {
                summary.append(this.trader1Name).append(" pays $").append(String.format("%.2f", this.trader1Money));
            }
            if (this.trader2Money > 0) {
                if (this.trader1Money > 0) summary.append(", ");
                summary.append(this.trader2Name).append(" pays $").append(String.format("%.2f", this.trader2Money));
            }
        }
        
        if (this.involvesItems()) {
            summary.append("\nItems: ").append(this.getTotalItemCount()).append(" items total");
        }
        
        return summary.toString();
    }
    
    /**
     * Check if trade is balanced (both sides offer something)
     * @return true if balanced
     */
    public boolean isBalanced() {
        final boolean trader1Offers = this.trader1Money > 0 || !this.trader1Items.isEmpty();
        final boolean trader2Offers = this.trader2Money > 0 || !this.trader2Items.isEmpty();
        
        return trader1Offers && trader2Offers;
    }
    
    /**
     * Get trade value estimation (money + rough item count)
     * @param playerUuid Player UUID
     * @return Estimated trade value for player
     */
    public double getEstimatedValue(final UUID playerUuid) {
        double value = 0.0;
        
        // Add money value
        if (this.trader1Uuid.equals(playerUuid)) {
            value += this.trader1Money;
            value += this.trader1Items.size() * 10; // Rough item estimation
        } else if (this.trader2Uuid.equals(playerUuid)) {
            value += this.trader2Money;
            value += this.trader2Items.size() * 10; // Rough item estimation
        }
        
        return value;
    }
}