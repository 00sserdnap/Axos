package cl.pandress.modules.rankup.menus;

import cl.pandress.utils.ChatUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RankTopMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatUtil.color("&8» &6&lTop 10 Global &8«"))) {
            event.setCancelled(true);
        }
    }
}