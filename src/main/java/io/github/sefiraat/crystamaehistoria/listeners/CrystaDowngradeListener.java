package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.slimefun.Materials;
import io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;

public class CrystaDowngradeListener implements Listener {

    @EventHandler
    public void onInteract(EntityCombustByBlockEvent e) {
        final Entity entity = e.getEntity();
        if (!(entity instanceof Item)) {
            return;
        }

        final Item item = (Item) entity;
        final ItemStack itemStack = item.getItemStack();
        // 材质门控：全部水晶（稀有度×类型）均以 PLAYER_HEAD 注册，
        // 其余材质的燃烧免 getByItem 元数据查询
        if (itemStack.getType() != Material.PLAYER_HEAD) {
            return;
        }
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);

        if (slimefunItem instanceof Crystal) {
            final Crystal crystal = (Crystal) slimefunItem;
            final int id = crystal.getRarity().getId();
            final StoryType type = crystal.getType();
            if (id != 6 && id > 1) {
                final StoryRarity rarity = StoryRarity.getById(id - 1);
                final ItemStack newItemStack = Materials.getCrystalMap().get(rarity).get(type).getItem().clone();
                final double velX = ThreadLocalRandom.current().nextDouble(-0.9, 1.1);
                final double velZ = ThreadLocalRandom.current().nextDouble(-0.9, 1.1);
                e.setCancelled(true);
                newItemStack.setAmount(itemStack.getAmount());
                item.setItemStack(newItemStack);
                item.setVelocity(new Vector(velX, 0.5, velZ));
            }
        }
    }
}
