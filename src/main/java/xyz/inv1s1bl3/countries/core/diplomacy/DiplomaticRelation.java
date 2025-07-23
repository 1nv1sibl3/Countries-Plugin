package xyz.inv1s1bl3.countries.core.diplomacy;

import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a diplomatic relationship between two countries.
 */
public class DiplomaticRelation {
    
    private final String country1;
    private final String country2;
    private RelationType relationType;
    private final long establishedDate;
    private long lastModified;
    private String description;
    
    // Trade settings
    private boolean tradeEnabled;
    private double tradeTaxRate;
    private long tradeVolume;
    
    // Alliance settings
    private boolean mutualDefense;
    private boolean sharedResources;
    private boolean freeMovement;
    
    // War settings
    private long warStartDate;
    private String warReason;
    private int warScore;
    
    public DiplomaticRelation(String country1, String country2, RelationType relationType) {
        this.country1 = country1;
        this.country2 = country2;
        this.relationType = relationType;
        this.establishedDate = System.currentTimeMillis();
        this.lastModified = establishedDate;
        this.description = "Diplomatic relationship established.";
        
        // Initialize based on relation type
        initializeRelationSettings();
    }
    
    public DiplomaticRelation(String country1, String country2, RelationType relationType,
                             long establishedDate, long lastModified, String description) {
        this.country1 = country1;
        this.country2 = country2;
        this.relationType = relationType;
        this.establishedDate = establishedDate;
        this.lastModified = lastModified;
        this.description = description != null ? description : "";
        
        initializeRelationSettings();
    }
    
    private void initializeRelationSettings() {
        switch (relationType) {
            case ALLIED -> {
                tradeEnabled = true;
                tradeTaxRate = 0.0;
                mutualDefense = true;
                sharedResources = true;
                freeMovement = true;
            }
            case FRIENDLY -> {
                tradeEnabled = true;
                tradeTaxRate = 0.05;
                mutualDefense = false;
                sharedResources = false;
                freeMovement = true;
            }
            case TRADE_PARTNER -> {
                tradeEnabled = true;
                tradeTaxRate = 0.02;
                mutualDefense = false;
                sharedResources = false;
                freeMovement = true;
            }
            case AT_WAR -> {
                tradeEnabled = false;
                tradeTaxRate = 0.0;
                mutualDefense = false;
                sharedResources = false;
                freeMovement = false;
                warStartDate = System.currentTimeMillis();
            }
            default -> {
                tradeEnabled = false;
                tradeTaxRate = 0.1;
                mutualDefense = false;
                sharedResources = false;
                freeMovement = false;
            }
        }
    }
    
    // Getters
    public String getCountry1() {
        return country1;
    }
    
    public String getCountry2() {
        return country2;
    }
    
    public RelationType getRelationType() {
        return relationType;
    }
    
    public long getEstablishedDate() {
        return establishedDate;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTradeEnabled() {
        return tradeEnabled;
    }
    
    public double getTradeTaxRate() {
        return tradeTaxRate;
    }
    
    public long getTradeVolume() {
        return tradeVolume;
    }
    
    public boolean hasMutualDefense() {
        return mutualDefense;
    }
    
    public boolean hasSharedResources() {
        return sharedResources;
    }
    
    public boolean hasFreeMovement() {
        return freeMovement;
    }
    
    public long getWarStartDate() {
        return warStartDate;
    }
    
    public String getWarReason() {
        return warReason;
    }
    
    public int getWarScore() {
        return warScore;
    }
    
    // Setters
    public void setRelationType(RelationType relationType) {
        this.relationType = relationType;
        this.lastModified = System.currentTimeMillis();
        initializeRelationSettings();
    }
    
    public void setDescription(String description) {
        this.description = description != null ? description : "";
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setTradeEnabled(boolean tradeEnabled) {
        this.tradeEnabled = tradeEnabled;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setTradeTaxRate(double tradeTaxRate) {
        this.tradeTaxRate = Math.max(0, Math.min(1, tradeTaxRate));
        this.lastModified = System.currentTimeMillis();
    }
    
    public void addTradeVolume(double amount) {
        this.tradeVolume += Math.max(0, amount);
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setMutualDefense(boolean mutualDefense) {
        this.mutualDefense = mutualDefense;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setSharedResources(boolean sharedResources) {
        this.sharedResources = sharedResources;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setFreeMovement(boolean freeMovement) {
        this.freeMovement = freeMovement;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setWarReason(String warReason) {
        this.warReason = warReason;
        this.lastModified = System.currentTimeMillis();
    }
    
    public void setWarScore(int warScore) {
        this.warScore = warScore;
        this.lastModified = System.currentTimeMillis();
    }
    
    /**
     * Check if this relation involves a specific country
     */
    public boolean involvesCountry(String countryName) {
        return country1.equalsIgnoreCase(countryName) || country2.equalsIgnoreCase(countryName);
    }
    
    /**
     * Get the other country in this relation
     */
    public String getOtherCountry(String countryName) {
        if (country1.equalsIgnoreCase(countryName)) {
            return country2;
        } else if (country2.equalsIgnoreCase(countryName)) {
            return country1;
        }
        return null;
    }
    
    /**
     * Check if relation is active (recently modified)
     */
    public boolean isActive() {
        // Consider relation active if modified within last 30 days
        return (System.currentTimeMillis() - lastModified) < 2592000000L;
    }
    
    /**
     * Get days since establishment
     */
    public long getDaysSinceEstablishment() {
        return (System.currentTimeMillis() - establishedDate) / 86400000;
    }
    
    /**
     * Get days since war started (if at war)
     */
    public long getDaysSinceWarStart() {
        if (relationType != RelationType.AT_WAR || warStartDate == 0) {
            return 0;
        }
        return (System.currentTimeMillis() - warStartDate) / 86400000;
    }
    
    /**
     * Get formatted relation information
     */
    public List<String> getFormattedInfo() {
        List<String> info = new ArrayList<>();
        
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6&l🤝 " + country1 + " ↔ " + country2));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        info.add(ChatUtils.colorize("&6Relation: &e" + relationType.getDisplayName()));
        info.add(ChatUtils.colorize("&6Established: &e" + getDaysSinceEstablishment() + " days ago"));
        info.add(ChatUtils.colorize("&6Trade Enabled: " + (tradeEnabled ? "&aYes" : "&cNo")));
        
        if (tradeEnabled) {
            info.add(ChatUtils.colorize("&6Trade Tax Rate: &e" + ChatUtils.formatPercentage(tradeTaxRate)));
            info.add(ChatUtils.colorize("&6Trade Volume: &e" + ChatUtils.formatCurrency(tradeVolume)));
        }
        
        if (relationType == RelationType.ALLIED) {
            info.add(ChatUtils.colorize("&6Mutual Defense: " + (mutualDefense ? "&aYes" : "&cNo")));
            info.add(ChatUtils.colorize("&6Shared Resources: " + (sharedResources ? "&aYes" : "&cNo")));
            info.add(ChatUtils.colorize("&6Free Movement: " + (freeMovement ? "&aYes" : "&cNo")));
        }
        
        if (relationType == RelationType.AT_WAR) {
            info.add(ChatUtils.colorize("&6War Duration: &c" + getDaysSinceWarStart() + " days"));
            info.add(ChatUtils.colorize("&6War Score: &e" + warScore));
            if (warReason != null && !warReason.isEmpty()) {
                info.add(ChatUtils.colorize("&6War Reason: &7" + warReason));
            }
        }
        
        info.add(ChatUtils.colorize("&6Status: " + (isActive() ? "&aActive" : "&7Inactive")));
        info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        
        if (!description.isEmpty()) {
            info.add(ChatUtils.colorize("&7" + description));
            info.add(ChatUtils.colorize("&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        }
        
        return info;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DiplomaticRelation relation = (DiplomaticRelation) obj;
        return (country1.equalsIgnoreCase(relation.country1) && country2.equalsIgnoreCase(relation.country2)) ||
               (country1.equalsIgnoreCase(relation.country2) && country2.equalsIgnoreCase(relation.country1));
    }
    
    @Override
    public int hashCode() {
        // Ensure consistent hash regardless of country order
        String lower1 = country1.toLowerCase();
        String lower2 = country2.toLowerCase();
        if (lower1.compareTo(lower2) > 0) {
            return (lower2 + lower1).hashCode();
        } else {
            return (lower1 + lower2).hashCode();
        }
    }
    
    @Override
    public String toString() {
        return String.format("DiplomaticRelation{%s ↔ %s: %s}", 
                           country1, country2, relationType.getDisplayName());
    }
}