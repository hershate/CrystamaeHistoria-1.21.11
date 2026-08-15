package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.MagicPaintbrush;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.LuminescenceScoop;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.covers.BlockVeil;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent;
import io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class MiscListener implements Listener {

    @EventHandler
    public void onPlaceStoriedBlock(BlockPlaceEvent e) {
        ItemStack itemStack = e.getItemInHand();
        if (itemStack.getType() != Material.AIR && StoryUtils.isStoried(itemStack)) {
            final Player player = e.getPlayer();
            player.sendMessage(ThemeType.WARNING.getColor() + "该方块已充满魔法，无法再放置于世界中");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlacerStoriedBlock(BlockPlacerPlaceEvent e) {
        ItemStack itemStack = e.getItemStack();
        if (itemStack.getType() != Material.AIR && StoryUtils.isStoried(itemStack)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onShootPaintbrush(EntityShootBowEvent e) {
        ItemStack itemStack = e.getConsumable();
        if (SlimefunItem.getByItem(itemStack) instanceof MagicPaintbrush) {
            e.setCancelled(true);
            final Entity entity = e.getEntity();
            if (entity instanceof Player) {
                entity.sendMessage(ThemeType.WARNING.getColor() + "You can't shoot a Paintbrush!");
            }
        }
    }

    @EventHandler
    public void onTryCraft(CraftItemEvent e) {
        for (ItemStack item : e.getInventory().getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                if (StoryUtils.isStoried(item)) {
                    e.setCancelled(true);
                    for (HumanEntity viewer : e.getInventory().getViewers()) {
                        viewer.sendMessage(ThemeType.WARNING.getColor() + "你不能使用该物品进行合成!");
                    }
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onTryCraft(AutoDisenchantEvent e) {
        final ItemStack itemStack = e.getItem();
        if (itemStack.getType() != Material.AIR && StoryUtils.isStoried(itemStack)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlaceCover(BlockPlaceEvent e) {
        ItemStack itemStack = e.getPlayer().getInventory().getItemInMainHand();
        if (SlimefunItem.getByItem(itemStack) instanceof BlockVeil) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void checkCooldown(PlayerInteractEvent event) {
        // 必须检查触发事件的那只手：原实现固定读主手，副手持冷却物品交互时
        // （事件为 OFF_HAND 派发）主手若无冷却则直接放行，构成冷却绕过
        final ItemStack itemStack = event.getItem();
        // 材质预检：冷却 PDC 的唯一写入方是折射透镜（SPYGLASS，RefractingLensListener），
        // 其余材质的右键交互零成本跳过，免去每次交互的 ItemMeta 克隆 + PDC 读取
        if (itemStack != null
            && itemStack.getType() == Material.SPYGLASS
            && (event.getAction() == Action.RIGHT_CLICK_AIR
            || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            && GeneralUtils.isOnCooldown(itemStack)
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void leaveSleepingBag(PlayerBedLeaveEvent event) {
        final Player player = event.getPlayer();
        final Location location = CrystamaeHistoria.getSpellMemory().getSleepingBags().remove(player.getUniqueId());

        if (location != null && location.isWorldLoaded() && location.getBlock().getType() == Material.WHITE_BED) {
            location.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuitWithSleepingBag(PlayerQuitEvent event) {
        // 玩家在睡袋中下线时 PlayerBedLeaveEvent 不保证触发，必须在此兜底清理，
        // 否则临时床会永久残留世界（睡袋可重复使用，残留床可采集 = 无中生有刷床）
        final Location location = CrystamaeHistoria.getSpellMemory().getSleepingBags().remove(event.getPlayer().getUniqueId());
        if (location != null && location.isWorldLoaded() && location.getBlock().getType() == Material.WHITE_BED) {
            location.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakSleepingBagBed(BlockBreakEvent event) {
        // 睡袋生成的临时床禁止被采集，否则成为免费床物品来源（睡袋不消耗，可无限重复）
        if (isSleepingBagBed(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onExplodeSleepingBagBed(EntityExplodeEvent event) {
        // 爆炸同样不得摧毁临时床（摧毁会产生掉落物，与直接挖掘同属刷物品路径）
        event.blockList().removeIf(this::isSleepingBagBed);
    }

    private boolean isSleepingBagBed(Block block) {
        for (Location location : CrystamaeHistoria.getSpellMemory().getSleepingBags().values()) {
            if (location.isWorldLoaded()
                && location.getWorld() == block.getWorld()
                && location.getBlockX() == block.getX()
                && location.getBlockY() == block.getY()
                && location.getBlockZ() == block.getZ()
            ) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onUseScoop(PlayerInteractEvent event) {
        // 右键会为主手/副手各派发一次事件：副手事件同样读主手荧光勺，
        // 不调光勺每点击一次会触发两次 adjustLight（亮度双重跳变），忽略副手事件
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        final Player player = event.getPlayer();
        // 材质预检：全部调光勺（4 档）材质只有 LANTERN/SOUL_LANTERN，
        // 其余物品的交互零成本跳过，免去每次交互的 getByItem 元数据查询
        final ItemStack mainHand = player.getInventory().getItemInMainHand();
        final Material mainHandType = mainHand.getType();
        if (mainHandType == Material.LANTERN || mainHandType == Material.SOUL_LANTERN) {
            final SlimefunItem item = SlimefunItem.getByItem(mainHand);
            if (item instanceof LuminescenceScoop) {
                LuminescenceScoop scoop = (LuminescenceScoop) item;
                if (scoop.isAdjustable()) {
                    scoop.adjustLight(player);
                }
            }
        }
    }
}