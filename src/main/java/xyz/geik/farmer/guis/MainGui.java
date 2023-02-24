package xyz.geik.farmer.guis;

import com.cryptomorin.xseries.XMaterial;
import de.themoep.inventorygui.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.api.handlers.FarmerMainGuiOpenEvent;
import xyz.geik.farmer.api.handlers.FarmerItemSellEvent;
import xyz.geik.farmer.helpers.Settings;
import xyz.geik.farmer.helpers.gui.GuiHelper;
import xyz.geik.farmer.helpers.gui.GroupItems;
import xyz.geik.farmer.model.Farmer;
import xyz.geik.farmer.model.inventory.FarmerItem;
import xyz.geik.farmer.model.user.FarmerPerm;

/**
 * Main gui of farmer
 * Player can sell, take and can open
 * manage gui in this gui if they have
 * permission to do.
 */
public class MainGui {

    /**
     * Gui main command
     *
     * @param player
     * @param farmer
     */
    public static void showGui(Player player, Farmer farmer) {
        // Array of gui interface
        String[] guiSetup = Main.getLangFile().getList("Gui.interface").toArray(String[]::new);
        // Gui object
        InventoryGui gui = new InventoryGui(Main.getInstance(), null, Main.getLangFile().getText("Gui.guiName"), guiSetup);
        // Fills empty spaces on  gui
        gui.setFiller(GuiHelper.getFiller());
        // Manage Icon element
        gui.addElement(new StaticGuiElement('m',
                // Manage item
                GuiHelper.getManageItemOnMain(farmer),
                1,
                // Event
                click -> {
                    // If player has admin perm or owner of farmer
                    if (player.hasPermission("farmer.admin")
                            || farmer.getOwnerUUID().equals(player.getUniqueId()))
                        ManageGui.showGui(player, farmer);
                    return true;
                })
        );
        // Help item
        gui.addElement(GuiHelper.createGuiElement("Gui.help", 'h'));

        // Item group which farmer collects
        GuiElementGroup group = new GuiElementGroup('g');
        // Foreach item list
        for (FarmerItem item : farmer.getInv().getItems()) {
            // Element of grup there can x amount of i
            group.addElement(new DynamicGuiElement('i', (viewer) -> {
                return new StaticGuiElement('i',
                        GroupItems.getGroupItem(farmer, item),
                        1,
                        click -> {
                            // If player has admin perm or member of farmer
                            // Because member can take item or sell from farmer
                            // Otherwise if user has coop role then they can only
                            // Look inventory of farmer
                            if (player.hasPermission("farmer.admin") ||
                                    farmer.getUsers().stream().anyMatch(user -> (
                                    !user.getPerm().equals(FarmerPerm.COOP)
                                            && user.getName().equalsIgnoreCase(player.getName())))) {
                                // XMaterial check for old version
                                XMaterial material = XMaterial.matchXMaterial(click.getEvent().getCurrentItem());
                                FarmerItem slotItem = farmer.getInv().getStockedItem(material);
                                // Sells all stock of an item
                                if (click.getType().equals(ClickType.SHIFT_RIGHT)) {
                                    if (slotItem.getAmount() == 0)
                                        return true;
                                    // Calculating tax, profit and selling price
                                    double sellPrice = slotItem.getPrice() * slotItem.getAmount();
                                    double profit = (farmer.getLevel().getTax() > 0)
                                            ? sellPrice-(sellPrice*farmer.getLevel().getTax()/100)
                                            : sellPrice;
                                    double tax = (sellPrice == profit) ? 0 : sellPrice*farmer.getLevel().getTax()/100;
                                    Main.getEcon().depositPlayer(player, profit);
                                    slotItem.setAmount(0);

                                    // If configuration has deposit tax to
                                    // defined player then it will deposit it
                                    // to player.
                                    if (Settings.depositTax)
                                        Main.getEcon()
                                                .depositPlayer(Settings.taxUser, tax);

                                    // Calls FarmerItemSellEvent
                                    FarmerItemSellEvent itemSellEvent = new FarmerItemSellEvent(farmer, XMaterial.matchXMaterial(click.getEvent().getCurrentItem()));
                                    Bukkit.getPluginManager().callEvent(itemSellEvent);

                                    player.sendMessage(Main.getLangFile().getText("sellComplete")
                                        .replace("{money}", roundDouble(profit))
                                        .replace("{tax}", roundDouble(tax)));
                                }
                                // Withdraw item
                                else {
                                    // If inventory full returns
                                    if (invFull(player)) {
                                        player.sendMessage(Main.getLangFile().getText("inventoryFull"));
                                        return true;
                                    }
                                    long count;
                                    // Left click can only have one stack of item
                                    // But if there is less than one stack
                                    // Then overriding this amount to count.
                                    if (click.getType().equals(ClickType.LEFT))
                                        count = (slotItem.getAmount() >= 64) ? 64 : slotItem.getAmount();

                                    // Withdraws max player can take from stocked amount
                                    else if (click.getType().equals(ClickType.RIGHT)) {
                                        int playerEmpty = getEmptySlots(player) * material.parseMaterial().getMaxStackSize();
                                        count = (slotItem.getAmount() >= playerEmpty)
                                                ? playerEmpty
                                                : slotItem.getAmount();
                                    }
                                    else return true;

                                    if (count == 0)
                                        return true;
                                    ItemStack returnItem = material.parseItem();
                                    returnItem.setAmount((int) count);
                                    player.getInventory().addItem(returnItem);
                                    slotItem.negateAmount(count);
                                }
                                gui.draw();
                            }
                            return true;
                        });
            }));
        }
        // Adding everything to gui and opening
        gui.addElement(group);
        gui.addElement(GuiHelper.createNextPage());
        gui.addElement(GuiHelper.createPreviousPage());
        FarmerMainGuiOpenEvent guiOpenEvent = new FarmerMainGuiOpenEvent(player, farmer, gui);
        Bukkit.getPluginManager().callEvent(guiOpenEvent);
        if (!guiOpenEvent.isCancelled())
            gui.show(player);
    }

    /**
     * Chkecs if player has slot in inventory
     *
     * @param p
     * @return
     */
    private static boolean invFull(@NotNull Player p) {
        return p.getInventory().firstEmpty() == -1;
    }

    /**
     * Gets all the empty slots
     *
     * @param player
     * @return
     */
    private static int getEmptySlots(@NotNull Player player) {
        int count = 0;
        for (ItemStack i : player.getInventory()) {
            if (i == null) {
                count++;
            } else
                continue;
        }
        return count;
    }

    /**
     * Rounds double for display good.
     * It shown as #.## but if this isn't exist
     * It shown as #.######## something like that.
     *
     * @param value
     * @return
     */
    private static @NotNull String roundDouble(double value) {
        long factor = (long) Math.pow(10, 2);
        value = value * factor;
        long tmp = Math.round(value);
        double result = (double) tmp / factor;
        return String.valueOf( result );
    }
}