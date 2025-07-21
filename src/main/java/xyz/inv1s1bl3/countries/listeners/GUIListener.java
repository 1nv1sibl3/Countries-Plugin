package xyz.inv1s1bl3.countries.listeners;

import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import xyz.inv1s1bl3.countries.CountriesPlugin;

/**
 * Handles GUI-related events for the Countries plugin.
 * Will be implemented in Phase 5 - GUI System.
 * 
 * @author inv1s1bl3
 * @version 1.0.0
 */
public final class GUIListener implements Listener {
    
    private final CountriesPlugin plugin;
    
    public GUIListener(@NotNull final CountriesPlugin plugin) {
        this.plugin = plugin;
    }
    
    // TODO: Implement inventory click events
    // TODO: Implement inventory close events
    // TODO: Implement GUI navigation
}