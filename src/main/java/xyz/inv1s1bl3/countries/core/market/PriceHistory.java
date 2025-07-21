package xyz.inv1s1bl3.countries.core.market;

import com.google.gson.annotations.Expose;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Tracks price history for items in the market.
 * Provides statistical analysis of item prices over time.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
@Data
public final class PriceHistory {
    
    @Expose
    private final String itemType;
    
    @Expose
    private final List<PricePoint> pricePoints;
    
    @Expose
    private LocalDateTime lastUpdated;
    
    /**
     * Create a new price history for an item type.
     * 
     * @param itemType the item type
     */
    public PriceHistory(@NotNull final String itemType) {
        this.itemType = itemType;
        this.pricePoints = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Add a new price point.
     * 
     * @param price the price
     * @param amount the amount sold
     */
    public void addPricePoint(final double price, final int amount) {
        this.pricePoints.add(new PricePoint(price, amount, LocalDateTime.now()));
        this.lastUpdated = LocalDateTime.now();
        
        // Keep only the last 100 price points to prevent memory issues
        if (this.pricePoints.size() > 100) {
            this.pricePoints.remove(0);
        }
    }
    
    /**
     * Get the current average price.
     * 
     * @return the average price, or 0 if no data
     */
    public double getAveragePrice() {
        return this.pricePoints.stream()
            .mapToDouble(PricePoint::getPrice)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get the average price for the last N days.
     * 
     * @param days the number of days
     * @return the average price
     */
    public double getAveragePrice(final int days) {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        return this.pricePoints.stream()
            .filter(point -> point.getTimestamp().isAfter(cutoff))
            .mapToDouble(PricePoint::getPrice)
            .average()
            .orElse(0.0);
    }
    
    /**
     * Get the minimum price recorded.
     * 
     * @return the minimum price, or 0 if no data
     */
    public double getMinPrice() {
        return this.pricePoints.stream()
            .mapToDouble(PricePoint::getPrice)
            .min()
            .orElse(0.0);
    }
    
    /**
     * Get the maximum price recorded.
     * 
     * @return the maximum price, or 0 if no data
     */
    public double getMaxPrice() {
        return this.pricePoints.stream()
            .mapToDouble(PricePoint::getPrice)
            .max()
            .orElse(0.0);
    }
    
    /**
     * Get the most recent price.
     * 
     * @return the most recent price, or 0 if no data
     */
    public double getLatestPrice() {
        if (this.pricePoints.isEmpty()) {
            return 0.0;
        }
        
        return this.pricePoints.get(this.pricePoints.size() - 1).getPrice();
    }
    
    /**
     * Get the total volume traded.
     * 
     * @return the total amount traded
     */
    public int getTotalVolume() {
        return this.pricePoints.stream()
            .mapToInt(PricePoint::getAmount)
            .sum();
    }
    
    /**
     * Get the volume traded in the last N days.
     * 
     * @param days the number of days
     * @return the volume traded
     */
    public int getVolume(final int days) {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        return this.pricePoints.stream()
            .filter(point -> point.getTimestamp().isAfter(cutoff))
            .mapToInt(PricePoint::getAmount)
            .sum();
    }
    
    /**
     * Get the price trend (positive = increasing, negative = decreasing).
     * 
     * @return the price trend percentage
     */
    public double getPriceTrend() {
        if (this.pricePoints.size() < 2) {
            return 0.0;
        }
        
        final double oldPrice = this.pricePoints.get(0).getPrice();
        final double newPrice = this.getLatestPrice();
        
        if (oldPrice == 0) {
            return 0.0;
        }
        
        return ((newPrice - oldPrice) / oldPrice) * 100.0;
    }
    
    /**
     * Get recent price points.
     * 
     * @param limit the maximum number of points to return
     * @return a list of recent price points
     */
    @NotNull
    public List<PricePoint> getRecentPricePoints(final int limit) {
        final int size = this.pricePoints.size();
        final int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(this.pricePoints.subList(fromIndex, size));
    }
    
    /**
     * Get price points for a specific time period.
     * 
     * @param days the number of days back
     * @return a list of price points
     */
    @NotNull
    public List<PricePoint> getPricePoints(final int days) {
        final LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        return this.pricePoints.stream()
            .filter(point -> point.getTimestamp().isAfter(cutoff))
            .toList();
    }
    
    /**
     * Represents a single price point in history.
     */
    @Data
    public static final class PricePoint {
        
        @Expose
        private final double price;
        
        @Expose
        private final int amount;
        
        @Expose
        private final LocalDateTime timestamp;
        
        /**
         * Create a new price point.
         * 
         * @param price the price
         * @param amount the amount
         * @param timestamp the timestamp
         */
        public PricePoint(final double price, final int amount, @NotNull final LocalDateTime timestamp) {
            this.price = price;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }
}