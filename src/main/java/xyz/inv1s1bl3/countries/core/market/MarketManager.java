package xyz.inv1s1bl3.countries.core.market;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.economy.Transaction;
import xyz.inv1s1bl3.countries.core.economy.TransactionType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages the market system for the plugin.
 * Handles item listings, purchases, and market statistics.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class MarketManager {
    
    private final CountriesPlugin plugin;
    
    @Getter
    private final Map<UUID, MarketListing> activeListings;
    private final Map<UUID, MarketListing> expiredListings;
    private final List<MarketTransaction> transactionHistory;
    private final Map<String, PriceHistory> priceHistories;
    private final Map<UUID, Set<UUID>> playerListings; // Player -> Set of listing IDs
    
    public MarketManager(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
        this.activeListings = new ConcurrentHashMap<>();
        this.expiredListings = new ConcurrentHashMap<>();
        this.transactionHistory = new ArrayList<>();
        this.priceHistories = new ConcurrentHashMap<>();
        this.playerListings = new ConcurrentHashMap<>();
    }
    
    /**
     * Create a new market listing.
     * 
     * @param seller the seller
     * @param itemStack the item to sell
     * @param pricePerItem the price per item
     * @return the created listing, or null if failed
     */
    @Nullable
    public MarketListing createListing(@NotNull final Player seller, 
                                     @NotNull final ItemStack itemStack, 
                                     final double pricePerItem) {
        // Validate input
        if (itemStack.getAmount() <= 0 || pricePerItem <= 0) {
            return null;
        }
        
        // Check if player has reached listing limit
        final int maxListings = this.plugin.getConfigManager()
            .getConfigValue("market.max-listings-per-player", 10);
        final int currentListings = this.getPlayerActiveListings(seller.getUniqueId()).size();
        
        if (currentListings >= maxListings) {
            return null;
        }
        
        // Check listing fee
        final double listingFee = this.plugin.getConfigManager()
            .getConfigValue("market.listing-fee", 10.0);
        
        if (this.plugin.getEconomyManager().getBalance(seller.getUniqueId()) < listingFee) {
            return null;
        }
        
        // Check if player has the item in inventory
        if (!seller.getInventory().containsAtLeast(itemStack, itemStack.getAmount())) {
            return null;
        }
        
        // Remove item from player's inventory
        seller.getInventory().removeItem(itemStack);
        
        // Charge listing fee
        this.plugin.getEconomyManager().withdraw(seller.getUniqueId(), listingFee, "Market listing fee");
        
        // Create listing
        final UUID listingId = UUID.randomUUID();
        final int durationHours = this.plugin.getConfigManager()
            .getConfigValue("market.listing-duration", 168); // 7 days default
        
        final MarketListing listing = new MarketListing(
            listingId, seller.getUniqueId(), seller.getName(), 
            itemStack, pricePerItem, durationHours);
        
        // Add to maps
        this.activeListings.put(listingId, listing);
        this.playerListings.computeIfAbsent(seller.getUniqueId(), k -> new HashSet<>()).add(listingId);
        
        // Save data
        this.plugin.getDataManager().saveMarketData();
        
        return listing;
    }
    
    /**
     * Purchase a market listing.
     * 
     * @param buyer the buyer
     * @param listingId the listing ID
     * @return true if purchase was successful
     */
    public boolean purchaseListing(@NotNull final Player buyer, @NotNull final UUID listingId) {
        final MarketListing listing = this.activeListings.get(listingId);
        if (listing == null || !listing.canBePurchasedBy(buyer.getUniqueId())) {
            return false;
        }
        
        final double totalPrice = listing.getTotalPrice();
        final double transactionFee = totalPrice * this.plugin.getConfigManager()
            .getConfigValue("market.transaction-fee", 0.02);
        final double sellerReceives = totalPrice - transactionFee;
        
        // Check buyer's balance
        if (this.plugin.getEconomyManager().getBalance(buyer.getUniqueId()) < totalPrice) {
            return false;
        }
        
        // Check if buyer has inventory space
        final ItemStack item = listing.deserializeItemStack();
        if (!this.hasInventorySpace(buyer, item)) {
            return false;
        }
        
        // Process transaction
        this.plugin.getEconomyManager().withdraw(buyer.getUniqueId(), totalPrice, 
            "Market purchase: " + listing.getDisplayName());
        this.plugin.getEconomyManager().deposit(listing.getSellerId(), sellerReceives, 
            "Market sale: " + listing.getDisplayName());
        
        // Give item to buyer
        buyer.getInventory().addItem(item);
        
        // Mark listing as sold
        listing.markAsSold();
        this.activeListings.remove(listingId);
        
        // Remove from player listings
        final Set<UUID> sellerListings = this.playerListings.get(listing.getSellerId());
        if (sellerListings != null) {
            sellerListings.remove(listingId);
        }
        
        // Record transaction
        final MarketTransaction transaction = new MarketTransaction(
            UUID.randomUUID(), listing, buyer.getUniqueId(), buyer.getName(), transactionFee);
        this.transactionHistory.add(transaction);
        
        // Update price history
        this.updatePriceHistory(listing.getItemType(), listing.getUnitPrice(), listing.getAmount());
        
        // Log economy transaction
        this.plugin.getEconomyManager().logTransaction(new Transaction(
            UUID.randomUUID(),
            TransactionType.MARKET_TRANSACTION,
            totalPrice,
            "Market purchase: " + listing.getDisplayName(),
            buyer.getUniqueId(),
            listing.getSellerId()
        ));
        
        // Notify players
        if (buyer.isOnline()) {
            // Buyer notification handled by command
        }
        
        final OfflinePlayer seller = listing.getSeller();
        if (seller.isOnline()) {
            final Player onlineSeller = seller.getPlayer();
            if (onlineSeller != null) {
                onlineSeller.sendMessage(this.plugin.getMessage("market.sold", 
                    "item", listing.getDisplayName(), 
                    "amount", String.valueOf(listing.getAmount()),
                    "price", String.format("%.2f", sellerReceives),
                    "buyer", buyer.getName()));
            }
        }
        
        // Save data
        this.plugin.getDataManager().saveMarketData();
        
        return true;
    }
    
    /**
     * Cancel a market listing.
     * 
     * @param player the player canceling
     * @param listingId the listing ID
     * @return true if canceled successfully
     */
    public boolean cancelListing(@NotNull final Player player, @NotNull final UUID listingId) {
        final MarketListing listing = this.activeListings.get(listingId);
        if (listing == null || !listing.getSellerId().equals(player.getUniqueId())) {
            return false;
        }
        
        // Return item to player
        final ItemStack item = listing.deserializeItemStack();
        if (this.hasInventorySpace(player, item)) {
            player.getInventory().addItem(item);
        } else {
            // Drop item if inventory is full
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        
        // Remove listing
        listing.cancel();
        this.activeListings.remove(listingId);
        
        // Remove from player listings
        final Set<UUID> playerListingSet = this.playerListings.get(player.getUniqueId());
        if (playerListingSet != null) {
            playerListingSet.remove(listingId);
        }
        
        // Save data
        this.plugin.getDataManager().saveMarketData();
        
        return true;
    }
    
    /**
     * Get a market listing by ID.
     * 
     * @param listingId the listing ID
     * @return the listing, or null if not found
     */
    @Nullable
    public MarketListing getListing(@NotNull final UUID listingId) {
        return this.activeListings.get(listingId);
    }
    
    /**
     * Get all active listings for a player.
     * 
     * @param playerId the player's UUID
     * @return a list of active listings
     */
    @NotNull
    public List<MarketListing> getPlayerActiveListings(@NotNull final UUID playerId) {
        final Set<UUID> listingIds = this.playerListings.get(playerId);
        if (listingIds == null) {
            return new ArrayList<>();
        }
        
        return listingIds.stream()
            .map(this.activeListings::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Search for market listings.
     * 
     * @param query the search query
     * @param category the category filter (null for all)
     * @param maxPrice the maximum price filter (0 for no limit)
     * @param limit the maximum number of results
     * @return a list of matching listings
     */
    @NotNull
    public List<MarketListing> searchListings(@Nullable final String query, 
                                            @Nullable final String category,
                                            final double maxPrice,
                                            final int limit) {
        return this.activeListings.values().stream()
            .filter(listing -> !listing.isExpired())
            .filter(listing -> query == null || listing.matchesQuery(query))
            .filter(listing -> category == null || listing.getCategory().equalsIgnoreCase(category))
            .filter(listing -> maxPrice <= 0 || listing.getTotalPrice() <= maxPrice)
            .sorted((l1, l2) -> Double.compare(l1.getTotalPrice(), l2.getTotalPrice()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get listings by category.
     * 
     * @param category the category
     * @param limit the maximum number of results
     * @return a list of listings
     */
    @NotNull
    public List<MarketListing> getListingsByCategory(@NotNull final String category, final int limit) {
        return this.activeListings.values().stream()
            .filter(listing -> !listing.isExpired())
            .filter(listing -> listing.getCategory().equalsIgnoreCase(category))
            .sorted((l1, l2) -> Double.compare(l1.getTotalPrice(), l2.getTotalPrice()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recent listings.
     * 
     * @param limit the maximum number of results
     * @return a list of recent listings
     */
    @NotNull
    public List<MarketListing> getRecentListings(final int limit) {
        return this.activeListings.values().stream()
            .filter(listing -> !listing.isExpired())
            .sorted((l1, l2) -> l2.getListedDate().compareTo(l1.getListedDate()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get featured listings.
     * 
     * @param limit the maximum number of results
     * @return a list of featured listings
     */
    @NotNull
    public List<MarketListing> getFeaturedListings(final int limit) {
        return this.activeListings.values().stream()
            .filter(listing -> !listing.isExpired())
            .filter(MarketListing::isFeatured)
            .sorted((l1, l2) -> l2.getListedDate().compareTo(l1.getListedDate()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Get price history for an item type.
     * 
     * @param itemType the item type
     * @return the price history, or null if not found
     */
    @Nullable
    public PriceHistory getPriceHistory(@NotNull final String itemType) {
        return this.priceHistories.get(itemType.toUpperCase());
    }
    
    /**
     * Update price history for an item type.
     * 
     * @param itemType the item type
     * @param price the price
     * @param amount the amount
     */
    private void updatePriceHistory(@NotNull final String itemType, final double price, final int amount) {
        final String key = itemType.toUpperCase();
        final PriceHistory history = this.priceHistories.computeIfAbsent(key, PriceHistory::new);
        history.addPricePoint(price, amount);
    }
    
    /**
     * Get market statistics.
     * 
     * @return a map of statistics
     */
    @NotNull
    public Map<String, Object> getMarketStatistics() {
        final Map<String, Object> stats = new HashMap<>();
        
        stats.put("active_listings", this.activeListings.size());
        stats.put("total_transactions", this.transactionHistory.size());
        
        final double totalValue = this.activeListings.values().stream()
            .mapToDouble(MarketListing::getTotalPrice)
            .sum();
        stats.put("total_market_value", totalValue);
        
        final Map<String, Long> categoryCounts = this.activeListings.values().stream()
            .collect(Collectors.groupingBy(MarketListing::getCategory, Collectors.counting()));
        stats.put("category_distribution", categoryCounts);
        
        final OptionalDouble avgPrice = this.activeListings.values().stream()
            .mapToDouble(MarketListing::getTotalPrice)
            .average();
        stats.put("average_listing_price", avgPrice.orElse(0.0));
        
        return stats;
    }
    
    /**
     * Get transaction history for a player.
     * 
     * @param playerId the player's UUID
     * @param limit the maximum number of transactions
     * @return a list of transactions
     */
    @NotNull
    public List<MarketTransaction> getPlayerTransactions(@NotNull final UUID playerId, final int limit) {
        return this.transactionHistory.stream()
            .filter(transaction -> transaction.involvesPlayer(playerId))
            .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a player has enough inventory space for an item.
     * 
     * @param player the player
     * @param item the item
     * @return true if there's enough space
     */
    private boolean hasInventorySpace(@NotNull final Player player, @NotNull final ItemStack item) {
        final ItemStack[] contents = player.getInventory().getContents();
        int remainingAmount = item.getAmount();
        
        for (final ItemStack slot : contents) {
            if (slot == null) {
                remainingAmount -= item.getMaxStackSize();
            } else if (slot.isSimilar(item)) {
                remainingAmount -= (item.getMaxStackSize() - slot.getAmount());
            }
            
            if (remainingAmount <= 0) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clean up expired market listings.
     * Removes expired listings and returns items to sellers.
     */
    public void cleanupExpiredListings() {
        final List<UUID> expiredIds = new ArrayList<>();
        
        for (final Map.Entry<UUID, MarketListing> entry : this.activeListings.entrySet()) {
            final MarketListing listing = entry.getValue();
            if (listing.isExpired()) {
                expiredIds.add(entry.getKey());
                
                // Try to return item to seller
                final OfflinePlayer seller = listing.getSeller();
                if (seller.isOnline()) {
                    final Player onlineSeller = seller.getPlayer();
                    if (onlineSeller != null) {
                        final ItemStack item = listing.deserializeItemStack();
                        if (this.hasInventorySpace(onlineSeller, item)) {
                            onlineSeller.getInventory().addItem(item);
                        } else {
                            // Drop item if inventory is full
                            onlineSeller.getWorld().dropItemNaturally(onlineSeller.getLocation(), item);
                        }
                        
                        onlineSeller.sendMessage(this.plugin.getMessage("market.listing-expired", 
                            "item", listing.getDisplayName()));
                    }
                }
                
                // Move to expired listings
                this.expiredListings.put(entry.getKey(), listing);
            }
        }
        
        // Remove expired listings
        for (final UUID expiredId : expiredIds) {
            this.activeListings.remove(expiredId);
            
            // Remove from player listings
            for (final Set<UUID> playerListingSet : this.playerListings.values()) {
                playerListingSet.remove(expiredId);
            }
        }
        
        if (!expiredIds.isEmpty()) {
            this.plugin.getDataManager().saveMarketData();
            this.plugin.getLogger().info("Cleaned up " + expiredIds.size() + " expired market listings");
        }
    }
    
    /**
     * Load market data from storage.
     * 
     * @param listingData the listing data
     * @param transactionData the transaction data
     * @param priceData the price history data
     */
    public void loadMarketData(@NotNull final Map<UUID, MarketListing> listingData,
                              @NotNull final List<MarketTransaction> transactionData,
                              @NotNull final Map<String, PriceHistory> priceData) {
        this.activeListings.clear();
        this.transactionHistory.clear();
        this.priceHistories.clear();
        this.playerListings.clear();
        
        // Load active listings
        for (final Map.Entry<UUID, MarketListing> entry : listingData.entrySet()) {
            final MarketListing listing = entry.getValue();
            if (listing.isActive() && !listing.isExpired()) {
                this.activeListings.put(entry.getKey(), listing);
                this.playerListings.computeIfAbsent(listing.getSellerId(), k -> new HashSet<>())
                    .add(entry.getKey());
            }
        }
        
        // Load transaction history
        this.transactionHistory.addAll(transactionData);
        
        // Load price histories
        this.priceHistories.putAll(priceData);
    }
    
    /**
     * Get all market data for saving.
     * 
     * @return a map containing all market data
     */
    @NotNull
    public Map<String, Object> getAllMarketData() {
        final Map<String, Object> data = new HashMap<>();
        data.put("active_listings", this.activeListings);
        data.put("expired_listings", this.expiredListings);
        data.put("transactions", this.transactionHistory);
        data.put("price_histories", this.priceHistories);
        data.put("last_cleanup", LocalDateTime.now());
        return data;
    }
}