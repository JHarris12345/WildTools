package com.bgsoftware.wildtools.listeners;

import com.bgsoftware.wildtools.WildToolsPlugin;
import com.bgsoftware.wildtools.api.objects.tools.Tool;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class CraftingListener implements Listener {

    private final WildToolsPlugin plugin;

    public CraftingListener(WildToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        if (e.getInventory().getMatrix() == null || e.getInventory().getResult() == null) {
            return;
        }

        for (ItemStack itemStack : e.getInventory().getMatrix()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }

            Tool tool = plugin.getToolsManager().getTool(itemStack);

            if (tool != null) {
                e.getInventory().setResult(null);
                return;
            }
        }
    }

}
