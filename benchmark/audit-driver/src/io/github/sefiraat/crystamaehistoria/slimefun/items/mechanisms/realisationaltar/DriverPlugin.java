package io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.realisationaltar;

import io.github.sefiraat.crystamaehistoria.slimefun.Materials;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasin;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasinCache;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.prismaticgilder.PrismaticGilder;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.prismaticgilder.PrismaticGilderCache;
import io.github.sefiraat.crystamaehistoria.stories.Story;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
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
import org.bukkit.event.block.BlockPlaceEvent;
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
        if (args.length >= 5 && args[0].equals("salts")) {
            try {
                driveSalts(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("crysta")) {
            try {
                driveCrysta(sender);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("brush")) {
            try {
                driveBrush(sender);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("exalted")) {
            try {
                driveExalted(sender, args.length >= 2 ? args[1] : "");
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 5 && args[0].equals("basinplate")) {
            try {
                driveBasinPlate(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 5 && args[0].equals("tpasync")) {
            try {
                driveTpAsync(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 6 && args[0].equals("waystone") && args[5].equals("pos")) {
            try {
                drivePos(sender, Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("waystone") && args.length >= 5) {
            try {
                driveWaystone(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("configurator") && args.length >= 6) {
            try {
                driveConfigurator(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), args[5]);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("stats")) {
            try {
                driveStats(sender);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 2 && args[0].equals("compendium")) {
            try {
                driveCompendium(sender, args[1]);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("legacy")) {
            try {
                driveLegacy(sender);
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("spells") && args.length >= 2) {
            try {
                if (args[1].equals("cast")) {
                    driveSpellsCast(sender);
                } else if (args[1].equals("stat")) {
                    driveSpellsStat(sender);
                } else {
                    reply(sender, "usage=spells cast|stat");
                }
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("gadgets") && args.length >= 5) {
            try {
                driveGadgets(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("place") && args.length >= 6) {
            try {
                drivePlace(sender, args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("gilder") && args.length >= 5) {
            try {
                driveGilder(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
        if (args.length >= 1 && args[0].equals("basin") && args.length >= 5) {
            try {
                driveBasin(sender, args[1], Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
            } catch (Exception e) {
                reply(sender, "error=" + e);
            }
            return true;
        }
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
            for (int i = 0; i < 2000; i++) {
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

    private ItemStack lastDropStack;

    /** 第 43 轮：沿 +x 逐格放置全部 tick 类 gadget（真实 BlockPlaceEvent，玩家须在线且邻近） */
    private void driveGadgets(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final String[] ids = {
            "CRY_MOB_LAMP_1", "CRY_MOB_FAN_1", "CRY_MOB_DIRT_1", "CRY_MOB_PLATE_1", "CRY_MOB_PLATE_TRAP",
            "CRY_EXP_COLLECTOR_1", "CRY_ENDER_INHIBITOR_1", "CRY_MOB_CANDLE_1", "CRY_CROP_GLASS_1",
            "CRY_MYSTERIOUS_POTTED_PLANT", "CRY_TROPHY_DISPLAY_1", "CRY_FRAGMENTED_VOID", "CRY_WAYSTONE"
        };
        int placed = 0, cancelled = 0, missing = 0;
        int cx = x;
        for (String sfId : ids) {
            final SlimefunItem sfi = SlimefunItem.getById(sfId);
            if (sfi == null) {
                reply(sender, "missing=" + sfId);
                missing++;
                cx++;
                continue;
            }
            final Block block = world.getBlockAt(cx, y, z);
            final org.bukkit.block.BlockState replacedState = block.getState();
            block.setType(sfi.getItem().getType());
            final BlockPlaceEvent event = new BlockPlaceEvent(
                block, replacedState, block.getRelative(org.bukkit.block.BlockFace.DOWN),
                sfi.getItem().clone(), player, true, org.bukkit.inventory.EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                block.setType(Material.AIR);
                reply(sender, "cancelled=" + sfId);
                cancelled++;
            } else {
                placed++;
            }
            cx++;
        }
        reply(sender, "gadgets_done placed=" + placed + " cancelled=" + cancelled + " missing=" + missing + " range=x" + x + "..x" + (cx - 1));
    }

    /** 第 57 轮：奇术盐清池 + 折射透镜展示（真实 PlayerInteractEvent 驱动，r6 修复回归） */
    private void driveSalts(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (world == null || player == null) {
            reply(sender, "error=world_or_player");
            return;
        }
        final LiquefactionBasinCache cache = ((LiquefactionBasin) SlimefunItem.getById("CRY_LIQUEFACTION_BASIN_1"))
            .getCacheMap().get(new Location(world, x, y, z));
        if (cache == null) {
            reply(sender, "error=no_basin_cache");
            return;
        }
        final Block basinBlock = world.getBlockAt(x, y, z);
        final Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        // 预填 3 单位液体
        final StoryType[] types = StoryType.values();
        java.util.Set<StoryType> recipe = null;
        outer:
        for (int a = 0; a < types.length; a++) {
            for (int b = a + 1; b < types.length; b++) {
                for (int c = b + 1; c < types.length; c++) {
                    final java.util.Set<StoryType> set = java.util.EnumSet.of(types[a], types[b], types[c]);
                    if (LiquefactionBasinCache.lookupSpellRecipe(set, 1) != null) {
                        recipe = set;
                        break outer;
                    }
                }
            }
        }
        if (recipe == null) {
            reply(sender, "error=no_recipe");
            return;
        }
        for (StoryType t : recipe) {
            world.dropItem(center, Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem());
            cache.consumeItems();
        }
        final int fillBefore = cache.getFillLevel();
        if (fillBefore < 3) {
            reply(sender, "prefill_fail fill=" + fillBefore);
            return;
        }

        // ---- 1. 奇术盐清池（r6：event.getItem + 副手忽略 + 判空）----
        final SlimefunItem saltsSf = SlimefunItem.getById("CRY_THAUMATURGIC_SALT");
        if (saltsSf == null) {
            reply(sender, "error=no_salts");
            return;
        }
        final ItemStack salts = saltsSf.getItem().clone();
        final ItemStack prevMain = player.getInventory().getItemInMainHand();
        player.getInventory().setItemInMainHand(salts);
        reply(sender, "hand_probe type=" + player.getInventory().getItemInMainHand().getType() + " getByItem=" + (SlimefunItem.getByItem(player.getInventory().getItemInMainHand()) != null ? SlimefunItem.getByItem(player.getInventory().getItemInMainHand()).getId() : "null"));
        reply(sender, "perm_probe BREAK_BLOCK=" + io.github.sefiraat.crystamaehistoria.utils.GeneralUtils.hasPermission(
                player, basinBlock, io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction.BREAK_BLOCK)
            + " check=" + (me.mrCookieSlime.Slimefun.api.BlockStorage.check(basinBlock) != null
                ? me.mrCookieSlime.Slimefun.api.BlockStorage.check(basinBlock).getId() : "null"));
        final org.bukkit.event.player.PlayerInteractEvent saltsEvent = new org.bukkit.event.player.PlayerInteractEvent(
            player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, salts, basinBlock, org.bukkit.block.BlockFace.UP, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(saltsEvent);
        final boolean saltConsumed = player.getInventory().getItemInMainHand().getType() != Material.REDSTONE;
        player.getInventory().setItemInMainHand(prevMain);
        final boolean cleared = cache.getFillLevel() == 0 && saltConsumed;
        reply(sender, "salts fill=" + fillBefore + "→" + cache.getFillLevel() + " " + (cleared ? "PASS" : "FAIL") + " cancelled=" + saltsEvent.isCancelled());

        // ---- 2. 折射透镜展示（r6：双展示修复）----
        for (StoryType t : recipe) {
            world.dropItem(center, Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem());
            cache.consumeItems();
        }
        final SlimefunItem lensSf = SlimefunItem.getById("CRY_REFRACTING_LENS");
        if (lensSf == null) {
            reply(sender, "error=no_lens");
            return;
        }
        final ItemStack lens = lensSf.getItem().clone();
        player.getInventory().setItemInMainHand(lens);
        final io.github.sefiraat.crystamaehistoria.SpellMemory sm =
            io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getSpellMemory();
        final int displaysBefore = sm.getDisplayItems().size();
        final org.bukkit.event.player.PlayerInteractEvent lensEvent = new org.bukkit.event.player.PlayerInteractEvent(
            player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, lens, basinBlock, org.bukkit.block.BlockFace.UP, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(lensEvent);
        final int displaysAfter1 = sm.getDisplayItems().size();
        // 同一玩家 3s 内二次展示（r6 双展示修复：应被抑制或替换，不叠加）
        final org.bukkit.event.player.PlayerInteractEvent lensEvent2 = new org.bukkit.event.player.PlayerInteractEvent(
            player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, lens.clone(), basinBlock, org.bukkit.block.BlockFace.UP, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(lensEvent2);
        final int displaysAfter2 = sm.getDisplayItems().size();
        reply(sender, "lens displays " + displaysBefore + "→" + displaysAfter1 + "→" + displaysAfter2
            + " " + ((displaysAfter1 == displaysBefore + 1 && displaysAfter2 <= displaysAfter1) ? "PASS" : "CHECK"));
        player.getInventory().setItemInMainHand(prevMain);
        reply(sender, "salts_done");
    }

    /** 第 55 轮：水晶燃烧降级 + 下界门脱水（真实事件驱动监听器生产路径，玩家邻区实体即时注册 r41 实证） */
    private void driveCrysta(CommandSender sender) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final Location loc = player.getLocation().clone().add(2, 0.5, 0);
        // ---- 1. 燃烧降级：RARE → UNCOMMON ----
        final org.bukkit.entity.Item rare = player.getWorld().dropItem(loc,
            Materials.getCrystalMap().get(io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.RARE)
                .get(io.github.sefiraat.crystamaehistoria.stories.definition.StoryType.ELEMENTAL).getItem());
        final org.bukkit.block.Block fire = loc.getBlock();
        fire.setType(Material.FIRE);
        final org.bukkit.event.entity.EntityCombustByBlockEvent combust =
            new org.bukkit.event.entity.EntityCombustByBlockEvent(fire, rare, 8);
        Bukkit.getPluginManager().callEvent(combust);
        final SlimefunItem downgraded = SlimefunItem.getByItem(rare.getItemStack());
        fire.setType(Material.AIR);
        final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity nowRarity =
            downgraded instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal
                ? ((io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal) downgraded).getRarity() : null;
        final boolean downgradeOk = nowRarity == io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.UNCOMMON
            && combust.isCancelled();
        reply(sender, "downgrade " + (downgradeOk ? "PASS" : "FAIL")
            + " (RARE→UNCOMMON 事件取消=" + combust.isCancelled() + " now=" + nowRarity + " valid=" + rare.isValid() + ")");
        rare.remove();

        // ---- 2. 下界门脱水：第一个有效配方入→出 ----
        java.util.Map.Entry<ItemStack, ItemStack> hit = null;
        for (java.util.Map.Entry<ItemStack, ItemStack> en : io.github.sefiraat.crystamaehistoria.slimefun.CrystaRecipeTypes.getDrainingRecipes().entrySet()) {
            if (en.getKey() != null && en.getKey().getType() != Material.AIR && en.getValue() != null) {
                hit = en;
                break;
            }
        }
        if (hit == null) {
            reply(sender, "drain SKIP no_recipe");
            return;
        }
        final org.bukkit.entity.Item toDrain = player.getWorld().dropItem(loc, hit.getKey().clone());
        final org.bukkit.event.entity.EntityPortalEnterEvent portal =
            new org.bukkit.event.entity.EntityPortalEnterEvent(toDrain, loc);
        Bukkit.getPluginManager().callEvent(portal);
        final ItemStack after = toDrain.getItemStack();
        final boolean drainOk = io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils.isItemSimilar(after, hit.getValue(), true, false);
        reply(sender, "drain " + (drainOk ? "PASS" : "FAIL")
            + " (in=" + hit.getKey().getType() + " expected=" + hit.getValue().getType() + " now=" + after.getType() + ")");
        toDrain.remove();
    }

    /** 第 53 轮：画笔消耗链（tryPaint 生产路径 + LimitedUseItem PDC 衰减 + 耗尽损坏） */
    private void driveBrush(CommandSender sender) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final SlimefunItem sfi = SlimefunItem.getById("CRY_BRUSH_BLACK_100");
        if (!(sfi instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.MagicPaintbrush)) {
            reply(sender, "error=no_brush found=" + (sfi != null));
            return;
        }
        final io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.MagicPaintbrush brush =
            (io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.MagicPaintbrush) sfi;
        final io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.PaintProfile profile =
            io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.PaintProfile.BLACK;
        final ItemStack stack = sfi.getItem().clone();
        final org.bukkit.NamespacedKey usesKey = io.github.sefiraat.crystamaehistoria.utils.Keys.newKey("uses");
        final int maxUses = ((io.github.thebusybiscuit.slimefun4.implementation.items.LimitedUseItem) sfi).getMaxUseCount();

        // 画一块石头 100 次（黑色染色羊毛目标保证每次都实际涂色）
        final Block target = player.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
        target.setType(Material.WHITE_WOOL);
        final io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.BasicPaintbrush bp =
            (io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.BasicPaintbrush) sfi;
        int painted = 0, unchanged = 0;
        Integer lastUses = null;
        for (int i = 0; i < maxUses + 5; i++) {
            final boolean paintedNow;
            try {
                paintedNow = brush.tryPaintBlock(profile, target);
            } catch (Throwable t) {
                reply(sender, "paint_error iter" + i + "=" + t);
                return;
            }
            if (paintedNow) {
                painted++;
                try {
                    final java.lang.reflect.Method damage = io.github.thebusybiscuit.slimefun4.implementation.items.LimitedUseItem.class
                        .getDeclaredMethod("damageItem", org.bukkit.entity.Player.class, ItemStack.class);
                    damage.setAccessible(true);
                    damage.invoke(sfi, player, stack);
                } catch (ReflectiveOperationException ex) {
                    reply(sender, "damage_reflect_error=" + ex);
                    return;
                }
                if (stack.getType() == Material.AIR || stack.getAmount() <= 0 || !stack.hasItemMeta()) {
                    reply(sender, "brush_depleted_at=" + painted + " (堆已空——LimitedUseItem 耗尽语义)");
                    return;
                }
                lastUses = stack.getItemMeta().getPersistentDataContainer().getOrDefault(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER, -1);
                // 重置方块材质使下一次仍可涂（循环白→黑）
                target.setType(Material.WHITE_WOOL);
            } else {
                unchanged++;
            }
        }
        final boolean depleted = stack.getType() == Material.AIR || stack.getAmount() == 0
            || !stack.hasItemMeta() || stack.getItemMeta().getPersistentDataContainer().has(usesKey, org.bukkit.persistence.PersistentDataType.INTEGER) == false
            || lastUses != null && lastUses <= 0;
        reply(sender, "brush maxUses=" + maxUses + " painted=" + painted + " unchanged=" + unchanged
            + " lastUses=" + lastUses + " stackNow=" + stack.getType() + "x" + stack.getAmount()
            + " depleted=" + depleted);
    }

    /** 第 52 轮：Exalted 物品效果链（onExalt 生产方法 + SpellMemory 冻结表登记/过期回收） */
    private void driveExalted(CommandSender sender, String which) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final String id = which.equals("weather") ? "CRY_EXALTED_SUN" : "CRY_EXALTED_DAWN";
        final SlimefunItem sfi = SlimefunItem.getById(id);
        if (!(sfi instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.exhalted.ExaltedItem)) {
            reply(sender, "error=not_exalted id=" + id + " found=" + (sfi != null));
            return;
        }
        final io.github.sefiraat.crystamaehistoria.slimefun.items.exhalted.ExaltedItem exalted =
            (io.github.sefiraat.crystamaehistoria.slimefun.items.exhalted.ExaltedItem) sfi;
        try {
            exalted.onExalt(exalted, player.getLocation().clone());
        } catch (Throwable t) {
            reply(sender, "exalt_error=" + t);
            return;
        }
        final io.github.sefiraat.crystamaehistoria.SpellMemory sm =
            io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getSpellMemory();
        final boolean timeHit = sm.getPlayersWithFrozenTime().containsKey(player.getUniqueId());
        final boolean weatherHit = sm.getPlayersWithFrozenWeather().containsKey(player.getUniqueId());
        reply(sender, "exalted=" + which
            + " frozenTimeTable=" + (timeHit ? "HIT" : "miss")
            + " frozenWeatherTable=" + (weatherHit ? "HIT" : "miss")
            + " playerTime=" + player.getPlayerTime() + " weather=" + player.getPlayerWeather());
    }

    /** 第 50 轮：液化池充能板三分支（同法术再充能/异法术销毁/损坏 PDC 吞没） */
    private void driveBasinPlate(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final LiquefactionBasinCache cache = ((LiquefactionBasin) SlimefunItem.getById("CRY_LIQUEFACTION_BASIN_1"))
            .getCacheMap().get(new Location(world, x, y, z));
        if (cache == null) {
            reply(sender, "error=no_basin_cache");
            return;
        }
        final Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        // 有效配方组合（与 basin 子命令同法）
        final StoryType[] types = StoryType.values();
        java.util.Set<StoryType> recipe = null;
        outer:
        for (int a = 0; a < types.length; a++) {
            for (int b = a + 1; b < types.length; b++) {
                for (int c = b + 1; c < types.length; c++) {
                    final java.util.Set<StoryType> set = java.util.EnumSet.of(types[a], types[b], types[c]);
                    if (LiquefactionBasinCache.lookupSpellRecipe(set, 1) != null) {
                        recipe = set;
                        break outer;
                    }
                }
            }
        }
        if (recipe == null) {
            reply(sender, "error=no_recipe");
            return;
        }
        final io.github.sefiraat.crystamaehistoria.magic.SpellType matching = LiquefactionBasinCache.lookupSpellRecipe(recipe, 1);
        final io.github.sefiraat.crystamaehistoria.magic.SpellType other = matching == io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH
            ? io.github.sefiraat.crystamaehistoria.magic.SpellType.HEAL : io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH;

        // ---- 分支 1：同法术再充能 ----
        cache.emptyBasin();
        for (StoryType t : recipe) {
            world.dropItem(center, Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem());
            cache.consumeItems();
        }
        final int fillBefore = cache.getFillLevel();
        final ItemStack plateSame = io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate.getChargedPlate(1, matching, 10);
        world.dropItem(center, plateSame);
        cache.consumeItems();
        cache.consumeItems();
        // 收集结果板（原地重铸：再充能板在物品位置重下）
        int newCrysta = -1;
        for (Entity en : world.getNearbyEntities(center, 2, 2, 2)) {
            if (en instanceof Item) {
                final ItemStack is = ((Item) en).getItemStack();
                if (SlimefunItem.getByItem(is) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate) {
                    final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate ip = io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.getCustom(
                        is.getItemMeta(), io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_PLATE_STORAGE,
                        io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentPlateDataType.TYPE);
                    newCrysta = ip == null ? -2 : ip.getCrysta();
                    en.remove();
                }
            }
        }
        boolean rechargeOk = newCrysta == 10 + fillBefore && cache.getFillLevel() == 0;
        reply(sender, "recharge expected=" + (10 + fillBefore) + " got=" + newCrysta + " fillAfter=" + cache.getFillLevel()
            + " " + (rechargeOk ? "PASS" : "FAIL"));

        // ---- 分支 2：异法术销毁（惩罚路径）----
        cache.emptyBasin();
        for (StoryType t : recipe) {
            world.dropItem(center, Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem());
            cache.consumeItems();
        }
        world.dropItem(center, io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate.getChargedPlate(1, other, 10));
        cache.consumeItems();
        cache.consumeItems();
        int platesLeft = 0;
        for (Entity en : world.getNearbyEntities(center, 3, 3, 3)) {
            if (en instanceof Item && SlimefunItem.getByItem(((Item) en).getItemStack()) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate) {
                platesLeft++;
            }
        }
        boolean mismatchOk = cache.getFillLevel() == 0 && platesLeft == 0;
        reply(sender, "mismatch fillAfter=" + cache.getFillLevel() + " platesLeft=" + platesLeft + " " + (mismatchOk ? "PASS" : "FAIL"));

        // ---- 分支 3：损坏 PDC 板吞没（失败关闭 + 告警）----
        cache.emptyBasin();
        for (StoryType t : recipe) {
            world.dropItem(center, Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem());
            cache.consumeItems();
        }
        final ItemStack corrupt = io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate.getChargedPlate(1, matching, 10);
        final org.bukkit.inventory.meta.ItemMeta cm = corrupt.getItemMeta();
        // 伪造损坏：以错误类型写键（读取抛 IllegalStateException → 吞没路径）
        cm.getPersistentDataContainer().set(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_PLATE_STORAGE, org.bukkit.persistence.PersistentDataType.STRING, "corrupted");
        corrupt.setItemMeta(cm);
        world.dropItem(center, corrupt);
        cache.consumeItems();
        cache.consumeItems();
        int corruptLeft = 0;
        for (Entity en : world.getNearbyEntities(center, 3, 3, 3)) {
            if (en instanceof Item && SlimefunItem.getByItem(((Item) en).getItemStack()) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate) {
                corruptLeft++;
            }
        }
        reply(sender, "corrupt platesLeft=" + corruptLeft + " fillAfter=" + cache.getFillLevel()
            + " " + (corruptLeft == 0 ? "PASS(吞没+告警)" : "FAIL"));
        reply(sender, "basinplate_done");
    }

    /** teleportAsync×mineflayer 微探针：fired 即回，60 tick 后回报完成态与位置 */
    private void driveTpAsync(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (world == null || player == null) {
            reply(sender, "error=world_or_player");
            return;
        }
        final java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean(false);
        player.teleportAsync(new Location(world, x + 0.5, y, z + 0.5)).thenRun(() -> done.set(true));
        reply(sender, "tpasync_fired");
        Bukkit.getScheduler().runTaskLater(this, () -> {
            final org.bukkit.Location now = player.getLocation();
            reply(sender, "tpasync_result completed=" + done.get()
                + " now=" + now.getBlockX() + ',' + now.getBlockY() + ',' + now.getBlockZ());
        }, 60L);
    }

    /** 异步传送后的位置查询（两段式验证） */
    private void drivePos(CommandSender sender, int x, int y, int z) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final org.bukkit.Location now = player.getLocation();
        final double dist = now.distance(new Location(now.getWorld(), x + 1.5, y, z + 0.5));
        reply(sender, "pos now=" + now.getBlockX() + ',' + now.getBlockY() + ',' + now.getBlockZ()
            + " distToWaystoneTop=" + String.format("%.1f", dist) + " " + (dist < 3.0 ? "PASS" : "FAIL"));
    }

    /** 第 49 轮：Waystone 绑定-传送往返（真实 PlayerInteractEvent 驱动生产路径） */
    private void driveWaystone(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final Block waystone = world.getBlockAt(x, y, z);
        if (!(me.mrCookieSlime.Slimefun.api.BlockStorage.check(waystone) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.gadgets.Waystone)) {
            reply(sender, "error=waystone_not_registered block=" + waystone.getType());
            return;
        }
        final SlimefunItem latticeSf = SlimefunItem.getById("CRY_RECALL_LATTICE");
        if (latticeSf == null) {
            reply(sender, "error=no_lattice");
            return;
        }
        final ItemStack lattice = latticeSf.getItem().clone();
        // 1) 绑定：潜行 + 右击路标（真实 PlayerInteractEvent → SlimefunItemListener → ItemUseHandler.setLocation）
        player.setSneaking(true);
        final org.bukkit.event.player.PlayerInteractEvent bindEvent = new org.bukkit.event.player.PlayerInteractEvent(
            player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, lattice, waystone, org.bukkit.block.BlockFace.UP, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(bindEvent);
        player.setSneaking(false);
        final boolean bound = lattice.getItemMeta().getPersistentDataContainer().has(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_RECALL_LOCATION, io.github.sefiraat.crystamaehistoria.utils.datatypes.DataType.LOCATION);
        reply(sender, "bind " + (bound ? "PASS" : "FAIL") + " (lattice meta PDC 含位置)");
        // 2) 传送：远离后非潜行右击（用绑定后的物品——重新从处理器写入后的物品取 meta）
        if (bound) {
            player.teleport(new Location(world, x + 40.5, y, z + 0.5));
            final org.bukkit.event.player.PlayerInteractEvent tpEvent = new org.bukkit.event.player.PlayerInteractEvent(
                player, org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK, lattice, waystone, org.bukkit.block.BlockFace.UP, org.bukkit.inventory.EquipmentSlot.HAND);
            Bukkit.getPluginManager().callEvent(tpEvent);
            final org.bukkit.Location now = player.getLocation();
            final double dist = now.distance(new Location(world, x + 1.5, y + 1.0, z + 0.5));
            reply(sender, "teleport_fired=async");
        }
    }

    /** 第 48 轮：法杖配置器驱动：fill（法杖+充能板入槽）/ assert（组装后 PDC 断言） */
    private void driveConfigurator(CommandSender sender, String worldName, int x, int y, int z, String mode) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final BlockMenu menu = BlockStorage.getInventory(new Location(world, x, y, z));
        if (menu == null) {
            reply(sender, "error=no_menu");
            return;
        }
        if (mode.equals("fill")) {
            final SlimefunItem staveSf = SlimefunItem.getById("CRY_STAVE_1");
            if (staveSf == null) {
                reply(sender, "error=no_stave_item");
                return;
            }
            menu.replaceExistingItem(19, staveSf.getItem().clone());
            menu.replaceExistingItem(14, io.github.sefiraat.crystamaehistoria.slimefun.items.tools.plates.ChargedPlate.getChargedPlate(1, io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH, 50));
            reply(sender, "configurator_filled stave=19 plate=14");
        } else if (mode.equals("assert")) {
            final ItemStack stave = menu.getItemInSlot(19);
            boolean ok = false;
            String detail = "noStave";
            if (stave != null && stave.getType() == Material.STICK) {
                final java.util.Map<io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot, io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate> map =
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.readStaveMap(stave.getItemMeta());
                final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate plate =
                    map == null ? null : map.get(io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.LEFT_CLICK);
                ok = plate != null && plate.getStoredSpell() == io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH && plate.getCrysta() == 50;
                detail = "plate=" + (plate == null ? "null" : plate.getStoredSpell() + "/" + plate.getCrysta())
                    + " platesSlot14=" + (menu.getItemInSlot(14) == null || menu.getItemInSlot(14).getType().isAir() ? "cleared" : "left");
            }
            reply(sender, "configurator_assert " + (ok ? "PASS" : "FAIL") + " " + detail);
        } else {
            reply(sender, "usage=configurator <world> <x> <y> <z> fill|assert");
        }
    }

    /** 第 47 轮：统计写入链驱动验证（六写点→纪元失效→落盘） */
    private void driveStats(CommandSender sender) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final java.util.UUID uuid = player.getUniqueId();
        final io.github.sefiraat.crystamaehistoria.stories.BlockDefinition def =
            io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(Material.DIORITE);
        if (def == null) {
            reply(sender, "error=no_definition");
            return;
        }
        final io.github.sefiraat.crystamaehistoria.magic.SpellType push = io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH;
        try {
            // 六写点
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addUsage(uuid, push);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockSpell(uuid, push);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockUniqueStory(uuid, def);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addChronicle(uuid, def);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addRealisation(player, def);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockStoryGilded(uuid, def);
        } catch (Throwable t) {
            reply(sender, "write_error=" + t);
            return;
        }
        // 读回断言（纪元缓存消费方）
        boolean okUsage = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getUsages(uuid, push) >= 1;
        boolean okSpell = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(uuid, push);
        boolean okUnique = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedUniqueStory(uuid, def);
        reply(sender, "readback usage=" + okUsage + " spell=" + okSpell + " unique=" + okUnique);
        // 落盘（force 路径，与关服一致）
        io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getConfigManager().saveAll(true);
        // 文件断言
        final java.io.File f = new java.io.File(getDataFolder().getParentFile(), "CrystamaeHistoria/player_stats.yml");
        boolean fileOk = false;
        try {
            final String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            fileOk = content.contains(uuid.toString());
        } catch (Throwable t) {
            reply(sender, "file_read_error=" + t);
        }
        reply(sender, "stats_done filePersisted=" + fileOk + " pass=" + (okUsage && okSpell && okUnique && fileOk));
    }

    /** 第 46 轮：图鉴 GUI 生产路径打开（三个 FlexGroup） */
    private void driveCompendium(CommandSender sender, String which) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        final java.util.Optional<io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile> profileOpt =
            io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile.find(offlinePlayer);
        if (profileOpt.isEmpty()) {
            reply(sender, "error=no_profile");
            return;
        }
        final io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile profile = profileOpt.get();
        switch (which) {
            case "spells" -> io.github.sefiraat.crystamaehistoria.slimefun.ItemGroups.SPELL_COLLECTION
                .open(player, profile, io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode.SURVIVAL_MODE);
            case "stories" -> io.github.sefiraat.crystamaehistoria.slimefun.ItemGroups.STORY_COLLECTION
                .open(player, profile, io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode.SURVIVAL_MODE);
            case "gilded" -> io.github.sefiraat.crystamaehistoria.slimefun.ItemGroups.GILDING_COLLECTION
                .open(player, profile, io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode.SURVIVAL_MODE);
            default -> {
                reply(sender, "usage=compendium spells|stories|gilded");
                return;
            }
        }
        reply(sender, "compendium_opened=" + which);
    }

    /** 第 45 轮：旧存档兼容复验——v1 写入 → v2 双读断言（故事列表/法杖/区块晶簇） */
    private void driveLegacy(CommandSender sender) {
        // ---- 1. 故事列表 v1（PDC_STORIES + 旧容器编码）----
        try {
            final ItemStack legacyItem = new ItemStack(Material.DIORITE);
            StoryUtils.makeStoried(legacyItem);
            final int limit = StoryUtils.getMaxStoryAmount(legacyItem.getItemMeta());
            int g = 0;
            while (StoryUtils.getStoryAmount(legacyItem.getItemMeta()) < limit && g++ < 20) {
                StoryUtils.commitStory(legacyItem, StoryUtils.pickStory(legacyItem), null);
            }
            final List<Story> sample = StoryUtils.getAllStories(legacyItem.getItemMeta());
            // 复制到新物品并以 v1 编码写入
            final ItemStack v1Item = new ItemStack(Material.DIORITE);
            final org.bukkit.inventory.meta.ItemMeta v1Meta = v1Item.getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                v1Meta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, sample);
            v1Meta.getPersistentDataContainer().set(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_IS_STORIED, org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
            v1Item.setItemMeta(v1Meta);
            final List<Story> readBack = StoryUtils.getAllStories(v1Item.getItemMeta());
            boolean storiesOk = readBack != null && readBack.size() == (sample == null ? 0 : sample.size());
            if (storiesOk && sample != null) {
                for (int i = 0; i < sample.size(); i++) {
                    if (!sample.get(i).getId().equals(readBack.get(i).getId())
                        || sample.get(i).getRarity() != readBack.get(i).getRarity()) {
                        storiesOk = false;
                        break;
                    }
                }
            }
            reply(sender, "legacy_stories " + (storiesOk ? "PASS" : "FAIL")
                + " written=" + (sample == null ? 0 : sample.size()) + " read=" + (readBack == null ? 0 : readBack.size()));
        } catch (Throwable t) {
            reply(sender, "legacy_stories ERROR " + t);
        }

        // ---- 2. 法杖 v1（PDC_STAVE_STORAGE + 旧每板容器编码）→ v2 readStaveMap 双读 ----
        try {
            final ItemStack stave = io.github.sefiraat.crystamaehistoria.slimefun.Tools.getStaveBasic().getItem().clone();
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave instance =
                new io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave(stave);
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate plate =
                new io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate(1, io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH, 123);
            instance.getSpellInstanceMap().put(io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.LEFT_CLICK, plate);
            final ItemStack v1Stave = io.github.sefiraat.crystamaehistoria.slimefun.Tools.getStaveBasic().getItem().clone();
            final org.bukkit.inventory.meta.ItemMeta sm = v1Stave.getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                sm, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STAVE_STORAGE,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType.TYPE, instance.getSpellInstanceMap());
            v1Stave.setItemMeta(sm);
            final java.util.Map<io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot, io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate> readMap =
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.readStaveMap(v1Stave.getItemMeta());
            boolean staveOk = readMap != null && readMap.size() == 1;
            if (staveOk) {
                final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate rp =
                    readMap.get(io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.LEFT_CLICK);
                staveOk = rp != null && rp.getStoredSpell() == io.github.sefiraat.crystamaehistoria.magic.SpellType.PUSH && rp.getCrysta() == 123;
            }
            reply(sender, "legacy_stave " + (staveOk ? "PASS" : "FAIL") + " readSize=" + (readMap == null ? -1 : readMap.size()));
        } catch (Throwable t) {
            reply(sender, "legacy_stave ERROR " + t);
        }

        // ---- 3. 区块晶簇 v1（旧容器编码）→ v2 readChunkStories 双读 ----
        try {
            final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
            if (player == null) {
                reply(sender, "legacy_chunk SKIP no_player");
            } else {
                final org.bukkit.Chunk chunk = player.getLocation().getChunk();
                final Story s = StoryUtils.pickStory(new ItemStack(Material.DIORITE));
                final Story cs = s == null ? null : s.copy();
                boolean chunkOk = false;
                if (cs != null) {
                    cs.setBlockPosition(new BlockPosition(player.getLocation().getBlock()));
                    final List<Story> write = new java.util.ArrayList<>();
                    write.add(cs);
                    final org.bukkit.NamespacedKey testKey = new org.bukkit.NamespacedKey(this, "legacy_test_v1");
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                        chunk, testKey,
                        io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType.TYPE, write);
                    final List<Story> readC = io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.readChunkStories(chunk, testKey);
                    chunkOk = readC != null && readC.size() == 1
                        && readC.get(0).getId().equals(cs.getId())
                        && readC.get(0).getBlockPosition() != null;
                    chunk.getPersistentDataContainer().remove(testKey);
                }
                reply(sender, "legacy_chunk " + (chunkOk ? "PASS" : "FAIL"));
            }
        } catch (Throwable t) {
            reply(sender, "legacy_chunk ERROR " + t);
        }
        reply(sender, "legacy_done");
    }

    /** 第 44 轮：多原型法术施放（生产路径：CastInformation+freeze+castSpell，同 test-spell） */
    private void driveSpellsCast(CommandSender sender) {
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final String[] ids = {
            "HEAL", "BRIGHT",
            "FIREBALL", "FAN_OF_ARROWS",
            "TIME_COMPRESSION", "PUSH",
            "STAR_FALL",
            "CALL_LIGHTNING",
            "SUMMON_GOLEM",
            "PHANTOMS_FLIGHT"
        };
        int ok = 0, err = 0;
        for (String id : ids) {
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell st = io.github.sefiraat.crystamaehistoria.magic.SpellType.getById(id);
            if (st == null) {
                reply(sender, "missing=" + id);
                continue;
            }
            try {
                final io.github.sefiraat.crystamaehistoria.magic.CastInformation ci =
                    new io.github.sefiraat.crystamaehistoria.magic.CastInformation(player, 2);
                ci.freezeTargetsOnCast();
                st.castSpell(ci);
                ok++;
                reply(sender, "cast_ok=" + id);
            } catch (Throwable t) {
                err++;
                reply(sender, "cast_err=" + id + ":" + t.getClass().getSimpleName() + ":" + t.getMessage());
            }
        }
        reply(sender, "cast_done ok=" + ok + " err=" + err);
    }

    /** 第 44 轮：SpellMemory 全表尺寸快照（生命周期回收断言用） */
    private void driveSpellsStat(CommandSender sender) {
        final io.github.sefiraat.crystamaehistoria.SpellMemory sm =
            io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getSpellMemory();
        reply(sender, "stat projectiles=" + sm.getProjectileMap().size()
            + " fallingBlocks=" + sm.getFallingBlockMap().size()
            + " strikes=" + sm.getStrikeMap().size()
            + " ticking=" + sm.getTickingCastables().size()
            + " summons=" + sm.getSummonedEntities().size()
            + " flight=" + sm.getPlayersWithFlight().size()
            + " frozenTime=" + sm.getPlayersWithFrozenTime().size()
            + " frozenWeather=" + sm.getPlayersWithFrozenWeather().size()
            + " displayItems=" + sm.getDisplayItems().size()
            + " sleepBags=" + sm.getSleepingBags().size());
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

    /** 第 38 轮：以真实 BlockPlaceEvent 放置并注册机械（与玩家放置同路径） */
    private void drivePlace(CommandSender sender, String sfId, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final SlimefunItem sfi = SlimefunItem.getById(sfId);
        if (sfi == null) {
            reply(sender, "error=unknown_sf_id:" + sfId);
            return;
        }
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "error=no_player");
            return;
        }
        final Block block = world.getBlockAt(x, y, z);
        final org.bukkit.block.BlockState replacedState = block.getState();
        block.setType(sfi.getItem().getType());
        final BlockPlaceEvent event = new BlockPlaceEvent(
            block, replacedState, block.getRelative(org.bukkit.block.BlockFace.DOWN),
            sfi.getItem().clone(), player, true, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            block.setType(Material.AIR);
            reply(sender, "placed=false cancelled=true");
            return;
        }
        reply(sender, "placed=true block=" + block.getType());
    }

    /** 第 38 轮：镀金器吸取 + 镀金路径（FLASH 修复第二受影响面） */
    private void driveGilder(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final SlimefunItem sfi = SlimefunItem.getById("CRY_PRISMATIC_GILDER");
        if (!(sfi instanceof PrismaticGilder)) {
            reply(sender, "error=gilder_item_not_found");
            return;
        }
        final PrismaticGilderCache cache = ((PrismaticGilder) sfi).getCacheMap().get(new Location(world, x, y, z));
        if (cache == null) {
            reply(sender, "error=no_gilder_cache known=" + ((PrismaticGilder) sfi).getCacheMap().keySet());
            return;
        }
        reply(sender, "cache_found=true fill0=" + cache.getFillAmount());

        // 1. 吸取：棱镜水晶掉落在镀金器中心 → consumeItems（public）→ addCrystamae（FLASH 路径）
        final Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        final ItemStack crystal = Materials.getPrismaticCrystal().getItem();
        world.dropItem(center, crystal);
        cache.consumeItems();
        cache.consumeItems();
        final int fillAfterAbsorb = cache.getFillAmount();
        final boolean absorbed = fillAfterAbsorb >= 1;
        reply(sender, "absorb fillAfter=" + fillAfterAbsorb + " pass=" + absorbed);

        // 2. 镀金：满故事闪长岩 + 在线玩家（DisplayItem/DUST 路径）
        final Player player = Bukkit.getOnlinePlayers().isEmpty() ? null : Bukkit.getOnlinePlayers().iterator().next();
        if (player == null) {
            reply(sender, "gild=skipped_no_player");
            return;
        }
        // 补足填充（有界！原无界 while 在吸收未生效时会无限 dropItem——
        // 实体注册 O(N) 扫描叠加曾把主线程拖死至 OOM（第 42 轮教训））
        int fillGuard = 0;
        while (cache.getFillAmount() < 1 && fillGuard++ < 8) {
            world.dropItem(center, Materials.getPrismaticCrystal().getItem());
            cache.consumeItems();
        }
        final ItemStack storied = new ItemStack(Material.DIORITE);
        StoryUtils.makeStoried(storied);
        final int limit = StoryUtils.getMaxStoryAmount(storied.getItemMeta());
        int guard = 0;
        while (StoryUtils.getStoryAmount(storied.getItemMeta()) < limit && guard++ < 20) {
            StoryUtils.commitStory(storied, StoryUtils.pickStory(storied), null);
        }
        final int fillBeforeGild = cache.getFillAmount();
        final Block block = world.getBlockAt(x, y, z);
        cache.gildItem(block, storied, player);
        final int fillAfterGild = cache.getFillAmount();
        final boolean heldConsumed = storied.getAmount() == 0;
        reply(sender, "gild fillBefore=" + fillBeforeGild + " fillAfter=" + fillAfterGild
            + " heldConsumed=" + heldConsumed + " pass=" + (fillAfterGild == fillBeforeGild - 1 && heldConsumed));
        reply(sender, "result=" + (absorbed && fillAfterGild == fillBeforeGild - 1 && heldConsumed ? "PASS" : "FAIL"));
    }

    /** 第 38 轮：液化池吸取 + 空白板催化合成链 */
    private void driveBasin(CommandSender sender, String worldName, int x, int y, int z) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            reply(sender, "error=world_not_found");
            return;
        }
        final SlimefunItem sfi = SlimefunItem.getById("CRY_LIQUEFACTION_BASIN_1");
        if (!(sfi instanceof LiquefactionBasin)) {
            reply(sender, "error=basin_item_not_found");
            return;
        }
        final LiquefactionBasinCache cache = ((LiquefactionBasin) sfi).getCacheMap().get(new Location(world, x, y, z));
        if (cache == null) {
            reply(sender, "error=no_basin_cache known=" + ((LiquefactionBasin) sfi).getCacheMap().keySet());
            return;
        }
        reply(sender, "cache_found=true fill0=" + cache.getFillLevel());

        // 1. 找一个有效的 3 类型配方组合（tier 1）
        final StoryType[] types = StoryType.values();
        java.util.Set<StoryType> recipe = null;
        outer:
        for (int a = 0; a < types.length; a++) {
            for (int b = a + 1; b < types.length; b++) {
                for (int c = b + 1; c < types.length; c++) {
                    final java.util.Set<StoryType> set = java.util.EnumSet.of(types[a], types[b], types[c]);
                    if (LiquefactionBasinCache.lookupSpellRecipe(set, 1) != null) {
                        recipe = set;
                        break outer;
                    }
                }
            }
        }
        if (recipe == null) {
            reply(sender, "error=no_recipe_combo");
            return;
        }
        reply(sender, "recipe_found=" + recipe);

        // 2. 依次投入 3 种 COMMON 水晶（各 1 体积）——吸取路径（addCrystamae/updateDisplay）
        final Location center = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        for (StoryType t : recipe) {
            final ItemStack dropStack = Materials.getCrystalMap().get(StoryRarity.COMMON).get(t).getItem();
            reply(sender, "preDiag type=" + t + " stack=" + dropStack.getType() + "x" + dropStack.getAmount()
                + " sameAsPrev=" + (dropStack == lastDropStack));
            lastDropStack = dropStack;
            final org.bukkit.entity.Item dropped = world.dropItem(center, dropStack);
            reply(sender, "diag type=" + t + " resolved=" + io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(dropped)
                + " boxEntities=" + world.getNearbyEntities(center, 0.3, 0.3, 0.3).size()
                + " spawnLoc=" + dropped.getLocation().getBlockX() + ',' + dropped.getLocation().getBlockY() + ',' + dropped.getLocation().getBlockZ()
                + " exact=" + String.format("%.2f,%.2f,%.2f", dropped.getLocation().getX(), dropped.getLocation().getY(), dropped.getLocation().getZ())
                + " valid=" + dropped.isValid());
            cache.consumeItems();
        }
        final int fillAfter = cache.getFillLevel();
        reply(sender, "absorbed fill=" + fillAfter + " pass=" + (fillAfter >= 3));

        // 3. 投入空白法术板 → processBlankPlate → ChargedPlate 掉落 + 清池
        final SlimefunItem blank = SlimefunItem.getById("CRY_SPELL_PLATE_1");
        if (blank == null) {
            reply(sender, "error=no_inert_plate");
            return;
        }
        final int itemsBefore = countItems(center);
        world.dropItem(center, blank.getItem());
        cache.consumeItems();
        cache.consumeItems();
        final int fillAfterCatalyst = cache.getFillLevel();
        final int itemsAfter = countItems(center);
        reply(sender, "catalyst fillAfter=" + fillAfterCatalyst + " itemsBefore=" + itemsBefore + " itemsAfter=" + itemsAfter);
        for (Entity en : world.getNearbyEntities(center, 3, 3, 3)) {
            if (en instanceof Item) {
                final ItemStack is = ((Item) en).getItemStack();
                reply(sender, "drop=" + is.getType() + "x" + is.getAmount());
            }
        }
        final boolean pass = fillAfter >= 3 && fillAfterCatalyst == 0 && itemsAfter > itemsBefore;
        reply(sender, "result=" + (pass ? "PASS" : "FAIL"));
    }
}
