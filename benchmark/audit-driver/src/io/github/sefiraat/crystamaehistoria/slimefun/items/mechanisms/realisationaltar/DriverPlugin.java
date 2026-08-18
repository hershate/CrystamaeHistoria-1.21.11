package io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.realisationaltar;

import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

/**
 * 审计第 37 轮专用驱动（非发行物）：服务器端端到端驱动现实祭坛提取链。
 * 与 RealisationAltarCache 同包以获得 protected process() 访问权，
 * 直接调用生产代码路径（物品构建→槽位注入→process 提取/生长→事件破碎→掉落清点）。
 * 输出以 AUDIT37| 前缀键值对，供 RCON 解析。
 */
public class DriverPlugin extends JavaPlugin {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equals("altar")) {
            if (args.length < 5) {
                reply(sender, "usage=altar <world> <x> <y> <z>");
                return true;
            }
            try {
                driveAltar(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e.getClass().getSimpleName() + ":" + e.getMessage());
                for (StackTraceElement st : e.getStackTrace()) {
                    if (st.getClassName().contains("crystamaehistoria") || st.getClassName().contains("DriverPlugin")) {
                        reply(sender, "at=" + st);
                    }
                }
            }
            return true;
        }
        reply(sender, "usage=altar <world> <x> <y> <z>");
        return true;
    }

    private void driveAltar(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found:" + worldName);
            return;
        }
        final Location loc = new Location(world, x, y, z);
        final RealisationAltarCache cache = RealisationAltar.getCaches().get(loc);
        if (cache == null) {
            final StringBuilder sb = new StringBuilder();
            for (Location l : RealisationAltar.getCaches().keySet()) {
                sb.append(l.getWorld().getName()).append(':').append(l.getBlockX()).append(',').append(l.getBlockY()).append(',').append(l.getBlockZ()).append(';');
            }
            reply(sender, "error=no_cache_at_target knownCaches=" + sb);
            return;
        }
        reply(sender, "cache_found=true");

        // 1. 构建满故事闪长岩（生产路径：makeStoried + pickStory/commitStory）
        final ItemStack stack = new ItemStack(Material.DIORITE);
        StoryUtils.makeStoried(stack);
        final int limit = StoryUtils.getMaxStoryAmount(stack.getItemMeta());
        int guard = 0;
        while (StoryUtils.getStoryAmount(stack.getItemMeta()) < limit && guard++ < 20) {
            final Story main = StoryUtils.pickStory(stack);
            final int remaining = limit - StoryUtils.getStoryAmount(stack.getItemMeta());
            final Story unique = remaining == 1 ? StoryUtils.pickUniqueStory(stack) : null;
            if (main == null && unique == null) {
                reply(sender, "warn=pool_empty");
                break;
            }
            StoryUtils.commitStory(stack, main, unique);
        }
        final List<Story> committed = StoryUtils.getAllStories(stack.getItemMeta());
        reply(sender, "storied_item=ready limit=" + limit + " stories=" + (committed == null ? 0 : committed.size()));

        // 2. 注入输入槽（生产路径：BlockMenu 槽位）
        final BlockMenu menu = BlockStorage.getInventory(loc);
        if (menu == null) {
            reply(sender, "error=no_menu");
            return;
        }
        menu.replaceExistingItem(22, stack);
        reply(sender, "injected=true");

        // 3. 驱动提取与生长（生产路径：process() 循环——插件类加载器隔离，
        //    同包不授予 protected 访问，反射调用）
        try {
            final java.lang.reflect.Method process = RealisationAltarCache.class.getDeclaredMethod("process");
            process.setAccessible(true);
            for (int i = 0; i < 5000; i++) {
                try {
                    process.invoke(cache);
                } catch (ReflectiveOperationException e) {
                    final Throwable c = e.getCause() != null ? e.getCause() : e;
                    reply(sender, "error=process_iter" + i + ":" + c.getClass().getName() + ":" + c.getMessage());
                    for (StackTraceElement st : c.getStackTrace()) {
                        if (st.getClassName().contains("crystamaehistoria")) {
                            reply(sender, "at=" + st);
                        }
                    }
                    return;
                }
            }
        } catch (ReflectiveOperationException e) {
            reply(sender, "error=reflect:" + e);
            return;
        }
        final Map<BlockPosition, RealisationAltarCache.RealisedCrystalState> map = cache.getCrystalStoryMap();
        int small = 0, medium = 0, large = 0;
        BlockPosition firstLarge = null;
        for (Map.Entry<BlockPosition, RealisationAltarCache.RealisedCrystalState> e : map.entrySet()) {
            final Material m = e.getKey().getBlock().getType();
            if (m == Material.SMALL_AMETHYST_BUD) {
                small++;
            } else if (m == Material.MEDIUM_AMETHYST_BUD) {
                medium++;
            } else if (m == Material.LARGE_AMETHYST_BUD) {
                large++;
                if (firstLarge == null) {
                    firstLarge = e.getKey();
                }
            } else {
                reply(sender, "unexpected_block=" + m + " at=" + e.getKey().getPosition());
            }
        }
        final ItemStack left = menu.getItemInSlot(22);
        reply(sender, "after_drive mapSize=" + map.size() + " small=" + small + " medium=" + medium + " large=" + large
            + " inputLeft=" + (left == null || left.getType().isAir() ? "none" : left.getType() + "x" + left.getAmount()));

        // 4. 破碎一个大晶簇（生产路径：真实 BlockBreakEvent → CrystalBreakListener）
        if (firstLarge == null) {
            reply(sender, "break=skipped_no_large");
            return;
        }
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "break=skipped_no_player");
            return;
        }
        final Block crystalBlock = firstLarge.getBlock();
        final int mapSizeBefore = map.size();
        final int itemsBefore = countItems(crystalBlock.getLocation());
        final BlockBreakEvent event = new BlockBreakEvent(crystalBlock, player);
        Bukkit.getPluginManager().callEvent(event);
        final int itemsAfter = countItems(crystalBlock.getLocation());
        reply(sender, "break_done cancelled=" + event.isCancelled()
            + " blockNow=" + crystalBlock.getType()
            + " droppedEntities=" + (itemsAfter - itemsBefore)
            + " mapSizeBefore=" + mapSizeBefore
            + " mapSizeAfter=" + map.size());
        for (Entity en : crystalBlock.getLocation().getWorld().getNearbyEntities(crystalBlock.getLocation(), 3, 3, 3)) {
            if (en instanceof Item) {
                final ItemStack is = ((Item) en).getItemStack();
                reply(sender, "drop=" + is.getType() + "x" + is.getAmount());
            }
        }
        final boolean pass = itemsAfter > itemsBefore && map.size() == mapSizeBefore - 1
            && (crystalBlock.getType() == Material.AIR);
        reply(sender, "result=" + (pass ? "PASS" : "CHECK_MANUALLY"));
    }

    private int countItems(Location l) {
        int n = 0;
        for (Entity e : l.getWorld().getNearbyEntities(l, 3, 3, 3)) {
            if (e instanceof Item) {
                n++;
            }
        }
        return n;
    }

    private void reply(CommandSender sender, String msg) {
        sender.sendMessage("AUDIT37|" + msg);
    }
}
