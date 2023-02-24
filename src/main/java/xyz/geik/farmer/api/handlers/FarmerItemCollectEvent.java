package xyz.geik.farmer.api.handlers;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import xyz.geik.farmer.model.Farmer;

/**
 * FarmerItemCollectEvent
 */
@Getter
public class FarmerItemCollectEvent extends Event {

    // Farmer object
    private Farmer farmer;
    private ItemStack item;
    private int collectAmount;

    // is cancelled boolean
    private boolean isCancelled = false;

    // Main constructor of event
    public FarmerItemCollectEvent(Farmer farmer, ItemStack item, int collectAmount) {
        this.farmer = farmer;
        this.item = item;
        this.collectAmount = collectAmount;
    }

    // Only setter method of event
    public void setCancelled(boolean arg0) {
        this.isCancelled = arg0;
    }

    /**
     * Spigot handlers requirements
     *
     * @return
     */
    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
