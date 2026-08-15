package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasin;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasinCache;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.ThaumaturgicSalt;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ThaumaturgicSaltsListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        // 右键会为主手/副手各派发一次事件，副手事件重复进入会重复清空液化池
        if (e.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        final Player player = e.getPlayer();
        final ItemStack heldStack = player.getInventory().getItemInMainHand();
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(heldStack);
        final Block block = e.getClickedBlock();
        if (block != null
            && e.getAction() == Action.RIGHT_CLICK_BLOCK
            && slimefunItem instanceof ThaumaturgicSalt
        ) {
            e.setCancelled(true);
            SlimefunItem item = BlockStorage.check(block);
            if (item instanceof LiquefactionBasin) {
                liquefactionBasin(player, heldStack, item, block);
            }
        }
    }

    private void liquefactionBasin(Player player, ItemStack heldItem, SlimefunItem blockItem, Block clickedBlock) {
        if (GeneralUtils.hasPermission(player, clickedBlock, Interaction.BREAK_BLOCK)) {
            final LiquefactionBasin basin = (LiquefactionBasin) blockItem;
            final LiquefactionBasinCache cache = basin.getCacheMap().get(clickedBlock.getLocation());
            // 缓存缺失窗口（BlockPlacer 放置/首 tick 之前）不能 NPE，也不应消耗神秘盐
            if (cache == null) {
                return;
            }
            cache.emptyBasin();
            heldItem.setAmount(heldItem.getAmount() - 1);
        }
    }
}
