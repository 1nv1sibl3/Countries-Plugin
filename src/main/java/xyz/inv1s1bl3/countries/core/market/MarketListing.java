package xyz.inv1s1bl3.countries.core.market;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Represents a market listing for an item.
 * Contains all information about an item being sold in the market.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class MarketListing {
    
    @Expose
    @EqualsAndHashCode.Include
    private final UUID listingId;
    
    @Expose
    private final UUID sellerId;
    
    @Expose
    private final String sellerName;
    
    @Expose
    private final String itemType;
    
    @Expose
    private final int amount;
    
    @Expose
    private final double price;
    
    @Expose
    private final String displayName;
    
    @Expose
    private final List<String> lore;
    
    @Expose
    private final String itemData; // Serialized ItemStack data
    
    @Expose
    private final LocalDateTime listedDate;
    
    @Expose
    private final LocalDateTime expiryDate;
    
    @Expose
    private final String category;
    
    @Expose
    private boolean isActive;
    
    @Expose
    private boolean isFeatured;
    
    @Expose
    private int views;
    
    @Expose
    private LocalDateTime lastViewed;
    
    /**
     * Create a new market listing.
     * 
     * @param listingId the listing ID
     * @param sellerId the seller's UUID
     * @param sellerName the seller's name
     * @param itemStack the item being sold
     * @param price the price per item
     * @param durationHours the listing duration in hours
     */
    public MarketListing(@NotNull final UUID listingId,
                        @NotNull final UUID sellerId,
                        @NotNull final String sellerName,
                        @NotNull final ItemStack itemStack,
                        final double price,
                        final int durationHours) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.itemType = itemStack.getType().name();
        this.amount = itemStack.getAmount();
        this.price = price;
        this.listedDate = LocalDateTime.now();
        this.expiryDate = this.listedDate.plusHours(durationHours);
        this.isActive = true;
        this.isFeatured = false;
        this.views = 0;
        this.lastViewed = null;
        
        // Extract item metadata
        final ItemMeta meta = itemStack.getItemMeta();
        this.displayName = meta != null && meta.hasDisplayName() ? meta.getDisplayName() : itemStack.getType().name();
        this.lore = meta != null && meta.hasLore() ? meta.getLore() : null;
        
        // Serialize item data (simplified - in production you'd use proper serialization)
        this.itemData = this.serializeItemStack(itemStack);
        
        // Determine category
        this.category = this.determineCategory(itemStack.getType());
    }
    
    /**
     * Serialize an ItemStack to string (simplified implementation).
     * 
     * @param itemStack the item stack
     * @return serialized data
     */
    @NotNull
    private String serializeItemStack(@NotNull final ItemStack itemStack) {
        // This is a simplified implementation
        // In production, you'd use proper ItemStack serialization
        return itemStack.getType().name() + ":" + itemStack.getAmount();
    }
    
    /**
     * Deserialize an ItemStack from string (simplified implementation).
     * 
     * @return the deserialized ItemStack
     */
    @NotNull
    public ItemStack deserializeItemStack() {
        // This is a simplified implementation
        // In production, you'd use proper ItemStack deserialization
        final String[] parts = this.itemData.split(":");
        final Material material = Material.valueOf(parts[0]);
        final int amount = Integer.parseInt(parts[1]);
        
        final ItemStack itemStack = new ItemStack(material, amount);
        
        // Apply metadata if available
        if (this.displayName != null || this.lore != null) {
            final ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                if (this.displayName != null && !this.displayName.equals(material.name())) {
                    meta.setDisplayName(this.displayName);
                }
                if (this.lore != null) {
                    meta.setLore(this.lore);
                }
                itemStack.setItemMeta(meta);
            }
        }
        
        return itemStack;
    }
    
    /**
     * Determine the category of an item.
     * 
     * @param material the item material
     * @return the category
     */
    @NotNull
    private String determineCategory(@NotNull final Material material) {
        final String name = material.name();
        
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || 
            name.contains("CROSSBOW") || name.contains("TRIDENT")) {
            return "WEAPONS";
        } else if (name.contains("HELMET") || name.contains("CHESTPLATE") || 
                  name.contains("LEGGINGS") || name.contains("BOOTS")) {
            return "ARMOR";
        } else if (name.contains("PICKAXE") || name.contains("SHOVEL") || 
                  name.contains("HOE") || name.contains("SHEARS")) {
            return "TOOLS";
        } else if (material.isEdible()) {
            return "FOOD";
        } else if (material.isBlock()) {
            return "BLOCKS";
        } else if (name.contains("POTION") || name.contains("SPLASH")) {
            return "POTIONS";
        } else if (name.contains("BOOK") || name.contains("ENCHANTED")) {
            return "ENCHANTMENTS";
        } else {
            return "MISC";
        }
    }
    
    /**
     * Check if the listing has expired.
     * 
     * @return true if expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
    
    /**
     * Get the time remaining until expiry.
     * 
     * @return hours remaining, or 0 if expired
     */
    public long getHoursRemaining() {
        if (this.isExpired()) {
            return 0;
        }
        
        return java.time.Duration.between(LocalDateTime.now(), this.expiryDate).toHours();
    }
    
    /**
     * Get the total price for all items.
     * 
     * @return the total price
     */
    public double getTotalPrice() {
        return this.price * this.amount;
    }
    
    /**
     * Get the price per item.
     * 
     * @return the unit price
     */
    public double getUnitPrice() {
        return this.price;
    }
    
    /**
     * Increment the view count.
     */
    public void incrementViews() {
        this.views++;
        this.lastViewed = LocalDateTime.now();
    }
    
    /**
     * Mark the listing as sold.
     */
    public void markAsSold() {
        this.isActive = false;
    }
    
    /**
     * Cancel the listing.
     */
    public void cancel() {
        this.isActive = false;
    }
    
    /**
     * Check if the listing can be purchased by a player.
     * 
     * @param buyerId the buyer's UUID
     * @return true if can be purchased
     */
    public boolean canBePurchasedBy(@NotNull final UUID buyerId) {
        return this.isActive && !this.isExpired() && !this.sellerId.equals(buyerId);
    }
    
    /**
     * Get the seller as an OfflinePlayer.
     * 
     * @return the seller
     */
    @NotNull
    public OfflinePlayer getSeller() {
        return Bukkit.getOfflinePlayer(this.sellerId);
    }
    
    /**
     * Get a formatted description of the listing.
     * 
     * @return formatted description
     */
    @NotNull
    public String getFormattedDescription() {
        return String.format("%s x%d - $%.2f each ($%.2f total) by %s", 
            this.getDisplayName(), this.amount, this.price, this.getTotalPrice(), this.sellerName);
    }
    
    /**
     * Get a short description for search results.
     * 
     * @return short description
     */
    @NotNull
    public String getShortDescription() {
        return String.format("%s x%d - $%.2f", this.getDisplayName(), this.amount, this.getTotalPrice());
    }
    
    /**
     * Check if the listing matches a search query.
     * 
     * @param query the search query
     * @return true if matches
     */
    public boolean matchesQuery(@NotNull final String query) {
        final String lowerQuery = query.toLowerCase();
        
        return this.itemType.toLowerCase().contains(lowerQuery) ||
               this.displayName.toLowerCase().contains(lowerQuery) ||
               this.sellerName.toLowerCase().contains(lowerQuery) ||
               this.category.toLowerCase().contains(lowerQuery);
    }
    
    /**
     * Get the age of the listing in hours.
     * 
     * @return age in hours
     */
    public long getAgeHours() {
        return java.time.Duration.between(this.listedDate, LocalDateTime.now()).toHours();
    }
}