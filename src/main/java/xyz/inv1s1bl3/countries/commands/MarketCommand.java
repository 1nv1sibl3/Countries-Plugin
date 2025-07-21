package xyz.inv1s1bl3.countries.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.market.MarketListing;
import xyz.inv1s1bl3.countries.core.market.MarketTransaction;
import xyz.inv1s1bl3.countries.core.market.PriceHistory;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles all market-related commands.
 * Provides functionality for listing, buying, and searching items in the market.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class MarketCommand implements CommandExecutor, TabCompleter {
    
    private final CountriesPlugin plugin;
    
    public MarketCommand(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull final CommandSender sender, 
                           @NotNull final Command command, 
                           @NotNull final String label, 
                           @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            ChatUtils.sendError(sender, this.plugin.getMessage("general.player-only"));
            return true;
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 0) {
            this.sendHelpMessage(player);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
            case "sell":
                this.handleListCommand(player, args);
                break;
            case "buy":
                this.handleBuyCommand(player, args);
                break;
            case "search":
                this.handleSearchCommand(player, args);
                break;
            case "browse":
                this.handleBrowseCommand(player, args);
                break;
            case "cancel":
            case "remove":
                this.handleCancelCommand(player, args);
                break;
            case "info":
                this.handleInfoCommand(player, args);
                break;
            case "history":
                this.handleHistoryCommand(player, args);
                break;
            case "price":
                this.handlePriceCommand(player, args);
                break;
            case "stats":
                this.handleStatsCommand(player);
                break;
            case "gui":
                this.handleGuiCommand(player);
                break;
            default:
                ChatUtils.sendError(player, this.plugin.getMessage("general.unknown-command"));
                break;
        }
        
        return true;
    }
    
    /**
     * Handle the list subcommand.
     */
    private void handleListCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.list")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/market list <price>"));
            return;
        }
        
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            ChatUtils.sendError(player, "&cYou must hold an item to list it!");
            return;
        }
        
        final double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (final NumberFormatException exception) {
            ChatUtils.sendError(player, "&cInvalid price!");
            return;
        }
        
        if (price <= 0) {
            ChatUtils.sendError(player, "&cPrice must be positive!");
            return;
        }
        
        final double maxPrice = this.plugin.getConfigManager()
            .getConfigValue("market.max-listing-price", 100000.0);
        if (price > maxPrice) {
            ChatUtils.sendError(player, "&cPrice exceeds maximum limit of $" + maxPrice);
            return;
        }
        
        final MarketListing listing = this.plugin.getMarketManager().createListing(player, heldItem, price);
        if (listing != null) {
            ChatUtils.sendSuccess(player, this.plugin.getMessage("market.listed", 
                "item", listing.getDisplayName(),
                "amount", String.valueOf(listing.getAmount()),
                "price", String.format("%.2f", price),
                "total", String.format("%.2f", listing.getTotalPrice()),
                "id", listing.getListingId().toString().substring(0, 8)));
        } else {
            ChatUtils.sendError(player, "&cFailed to create listing! Check your balance and listing limits.");
        }
    }
    
    /**
     * Handle the buy subcommand.
     */
    private void handleBuyCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.buy")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/market buy <listing-id>"));
            return;
        }
        
        final UUID listingId;
        try {
            // Support partial UUIDs (first 8 characters)
            String idString = args[1];
            if (idString.length() == 8) {
                // Find full UUID by partial match
                final MarketListing found = this.plugin.getMarketManager().getActiveListings().values().stream()
                    .filter(listing -> listing.getListingId().toString().startsWith(idString))
                    .findFirst()
                    .orElse(null);
                
                if (found == null) {
                    ChatUtils.sendError(player, this.plugin.getMessage("market.listing-not-found", "id", idString));
                    return;
                }
                
                listingId = found.getListingId();
            } else {
                listingId = UUID.fromString(idString);
            }
        } catch (final IllegalArgumentException exception) {
            ChatUtils.sendError(player, "&cInvalid listing ID!");
            return;
        }
        
        if (this.plugin.getMarketManager().purchaseListing(player, listingId)) {
            final MarketListing listing = this.plugin.getMarketManager().getListing(listingId);
            if (listing != null) {
                ChatUtils.sendSuccess(player, this.plugin.getMessage("market.purchased", 
                    "item", listing.getDisplayName(),
                    "amount", String.valueOf(listing.getAmount()),
                    "price", String.format("%.2f", listing.getTotalPrice()),
                    "seller", listing.getSellerName()));
            }
        } else {
            ChatUtils.sendError(player, "&cFailed to purchase listing! Check your balance and inventory space.");
        }
    }
    
    /**
     * Handle the search subcommand.
     */
    private void handleSearchCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.search")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final String query = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
        final List<MarketListing> results = this.plugin.getMarketManager().searchListings(query, null, 0, 20);
        
        if (results.isEmpty()) {
            ChatUtils.sendInfo(player, this.plugin.getMessage("market.no-results", "query", query));
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Market Search Results"));
        ChatUtils.sendInfo(player, "&7Found " + results.size() + " listings for '" + query + "':");
        
        for (final MarketListing listing : results) {
            final String shortId = listing.getListingId().toString().substring(0, 8);
            ChatUtils.sendMessage(player, String.format("&e#%s &7- %s &f$%.2f &7by &e%s", 
                shortId, listing.getShortDescription(), listing.getTotalPrice(), listing.getSellerName()));
        }
        
        ChatUtils.sendInfo(player, "&7Use &e/market buy <id> &7to purchase a listing.");
    }
    
    /**
     * Handle the browse subcommand.
     */
    private void handleBrowseCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.browse")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final String category = args.length > 1 ? args[1].toUpperCase() : null;
        final List<MarketListing> listings;
        
        if (category != null) {
            listings = this.plugin.getMarketManager().getListingsByCategory(category, 20);
        } else {
            listings = this.plugin.getMarketManager().getRecentListings(20);
        }
        
        if (listings.isEmpty()) {
            ChatUtils.sendInfo(player, "&7No listings found" + (category != null ? " in category " + category : "") + ".");
            return;
        }
        
        final String title = category != null ? category + " Listings" : "Recent Listings";
        ChatUtils.sendMessage(player, ChatUtils.createHeader(title));
        
        for (final MarketListing listing : listings) {
            final String shortId = listing.getListingId().toString().substring(0, 8);
            final long hoursRemaining = listing.getHoursRemaining();
            ChatUtils.sendMessage(player, String.format("&e#%s &7- %s &f$%.2f &7by &e%s &8(%dh left)", 
                shortId, listing.getShortDescription(), listing.getTotalPrice(), 
                listing.getSellerName(), hoursRemaining));
        }
        
        ChatUtils.sendInfo(player, "&7Categories: WEAPONS, ARMOR, TOOLS, FOOD, BLOCKS, POTIONS, ENCHANTMENTS, MISC");
    }
    
    /**
     * Handle the cancel subcommand.
     */
    private void handleCancelCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.cancel")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        if (args.length < 2) {
            // Show player's active listings
            final List<MarketListing> listings = this.plugin.getMarketManager()
                .getPlayerActiveListings(player.getUniqueId());
            
            if (listings.isEmpty()) {
                ChatUtils.sendInfo(player, "&7You have no active listings.");
                return;
            }
            
            ChatUtils.sendMessage(player, ChatUtils.createHeader("Your Active Listings"));
            for (final MarketListing listing : listings) {
                final String shortId = listing.getListingId().toString().substring(0, 8);
                final long hoursRemaining = listing.getHoursRemaining();
                ChatUtils.sendMessage(player, String.format("&e#%s &7- %s &f$%.2f &8(%dh left)", 
                    shortId, listing.getShortDescription(), listing.getTotalPrice(), hoursRemaining));
            }
            ChatUtils.sendInfo(player, "&7Use &e/market cancel <id> &7to cancel a listing.");
            return;
        }
        
        final UUID listingId;
        try {
            String idString = args[1];
            if (idString.length() == 8) {
                // Find full UUID by partial match
                final MarketListing found = this.plugin.getMarketManager()
                    .getPlayerActiveListings(player.getUniqueId()).stream()
                    .filter(listing -> listing.getListingId().toString().startsWith(idString))
                    .findFirst()
                    .orElse(null);
                
                if (found == null) {
                    ChatUtils.sendError(player, this.plugin.getMessage("market.listing-not-found", "id", idString));
                    return;
                }
                
                listingId = found.getListingId();
            } else {
                listingId = UUID.fromString(idString);
            }
        } catch (final IllegalArgumentException exception) {
            ChatUtils.sendError(player, "&cInvalid listing ID!");
            return;
        }
        
        if (this.plugin.getMarketManager().cancelListing(player, listingId)) {
            ChatUtils.sendSuccess(player, "&aListing canceled and item returned!");
        } else {
            ChatUtils.sendError(player, "&cFailed to cancel listing! Make sure you own it.");
        }
    }
    
    /**
     * Handle the info subcommand.
     */
    private void handleInfoCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (args.length < 2) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                "usage", "/market info <listing-id>"));
            return;
        }
        
        final UUID listingId;
        try {
            String idString = args[1];
            if (idString.length() == 8) {
                // Find full UUID by partial match
                final MarketListing found = this.plugin.getMarketManager().getActiveListings().values().stream()
                    .filter(listing -> listing.getListingId().toString().startsWith(idString))
                    .findFirst()
                    .orElse(null);
                
                if (found == null) {
                    ChatUtils.sendError(player, this.plugin.getMessage("market.listing-not-found", "id", idString));
                    return;
                }
                
                listingId = found.getListingId();
            } else {
                listingId = UUID.fromString(idString);
            }
        } catch (final IllegalArgumentException exception) {
            ChatUtils.sendError(player, "&cInvalid listing ID!");
            return;
        }
        
        final MarketListing listing = this.plugin.getMarketManager().getListing(listingId);
        if (listing == null) {
            ChatUtils.sendError(player, this.plugin.getMessage("market.listing-not-found", "id", args[1]));
            return;
        }
        
        // Increment view count
        listing.incrementViews();
        
        this.sendListingInfo(player, listing);
    }
    
    /**
     * Handle the history subcommand.
     */
    private void handleHistoryCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (!player.hasPermission("countries.market.history")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final int limit = args.length > 1 ? Math.min(Integer.parseInt(args[1]), 20) : 10;
        final List<MarketTransaction> transactions = this.plugin.getMarketManager()
            .getPlayerTransactions(player.getUniqueId(), limit);
        
        if (transactions.isEmpty()) {
            ChatUtils.sendInfo(player, "&7No market transaction history found.");
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Market Transaction History"));
        for (final MarketTransaction transaction : transactions) {
            final String type = transaction.isSaleFor(player.getUniqueId()) ? "&aSold" : "&eBought";
            ChatUtils.sendMessage(player, String.format("%s &f%s x%d &7for &f$%.2f", 
                type, transaction.getItemDisplayName(), transaction.getAmount(), transaction.getTotalPrice()));
        }
    }
    
    /**
     * Handle the price subcommand.
     */
    private void handlePriceCommand(@NotNull final Player player, @NotNull final String[] args) {
        if (args.length < 2) {
            final ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (heldItem.getType() == Material.AIR) {
                ChatUtils.sendError(player, this.plugin.getMessage("general.invalid-syntax", 
                    "usage", "/market price <item-type>"));
                return;
            }
            args = new String[]{"price", heldItem.getType().name()};
        }
        
        final String itemType = args[1].toUpperCase();
        final PriceHistory history = this.plugin.getMarketManager().getPriceHistory(itemType);
        
        if (history == null) {
            ChatUtils.sendInfo(player, "&7No price history found for " + itemType);
            return;
        }
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Price History: " + itemType));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Current Average", "$" + String.format("%.2f", history.getAveragePrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Latest Price", "$" + String.format("%.2f", history.getLatestPrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Min Price", "$" + String.format("%.2f", history.getMinPrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Max Price", "$" + String.format("%.2f", history.getMaxPrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Volume", String.valueOf(history.getTotalVolume())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Price Trend", String.format("%.1f%%", history.getPriceTrend())));
    }
    
    /**
     * Handle the stats subcommand.
     */
    private void handleStatsCommand(@NotNull final Player player) {
        if (!player.hasPermission("countries.market.stats")) {
            ChatUtils.sendError(player, this.plugin.getMessage("general.no-permission"));
            return;
        }
        
        final Map<String, Object> stats = this.plugin.getMarketManager().getMarketStatistics();
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Market Statistics"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Active Listings", stats.get("active_listings").toString()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Transactions", stats.get("total_transactions").toString()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Market Value", 
            "$" + String.format("%.2f", (Double) stats.get("total_market_value"))));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Average Listing Price", 
            "$" + String.format("%.2f", (Double) stats.get("average_listing_price"))));
        
        @SuppressWarnings("unchecked")
        final Map<String, Long> categories = (Map<String, Long>) stats.get("category_distribution");
        if (categories != null && !categories.isEmpty()) {
            ChatUtils.sendMessage(player, "&eCategory Distribution:");
            categories.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .forEach(entry -> ChatUtils.sendMessage(player, 
                    "&7  " + entry.getKey() + ": " + entry.getValue()));
        }
    }
    
    /**
     * Handle the gui subcommand.
     */
    private void handleGuiCommand(@NotNull final Player player) {
        // TODO: Implement GUI opening
        ChatUtils.sendInfo(player, "&7Market GUI system coming soon!");
    }
    
    /**
     * Send detailed listing information to a player.
     */
    private void sendListingInfo(@NotNull final Player player, @NotNull final MarketListing listing) {
        final String shortId = listing.getListingId().toString().substring(0, 8);
        
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Listing Info"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("ID", shortId));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Item", listing.getDisplayName()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Amount", String.valueOf(listing.getAmount())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Price per Item", "$" + String.format("%.2f", listing.getUnitPrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Total Price", "$" + String.format("%.2f", listing.getTotalPrice())));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Seller", listing.getSellerName()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Category", listing.getCategory()));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Listed", listing.getAgeHours() + " hours ago"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Expires", listing.getHoursRemaining() + " hours"));
        ChatUtils.sendMessage(player, ChatUtils.createListItem("Views", String.valueOf(listing.getViews())));
        
        if (listing.getLore() != null && !listing.getLore().isEmpty()) {
            ChatUtils.sendMessage(player, "&eLore:");
            for (final String line : listing.getLore()) {
                ChatUtils.sendMessage(player, "&7  " + line);
            }
        }
    }
    
    /**
     * Send help message to a player.
     */
    private void sendHelpMessage(@NotNull final Player player) {
        ChatUtils.sendMessage(player, ChatUtils.createHeader("Market Commands"));
        ChatUtils.sendMessage(player, "&e/market list <price> &7- List held item for sale");
        ChatUtils.sendMessage(player, "&e/market buy <id> &7- Purchase a listing");
        ChatUtils.sendMessage(player, "&e/market search [query] &7- Search for items");
        ChatUtils.sendMessage(player, "&e/market browse [category] &7- Browse listings");
        ChatUtils.sendMessage(player, "&e/market cancel [id] &7- Cancel your listing");
        ChatUtils.sendMessage(player, "&e/market info <id> &7- View listing details");
        ChatUtils.sendMessage(player, "&e/market history [limit] &7- View transaction history");
        ChatUtils.sendMessage(player, "&e/market price [item] &7- View price history");
        ChatUtils.sendMessage(player, "&e/market stats &7- View market statistics");
        ChatUtils.sendMessage(player, "&e/market gui &7- Open market GUI");
    }
        
        return true;
    }
    
    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull final CommandSender sender, 
                                    @NotNull final Command command, 
                                    @NotNull final String alias, 
                                    @NotNull final String[] args) {
        
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        final Player player = (Player) sender;
        
        if (args.length == 1) {
            return Arrays.asList("list", "sell", "buy", "search", "browse", "cancel", "remove", 
                               "info", "history", "price", "stats", "gui")
                .stream()
                .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            final String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "browse":
                    return Arrays.asList("WEAPONS", "ARMOR", "TOOLS", "FOOD", "BLOCKS", "POTIONS", "ENCHANTMENTS", "MISC")
                        .stream()
                        .filter(category -> category.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                        
                case "buy":
                case "info":
                case "cancel":
                case "remove":
                    return this.plugin.getMarketManager().getActiveListings().values().stream()
                        .map(listing -> listing.getListingId().toString().substring(0, 8))
                        .filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
                        
                case "price":
                    return Arrays.stream(Material.values())
                        .map(material -> material.name().toLowerCase())
                        .filter(name -> name.startsWith(args[1].toLowerCase()))
                        .limit(10)
                        .collect(Collectors.toList());
            }
        }
        
        return new ArrayList<>();
    }
}