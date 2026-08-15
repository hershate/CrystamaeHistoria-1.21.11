package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.CrystamageSatchel;
import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class SatchelListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(EntityPickupItemEvent e) {
        final Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }

        final Player player = (Player) entity;
        final Item item = e.getItem();
        final ItemStack itemStack = item.getItemStack();
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);

        if (slimefunItem instanceof Crystal) {
            final Crystal crystal = (Crystal) slimefunItem;

            for (ItemStack possibleSatchel : player.getInventory().getContents()) {
                final SlimefunItem satchelSfItem = SlimefunItem.getByItem(possibleSatchel);

                // Try to pick up the item into a dank first
                if (satchelSfItem instanceof CrystamageSatchel) {
                    final CrystamageSatchel crystamageSatchel = (CrystamageSatchel) satchelSfItem;

                    if (possibleSatchel.getAmount() > 1) {
                        player.sendMessage(ThemeType.WARNING.getColor() + "你拥有堆叠的水晶收纳袋, 需要将他们分开才可以正常使用.");
                        return;
                    }

                    if (crystamageSatchel.tryAddItem(possibleSatchel, itemStack, crystal)) {
                        final java.awt.Color baseColor = ThemeType.getByType(crystal.getType()).getColor().getColor();
                        final Color color = Color.fromRGB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue());
                        final Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1);

                        ParticleUtils.displayParticleEffect(item, 0.4, 10, dustOptions);
                        item.remove();
                        e.setCancelled(true);
                        return;
                    } else {
                        // tryAddItem 失败有两种原因（未初始化 / 收纳袋等级不足），
                        // 原文案只提示初始化，与实现不符
                        player.sendMessage(ThemeType.WARNING.getColor() + "水晶收纳袋无法收纳该水晶: 尚未初始化(需要打开一次)或收纳袋等级不足.");
                    }
                }
            }
        }
    }
}
