package xyz.inv1s1bl3.countries.core.diplomacy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.countries.CountriesPlugin;
import xyz.inv1s1bl3.countries.core.country.Country;
import xyz.inv1s1bl3.countries.core.country.Citizen;
import xyz.inv1s1bl3.countries.core.economy.BankAccount;
import xyz.inv1s1bl3.countries.core.economy.TransactionType;
import xyz.inv1s1bl3.countries.utils.ChatUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages diplomatic relations and trade agreements between countries.
 */
public class DiplomacyManager {
    
    private final CountriesPlugin plugin;
    private final Map<String, DiplomaticRelation> relations; // Relation key -> Relation
    private final Map<String, TradeAgreement> tradeAgreements; // Agreement key -> Agreement
    private final Map<String, Set<UUID>> pendingProposals; // Country -> Set of proposer UUIDs
    
    public DiplomacyManager(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.relations = new ConcurrentHashMap<>();
        this.tradeAgreements = new ConcurrentHashMap<>();
        this.pendingProposals = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the diplomacy system
     */
    public void initialize() {
        loadRelations();
        loadTradeAgreements();
        
        plugin.debug("Diplomacy system initialized");
    }
    
    /**
     * Load all diplomatic relations from storage
     */
    private void loadRelations() {
        // This will be implemented when storage system is complete
        plugin.debug("Diplomatic relations loading system ready");
    }
    
    /**
     * Load all trade agreements from storage
     */
    private void loadTradeAgreements() {
        // This will be implemented when storage system is complete
        plugin.debug("Trade agreements loading system ready");
    }
    
    /**
     * Propose an alliance between two countries
     */
    public boolean proposeAlliance(UUID proposerUUID, String proposerCountry, String targetCountry) {
        Country proposer = plugin.getCountryManager().getCountry(proposerCountry);
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        
        if (proposer == null || target == null) {
            return false;
        }
        
        // Check if proposer has permission
        Citizen citizen = proposer.getCitizen(proposerUUID);
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            return false;
        }
        
        // Check alliance cost
        double allianceCost = plugin.getConfigManager().getConfig()
                .getDouble("diplomacy.alliance-cost", 5000.0);
        
        BankAccount proposerAccount = plugin.getEconomyManager().getCountryAccount(proposerCountry);
        if (allianceCost > 0 && !proposerAccount.hasBalance(allianceCost)) {
            return false;
        }
        
        // Check if already allied
        DiplomaticRelation existing = getRelation(proposerCountry, targetCountry);
        if (existing != null && existing.getRelationType() == RelationType.ALLIED) {
            return false;
        }
        
        // Check max allies
        int maxAllies = plugin.getConfigManager().getConfig()
                .getInt("diplomacy.max-allies", 10);
        
        if (getAllies(proposerCountry).size() >= maxAllies) {
            return false;
        }
        
        try {
            // Add pending proposal
            pendingProposals.computeIfAbsent(targetCountry.toLowerCase(), 
                    k -> ConcurrentHashMap.newKeySet()).add(proposerUUID);
            
            // Charge alliance cost
            if (allianceCost > 0) {
                plugin.getEconomyManager().withdrawMoney(proposerAccount, allianceCost, 
                        TransactionType.ALLIANCE_FEE, "Alliance proposal to " + targetCountry);
            }
            
            // Notify target country members
            notifyCountryMembers(target, "Alliance proposed by " + proposerCountry + "!");
            
            plugin.debug("Alliance proposed: " + proposerCountry + " -> " + targetCountry);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error proposing alliance", e);
            return false;
        }
    }
    
    /**
     * Accept an alliance proposal
     */
    public boolean acceptAlliance(UUID accepterUUID, String accepterCountry, String proposerCountry) {
        Country accepter = plugin.getCountryManager().getCountry(accepterCountry);
        Country proposer = plugin.getCountryManager().getCountry(proposerCountry);
        
        if (accepter == null || proposer == null) {
            return false;
        }
        
        // Check if accepter has permission
        Citizen citizen = accepter.getCitizen(accepterUUID);
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            return false;
        }
        
        // Check if there's a pending proposal
        Set<UUID> proposals = pendingProposals.get(accepterCountry.toLowerCase());
        if (proposals == null || proposals.isEmpty()) {
            return false;
        }
        
        try {
            // Create or update diplomatic relation
            String relationKey = createRelationKey(proposerCountry, accepterCountry);
            DiplomaticRelation relation = new DiplomaticRelation(proposerCountry, accepterCountry, RelationType.ALLIED);
            relations.put(relationKey, relation);
            
            // Remove pending proposal
            proposals.clear();
            
            // Notify both countries
            notifyCountryMembers(proposer, "Alliance with " + accepterCountry + " has been accepted!");
            notifyCountryMembers(accepter, "Alliance with " + proposerCountry + " has been formed!");
            
            // Save relation
            saveRelation(relation);
            
            plugin.debug("Alliance formed: " + proposerCountry + " <-> " + accepterCountry);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error accepting alliance", e);
            return false;
        }
    }
    
    /**
     * Declare war on another country
     */
    public boolean declareWar(UUID declarerUUID, String declarerCountry, String targetCountry, String reason) {
        Country declarer = plugin.getCountryManager().getCountry(declarerCountry);
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        
        if (declarer == null || target == null) {
            return false;
        }
        
        // Check if declarer has permission
        Citizen citizen = declarer.getCitizen(declarerUUID);
        if (citizen == null || !citizen.getRole().canDisband()) {
            return false;
        }
        
        // Check war declaration cost
        double warCost = plugin.getConfigManager().getConfig()
                .getDouble("diplomacy.war-declaration-cost", 10000.0);
        
        BankAccount declarerAccount = plugin.getEconomyManager().getCountryAccount(declarerCountry);
        if (warCost > 0 && !declarerAccount.hasBalance(warCost)) {
            return false;
        }
        
        try {
            // Create or update diplomatic relation
            String relationKey = createRelationKey(declarerCountry, targetCountry);
            DiplomaticRelation relation = new DiplomaticRelation(declarerCountry, targetCountry, RelationType.AT_WAR);
            relation.setWarReason(reason != null ? reason : "No reason given");
            relations.put(relationKey, relation);
            
            // Charge war cost
            if (warCost > 0) {
                plugin.getEconomyManager().withdrawMoney(declarerAccount, warCost, 
                        TransactionType.WAR_DECLARATION, "War declaration against " + targetCountry);
            }
            
            // Notify both countries
            notifyCountryMembers(declarer, "War declared against " + targetCountry + "!");
            notifyCountryMembers(target, "War declared by " + declarerCountry + "!");
            
            // Save relation
            saveRelation(relation);
            
            plugin.debug("War declared: " + declarerCountry + " -> " + targetCountry);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error declaring war", e);
            return false;
        }
    }
    
    /**
     * Set neutral relations with another country
     */
    public boolean setNeutral(UUID setterUUID, String setterCountry, String targetCountry) {
        Country setter = plugin.getCountryManager().getCountry(setterCountry);
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        
        if (setter == null || target == null) {
            return false;
        }
        
        // Check if setter has permission
        Citizen citizen = setter.getCitizen(setterUUID);
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            return false;
        }
        
        try {
            String relationKey = createRelationKey(setterCountry, targetCountry);
            DiplomaticRelation relation = relations.get(relationKey);
            
            if (relation == null) {
                relation = new DiplomaticRelation(setterCountry, targetCountry, RelationType.NEUTRAL);
            } else {
                relation.setRelationType(RelationType.NEUTRAL);
            }
            
            relations.put(relationKey, relation);
            
            // Notify both countries
            notifyCountryMembers(setter, "Relations with " + targetCountry + " set to neutral.");
            notifyCountryMembers(target, "Relations with " + setterCountry + " set to neutral.");
            
            // Save relation
            saveRelation(relation);
            
            plugin.debug("Neutral relations set: " + setterCountry + " <-> " + targetCountry);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting neutral relations", e);
            return false;
        }
    }
    
    /**
     * Propose a trade agreement
     */
    public boolean proposeTradeAgreement(UUID proposerUUID, String proposerCountry, 
                                       String targetCountry, double taxRate, long durationDays) {
        Country proposer = plugin.getCountryManager().getCountry(proposerCountry);
        Country target = plugin.getCountryManager().getCountry(targetCountry);
        
        if (proposer == null || target == null) {
            return false;
        }
        
        // Check if proposer has permission
        Citizen citizen = proposer.getCitizen(proposerUUID);
        if (citizen == null || !citizen.getRole().canManageEconomy()) {
            return false;
        }
        
        // Check if trade is enabled in diplomacy
        if (!plugin.getConfigManager().getConfig().getBoolean("diplomacy.enable-trade", true)) {
            return false;
        }
        
        try {
            String agreementKey = createRelationKey(proposerCountry, targetCountry);
            TradeAgreement agreement = new TradeAgreement(proposerCountry, targetCountry, taxRate, durationDays);
            agreement.setActive(false); // Inactive until accepted
            tradeAgreements.put(agreementKey, agreement);
            
            // Add pending proposal
            pendingProposals.computeIfAbsent(targetCountry.toLowerCase(), 
                    k -> ConcurrentHashMap.newKeySet()).add(proposerUUID);
            
            // Notify target country
            notifyCountryMembers(target, "Trade agreement proposed by " + proposerCountry + "!");
            
            // Save agreement
            saveTradeAgreement(agreement);
            
            plugin.debug("Trade agreement proposed: " + proposerCountry + " -> " + targetCountry);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error proposing trade agreement", e);
            return false;
        }
    }
    
    /**
     * Get diplomatic relation between two countries
     */
    public DiplomaticRelation getRelation(String country1, String country2) {
        String relationKey = createRelationKey(country1, country2);
        return relations.get(relationKey);
    }
    
    /**
     * Get trade agreement between two countries
     */
    public TradeAgreement getTradeAgreement(String country1, String country2) {
        String agreementKey = createRelationKey(country1, country2);
        return tradeAgreements.get(agreementKey);
    }
    
    /**
     * Get all allies of a country
     */
    public Set<String> getAllies(String countryName) {
        Set<String> allies = new HashSet<>();
        
        for (DiplomaticRelation relation : relations.values()) {
            if (relation.getRelationType() == RelationType.ALLIED && 
                relation.involvesCountry(countryName)) {
                String ally = relation.getOtherCountry(countryName);
                if (ally != null) {
                    allies.add(ally);
                }
            }
        }
        
        return allies;
    }
    
    /**
     * Get all enemies of a country
     */
    public Set<String> getEnemies(String countryName) {
        Set<String> enemies = new HashSet<>();
        
        for (DiplomaticRelation relation : relations.values()) {
            if (relation.getRelationType() == RelationType.AT_WAR && 
                relation.involvesCountry(countryName)) {
                String enemy = relation.getOtherCountry(countryName);
                if (enemy != null) {
                    enemies.add(enemy);
                }
            }
        }
        
        return enemies;
    }
    
    /**
     * Get all diplomatic relations for a country
     */
    public Set<DiplomaticRelation> getCountryRelations(String countryName) {
        Set<DiplomaticRelation> countryRelations = new HashSet<>();
        
        for (DiplomaticRelation relation : relations.values()) {
            if (relation.involvesCountry(countryName)) {
                countryRelations.add(relation);
            }
        }
        
        return countryRelations;
    }
    
    /**
     * Get all trade agreements for a country
     */
    public Set<TradeAgreement> getCountryTradeAgreements(String countryName) {
        Set<TradeAgreement> countryAgreements = new HashSet<>();
        
        for (TradeAgreement agreement : tradeAgreements.values()) {
            if (agreement.involvesCountry(countryName)) {
                countryAgreements.add(agreement);
            }
        }
        
        return countryAgreements;
    }
    
    /**
     * Check if two countries can trade
     */
    public boolean canTrade(String country1, String country2) {
        DiplomaticRelation relation = getRelation(country1, country2);
        if (relation != null && !relation.getRelationType().allowsTrade()) {
            return false;
        }
        
        TradeAgreement agreement = getTradeAgreement(country1, country2);
        return agreement != null && agreement.isActive();
    }
    
    /**
     * Check if citizens can travel between countries
     */
    public boolean canTravel(String fromCountry, String toCountry) {
        DiplomaticRelation relation = getRelation(fromCountry, toCountry);
        return relation == null || relation.getRelationType().allowsTravel();
    }
    
    /**
     * Create a unique key for relations/agreements
     */
    private String createRelationKey(String country1, String country2) {
        String lower1 = country1.toLowerCase();
        String lower2 = country2.toLowerCase();
        
        // Ensure consistent ordering
        if (lower1.compareTo(lower2) > 0) {
            return lower2 + ":" + lower1;
        } else {
            return lower1 + ":" + lower2;
        }
    }
    
    /**
     * Notify all members of a country
     */
    private void notifyCountryMembers(Country country, String message) {
        for (Citizen citizen : country.getCitizens()) {
            Player player = Bukkit.getPlayer(citizen.getPlayerUUID());
            if (player != null) {
                ChatUtils.sendPrefixedMessage(player, "&6[Diplomacy] &e" + message);
            }
        }
    }
    
    /**
     * Save a diplomatic relation to storage
     */
    private void saveRelation(DiplomaticRelation relation) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving diplomatic relation: " + relation.getCountry1() + " <-> " + relation.getCountry2());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving diplomatic relation", e);
            }
        });
    }
    
    /**
     * Save a trade agreement to storage
     */
    private void saveTradeAgreement(TradeAgreement agreement) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving trade agreement: " + agreement.getCountry1() + " <-> " + agreement.getCountry2());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving trade agreement", e);
            }
        });
    }
    
    /**
     * Get diplomacy statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long alliances = relations.values().stream()
                .mapToLong(r -> r.getRelationType() == RelationType.ALLIED ? 1 : 0)
                .sum();
        
        long wars = relations.values().stream()
                .mapToLong(r -> r.getRelationType() == RelationType.AT_WAR ? 1 : 0)
                .sum();
        
        long activeAgreements = tradeAgreements.values().stream()
                .mapToLong(a -> a.isActive() ? 1 : 0)
                .sum();
        
        stats.put("total_relations", relations.size());
        stats.put("alliances", alliances);
        stats.put("wars", wars);
        stats.put("trade_agreements", tradeAgreements.size());
        stats.put("active_trade_agreements", activeAgreements);
        
        return stats;
    }
}