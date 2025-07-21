package xyz.inv1s1bl3.countries.core.market;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a completed market transaction.
 * Records the sale of items between players through the market.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class MarketTransaction {
    
    @Expose
    @EqualsAndHashCode.Include
    private final UUID transactionId;
    
    @Expose
    private final UUID listingId;
    
    @Expose
    private final UUID sellerId;
    
    @Expose
    private final String sellerName;
    
    @Expose
    private final UUID buyerId;
    
    @Expose
    private final String buyerName;
    
    @Expose
    private final String itemType;
    
    @Expose
    private final String itemDisplayName;
    
    @Expose
    private final int amount;
    
    @Expose
    private final double unitPrice;
    
    @Expose
    private final double totalPrice;
    
    @Expose
    private final double marketFee;
    
    @Expose
    private final double sellerReceived;
    
    @Expose
    private final LocalDateTime transactionDate;
    
    @Expose
    private final String category;
    
    /**
     * Create a new market transaction.
     * 
     * @param transactionId the transaction ID
     * @param listing the market listing
     * @param buyerId the buyer's UUID
     * @param buyerName the buyer's name
     * @param marketFee the market fee charged
     */
    public MarketTransaction(@NotNull final UUID transactionId,
                           @NotNull final MarketListing listing,
                           @NotNull final UUID buyerId,
                           @NotNull final String buyerName,
                           final double marketFee) {
        this.transactionId = transactionId;
        this.listingId = listing.getListingId();
        this.sellerId = listing.getSellerId();
        this.sellerName = listing.getSellerName();
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.itemType = listing.getItemType();
        this.itemDisplayName = listing.getDisplayName();
        this.amount = listing.getAmount();
        this.unitPrice = listing.getUnitPrice();
        this.totalPrice = listing.getTotalPrice();
        this.marketFee = marketFee;
        this.sellerReceived = this.totalPrice - marketFee;
        this.transactionDate = LocalDateTime.now();
        this.category = listing.getCategory();
    }
    
    /**
     * Get a formatted description of the transaction.
     * 
     * @return formatted description
     */
    @NotNull
    public String getFormattedDescription() {
        return String.format("[%s] %s bought %s x%d from %s for $%.2f", 
            this.transactionDate.toString().substring(0, 19),
            this.buyerName, this.itemDisplayName, this.amount, this.sellerName, this.totalPrice);
    }
    
    /**
     * Get the profit margin for the seller (after fees).
     * 
     * @return the profit margin percentage
     */
    public double getProfitMargin() {
        if (this.totalPrice == 0) {
            return 0.0;
        }
        return (this.sellerReceived / this.totalPrice) * 100.0;
    }
    
    /**
     * Get the age of the transaction in hours.
     * 
     * @return age in hours
     */
    public long getAgeHours() {
        return java.time.Duration.between(this.transactionDate, LocalDateTime.now()).toHours();
    }
    
    /**
     * Check if this transaction involves a specific player.
     * 
     * @param playerId the player's UUID
     * @return true if the player was involved
     */
    public boolean involvesPlayer(@NotNull final UUID playerId) {
        return this.sellerId.equals(playerId) || this.buyerId.equals(playerId);
    }
    
    /**
     * Check if this transaction was a sale for the specified player.
     * 
     * @param playerId the player's UUID
     * @return true if this was a sale for the player
     */
    public boolean isSaleFor(@NotNull final UUID playerId) {
        return this.sellerId.equals(playerId);
    }
    
    /**
     * Check if this transaction was a purchase for the specified player.
     * 
     * @param playerId the player's UUID
     * @return true if this was a purchase for the player
     */
    public boolean isPurchaseFor(@NotNull final UUID playerId) {
        return this.buyerId.equals(playerId);
    }
}