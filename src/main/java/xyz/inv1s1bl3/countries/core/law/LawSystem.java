package xyz.inv1s1bl3.countries.core.law;

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
 * Manages the law and order system for countries.
 */
public class LawSystem {
    
    private final CountriesPlugin plugin;
    private final Map<String, List<Law>> countryLaws; // Country name -> Laws
    private final Map<Integer, Crime> crimes; // Crime ID -> Crime
    private final Map<UUID, List<Integer>> playerCrimes; // Player UUID -> Crime IDs
    private final Map<UUID, Long> jailedPlayers; // Player UUID -> Release time
    private final Map<UUID, Double> bounties; // Player UUID -> Bounty amount
    
    public LawSystem(CountriesPlugin plugin) {
        this.plugin = plugin;
        this.countryLaws = new ConcurrentHashMap<>();
        this.crimes = new ConcurrentHashMap<>();
        this.playerCrimes = new ConcurrentHashMap<>();
        this.jailedPlayers = new ConcurrentHashMap<>();
        this.bounties = new ConcurrentHashMap<>();
    }
    
    /**
     * Initialize the law system
     */
    public void initialize() {
        loadLaws();
        loadCrimes();
        
        plugin.debug("Law system initialized");
    }
    
    /**
     * Load all laws from storage
     */
    private void loadLaws() {
        // This will be implemented when storage system is complete
        plugin.debug("Law system loading ready");
    }
    
    /**
     * Load all crimes from storage
     */
    private void loadCrimes() {
        // This will be implemented when storage system is complete
        plugin.debug("Crime system loading ready");
    }
    
    /**
     * Create a new law for a country
     */
    public boolean createLaw(String countryName, String title, String description, 
                           double fineAmount, int jailTime, UUID creatorUUID) {
        Country country = plugin.getCountryManager().getCountry(countryName);
        if (country == null) {
            return false;
        }
        
        // Check if creator has permission
        Citizen creator = country.getCitizen(creatorUUID);
        if (creator == null || !creator.getRole().canManageEconomy()) {
            return false;
        }
        
        try {
            int lawId = generateLawId();
            Law law = new Law(lawId, countryName, title, description);
            law.setFineAmount(fineAmount);
            law.setJailTime(jailTime);
            
            countryLaws.computeIfAbsent(countryName.toLowerCase(), k -> new ArrayList<>()).add(law);
            
            // Save law
            saveLaw(law);
            
            plugin.debug("Law created: " + title + " for country: " + countryName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating law", e);
            return false;
        }
    }
    
    /**
     * Report a crime
     */
    public boolean reportCrime(UUID criminalUUID, String criminalName, UUID victimUUID, 
                             String victimName, CrimeType crimeType, String countryName, 
                             String description, String evidence) {
        try {
            int crimeId = generateCrimeId();
            Crime crime = new Crime(crimeId, criminalUUID, criminalName, victimUUID, 
                                  victimName, crimeType, countryName, description, evidence);
            
            crimes.put(crimeId, crime);
            playerCrimes.computeIfAbsent(criminalUUID, k -> new ArrayList<>()).add(crimeId);
            
            // Save crime
            saveCrime(crime);
            
            // Notify law enforcement
            notifyLawEnforcement(countryName, crime);
            
            plugin.debug("Crime reported: " + crimeType.getDisplayName() + " by " + criminalName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reporting crime", e);
            return false;
        }
    }
    
    /**
     * Arrest a player for a crime
     */
    public boolean arrestPlayer(UUID criminalUUID, UUID officerUUID, int crimeId) {
        Crime crime = crimes.get(crimeId);
        if (crime == null || !crime.getCriminalUUID().equals(criminalUUID)) {
            return false;
        }
        
        Country country = plugin.getCountryManager().getCountry(crime.getCountryName());
        if (country == null) {
            return false;
        }
        
        // Check if officer has permission
        Citizen officer = country.getCitizen(officerUUID);
        if (officer == null || !officer.getRole().canKick()) {
            return false;
        }
        
        try {
            // Mark crime as arrested
            crime.setArrested(true);
            crime.setArrestingOfficer(officerUUID);
            
            // Calculate jail time
            int jailMinutes = calculateJailTime(crime);
            long releaseTime = System.currentTimeMillis() + (jailMinutes * 60000L);
            
            crime.setReleaseDate(releaseTime);
            jailedPlayers.put(criminalUUID, releaseTime);
            
            // Issue fine
            issueFine(criminalUUID, crime.getFineAmount(), crime.getCountryName());
            
            // Notify players
            Player criminal = Bukkit.getPlayer(criminalUUID);
            Player officerPlayer = Bukkit.getPlayer(officerUUID);
            
            if (criminal != null) {
                ChatUtils.sendPrefixedConfigMessage(criminal, "law.arrested", 
                        crime.getCrimeType().getDisplayName());
                ChatUtils.sendPrefixedConfigMessage(criminal, "law.jail-time", jailMinutes);
            }
            
            if (officerPlayer != null) {
                ChatUtils.sendPrefixedConfigMessage(officerPlayer, "law.arrest-made", 
                        crime.getCriminalName(), crime.getCrimeType().getDisplayName());
            }
            
            // Save updated crime
            saveCrime(crime);
            
            plugin.debug("Player arrested: " + crime.getCriminalName() + " for " + 
                        crime.getCrimeType().getDisplayName());
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error arresting player", e);
            return false;
        }
    }
    
    /**
     * Issue a fine to a player
     */
    public boolean issueFine(UUID playerUUID, double amount, String countryName) {
        BankAccount playerAccount = plugin.getEconomyManager().getPlayerAccount(playerUUID);
        BankAccount countryAccount = plugin.getEconomyManager().getCountryAccount(countryName);
        
        if (playerAccount == null || countryAccount == null) {
            return false;
        }
        
        // Transfer fine amount
        if (plugin.getEconomyManager().transferMoney(playerAccount, countryAccount, amount, 
                TransactionType.FINE_PAYMENT, "Legal fine payment")) {
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                ChatUtils.sendPrefixedConfigMessage(player, "law.fine-issued", 
                        ChatUtils.formatCurrency(amount), "crime");
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Release a player from jail
     */
    public boolean releasePlayer(UUID playerUUID) {
        if (!isPlayerInJail(playerUUID)) {
            return false;
        }
        
        jailedPlayers.remove(playerUUID);
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            ChatUtils.sendPrefixedConfigMessage(player, "law.released");
        }
        
        plugin.debug("Player released from jail: " + playerUUID);
        return true;
    }
    
    /**
     * Set a bounty on a player
     */
    public boolean setBounty(UUID targetUUID, double amount, UUID setterUUID) {
        if (amount <= 0) {
            return false;
        }
        
        BankAccount setterAccount = plugin.getEconomyManager().getPlayerAccount(setterUUID);
        if (setterAccount == null || !setterAccount.hasBalance(amount)) {
            return false;
        }
        
        // Withdraw bounty amount
        if (plugin.getEconomyManager().withdrawMoney(setterAccount, amount, 
                TransactionType.SYSTEM_ADJUSTMENT, "Bounty set")) {
            
            bounties.put(targetUUID, bounties.getOrDefault(targetUUID, 0.0) + amount);
            
            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null) {
                ChatUtils.sendPrefixedConfigMessage(target, "law.bounty-set", 
                        ChatUtils.formatCurrency(amount), target.getName());
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Claim a bounty
     */
    public boolean claimBounty(UUID targetUUID, UUID claimerUUID) {
        Double bountyAmount = bounties.get(targetUUID);
        if (bountyAmount == null || bountyAmount <= 0) {
            return false;
        }
        
        BankAccount claimerAccount = plugin.getEconomyManager().getPlayerAccount(claimerUUID);
        if (claimerAccount == null) {
            return false;
        }
        
        // Pay bounty
        if (plugin.getEconomyManager().depositMoney(claimerAccount, bountyAmount, 
                TransactionType.SYSTEM_ADJUSTMENT, "Bounty claimed")) {
            
            bounties.remove(targetUUID);
            
            Player claimer = Bukkit.getPlayer(claimerUUID);
            if (claimer != null) {
                String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
                ChatUtils.sendPrefixedConfigMessage(claimer, "law.bounty-claimed", 
                        targetName, ChatUtils.formatCurrency(bountyAmount));
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a player is currently in jail
     */
    public boolean isPlayerInJail(UUID playerUUID) {
        Long releaseTime = jailedPlayers.get(playerUUID);
        if (releaseTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= releaseTime) {
            // Auto-release if time is up
            jailedPlayers.remove(playerUUID);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get remaining jail time for a player
     */
    public long getRemainingJailTime(UUID playerUUID) {
        Long releaseTime = jailedPlayers.get(playerUUID);
        if (releaseTime == null) {
            return 0;
        }
        
        long remaining = releaseTime - System.currentTimeMillis();
        return Math.max(0, remaining / 60000); // Convert to minutes
    }
    
    /**
     * Get all laws for a country
     */
    public List<Law> getCountryLaws(String countryName) {
        return new ArrayList<>(countryLaws.getOrDefault(countryName.toLowerCase(), new ArrayList<>()));
    }
    
    /**
     * Get all crimes for a player
     */
    public List<Crime> getPlayerCrimes(UUID playerUUID) {
        List<Integer> crimeIds = playerCrimes.getOrDefault(playerUUID, new ArrayList<>());
        List<Crime> playerCrimeList = new ArrayList<>();
        
        for (Integer crimeId : crimeIds) {
            Crime crime = crimes.get(crimeId);
            if (crime != null) {
                playerCrimeList.add(crime);
            }
        }
        
        return playerCrimeList;
    }
    
    /**
     * Get bounty amount for a player
     */
    public double getBounty(UUID playerUUID) {
        return bounties.getOrDefault(playerUUID, 0.0);
    }
    
    /**
     * Calculate jail time for a crime
     */
    private int calculateJailTime(Crime crime) {
        int baseTime = crime.getCrimeType().getBaseJailTime();
        int multiplier = plugin.getConfigManager().getConfig().getInt("law.jail-time-multiplier", 5);
        return baseTime * multiplier;
    }
    
    /**
     * Notify law enforcement of a crime
     */
    private void notifyLawEnforcement(String countryName, Crime crime) {
        Country country = plugin.getCountryManager().getCountry(countryName);
        if (country == null) {
            return;
        }
        
        for (Citizen citizen : country.getCitizens()) {
            if (citizen.getRole().canKick()) { // Officers can arrest
                Player officer = Bukkit.getPlayer(citizen.getPlayerUUID());
                if (officer != null) {
                    ChatUtils.sendPrefixedMessage(officer, 
                            "&c[Law Enforcement] Crime reported: " + crime.getCrimeType().getDisplayName() + 
                            " by " + crime.getCriminalName());
                }
            }
        }
    }
    
    /**
     * Generate unique law ID
     */
    private int generateLawId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }
    
    /**
     * Generate unique crime ID
     */
    private int generateCrimeId() {
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE) + crimes.size();
    }
    
    /**
     * Save law to storage
     */
    private void saveLaw(Law law) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving law: " + law.getTitle());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving law: " + law.getTitle(), e);
            }
        });
    }
    
    /**
     * Save crime to storage
     */
    private void saveCrime(Crime crime) {
        plugin.getDataManager().executeAsync(() -> {
            try {
                plugin.debug("Saving crime: " + crime.getCrimeId());
                // This will be implemented when storage system is complete
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error saving crime: " + crime.getCrimeId(), e);
            }
        });
    }
    
    /**
     * Get law system statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        int totalLaws = countryLaws.values().stream().mapToInt(List::size).sum();
        long activeCrimes = crimes.values().stream().filter(c -> !c.isConvicted()).count();
        
        stats.put("total_laws", totalLaws);
        stats.put("total_crimes", crimes.size());
        stats.put("active_crimes", activeCrimes);
        stats.put("jailed_players", jailedPlayers.size());
        stats.put("active_bounties", bounties.size());
        
        return stats;
    }
}