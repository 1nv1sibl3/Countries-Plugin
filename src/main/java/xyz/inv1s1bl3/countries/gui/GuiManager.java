package xyz.inv1s1bl3.countries.gui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import xyz.inv1s1bl3.countries.CountriesPlugin;

/**
 * Manager for GUI interfaces
 */
@Getter
@RequiredArgsConstructor
public final class GuiManager {
    
    private final CountriesPlugin plugin;
    
    /**
     * Initialize the GUI manager
     */
    public void initialize() {
        this.plugin.getLogger().info("Initializing GUI Manager...");
        
        // TODO: Initialize GUI systems when implemented
        
        this.plugin.getLogger().info("GUI Manager initialized successfully!");
    }
}