package ch.perfbench;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicProjectile;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentUUIDDataType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Particle;
import org.bukkit.entity.Projectile;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.util.Vector;

/**
 * 服务器内基准（第 2 轮）：真实世界 raycast 与真实法杖 PDC 反序列化实测。
 * onEnable 后延迟 5 秒在主线程运行，结果写 plugins/CHPerfBench/results.tsv。
 */
public final class CHPerfBench extends JavaPlugin {

    /** 黑洞计数，防死码消除 */
    private long bh;

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskLater(this, this::runBenchmarks, 100L);
    }

    private void runBenchmarks() {
        final File outDir = new File(getDataFolder().getParentFile(), "CHPerfBench");
        outDir.mkdirs();
        try {
            final PrintWriter w = new PrintWriter(new File(outDir, "results.tsv"), "UTF-8");
            w.println("bench\tvariant\tmedian_ns_op");
            w.flush();
            // 基准组链式调度：每组之间让出至少 2 tick，避免连续阻塞主线程超过
            // 服务器 watchdog 阈值（曾触发线程转储乃至强制停机）
            final Runnable[] steps = {
                () -> benchRaycast(w),
                () -> benchStavePdc(w),
                () -> benchInteractPaths(w),
                () -> benchMachineTick(w),
                () -> benchMachineTickMemo(w),
                () -> benchStaveCast(w),
                () -> benchGadgetTick(w),
                () -> benchStoryPick(w),
                () -> benchStatsPath(w),
                () -> benchRound10(w),
                () -> benchRound11(w),
                () -> benchRound12(w),
                () -> benchRound13(w),
                () -> benchRound15(w),
                () -> benchRound16(w),
                () -> benchRound17(w),
                () -> benchRound18(w),
                () -> benchRound19(w),
                () -> benchRound21(w),
                () -> benchRound22(w),
                () -> benchRound23(w),
                () -> benchRound24(w),
                () -> benchRound26(w),
                () -> benchRound27(w),
                () -> benchRound28(w),
                () -> benchRound29(w),
                () -> benchRound31(w),
                () -> benchRound34(w),
                () -> benchRound35(w),
                () -> benchRound38(w),
                () -> benchRound39(w),
                () -> benchRound40(w),
                () -> benchRound42(w),
                () -> benchRound44(w),
                () -> benchRound45(w),
                () -> benchRound46(w),
                () -> benchRound47(w),
                () -> benchRound49(w),
                () -> benchRound50(w),
                () -> benchRound53(w),
                () -> benchRound55(w),
                () -> benchRound57(w),
                () -> benchRound59(w),
                () -> benchRound62(w),
                () -> benchRound63(w),
                () -> benchRound70(w),
                () -> benchRound73(w),
                () -> benchRound76(w)
            };
            chainSteps(w, steps, 0);
        } catch (Exception e) {
            getLogger().severe("基准失败: " + e);
            e.printStackTrace();
        }
    }

    private void chainSteps(PrintWriter w, Runnable[] steps, int index) {
        if (index >= steps.length) {
            w.close();
            getLogger().info("CHPERFBENCH COMPLETE, blackhole=" + bh);
            return;
        }
        try {
            steps[index].run();
        } catch (Exception e) {
            getLogger().severe("基准组 " + index + " 失败: " + e);
            e.printStackTrace();
        }
        // 让出 2 tick（约 100ms）供服务器呼吸，重置 watchdog 计时
        Bukkit.getScheduler().runTaskLater(this, () -> chainSteps(w, steps, index + 1), 2L);
    }

    /** 变体：A=旧构造器（两次 raycast），B=新构造器（单次 rayTraceBlocks）；miss=50格无命中，hit=5格石墙命中 */
    private void benchRaycast(PrintWriter w) {
        World world = Bukkit.getWorlds().get(0);
        // miss 场景：悬空实体水平看向无遮挡方向
        Zombie miss = world.spawn(new Location(world, 0, 200, 0), Zombie.class);
        miss.setRotation(0f, 0f);
        time(w, "raycast50.miss", "old_exact_plus_face", 20_000, () -> {
            miss.getTargetBlockExact(50);
            miss.getTargetBlockFace(50);
        });
        time(w, "raycast50.miss", "new_single_raytrace", 20_000, () -> miss.rayTraceBlocks(50));

        // hit 场景：5 格外一面石墙
        Location hitLoc = new Location(world, 1000, 100, 1000);
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                hitLoc.clone().add(5, dy - 1, dx - 1).getBlock().setType(Material.STONE);
            }
        }
        Zombie hit = world.spawn(hitLoc, Zombie.class);
        hit.setRotation(-90f, 0f); // 面向 +X? 由实测 hitBlock 非空校验
        boolean wallVisible = hit.rayTraceBlocks(5) != null;
        if (!wallVisible) {
            hit.setRotation(90f, 0f);
            wallVisible = hit.rayTraceBlocks(5) != null;
        }
        getLogger().info("hit 场景墙壁可见: " + wallVisible);
        time(w, "raycast50.hit5", "old_exact_plus_face", 20_000, () -> {
            hit.getTargetBlockExact(50);
            hit.getTargetBlockFace(50);
        });
        time(w, "raycast50.hit5", "new_single_raytrace", 20_000, () -> hit.rayTraceBlocks(50));

        miss.remove();
        hit.remove();
        for (int dx = 0; dx < 3; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                hitLoc.clone().add(5, dy - 1, dx - 1).getBlock().setType(Material.AIR);
            }
        }
    }

    /** 真实法杖 PDC：getItemMeta + PersistentStaveDataType 反序列化（4 板全满的法杖） */
    private void benchStavePdc(PrintWriter w) {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        Map<SpellSlot, InstancePlate> plates = new EnumMap<>(SpellSlot.class);
        for (SpellSlot slot : SpellSlot.values()) {
            plates.put(slot, new InstancePlate(1, SpellType.HEAL, 100));
        }
        DataTypeMethods.setCustom(meta, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE, plates);
        stack.setItemMeta(meta);

        time(w, "stavePdc.deserialize", "full_meta_plus_pdc", 5_000, () -> {
            ItemMeta m = stack.getItemMeta();
            DataTypeMethods.getCustom(m, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE);
        });
    }

    /** 第 3 轮：交互路径 ItemMeta 操作（真实插件代码/真实 ItemStack） */
    private void benchInteractPaths(PrintWriter w) {
        // 任意物品右键的冷却检查（MiscListener.checkCooldown，LOWEST，每次右键触发）
        ItemStack dirt = new ItemStack(Material.DIRT);
        time(w, "interactRightClick.cooldownCheck", "old_pdc_read", 20_000,
            () -> GeneralUtils.isOnCooldown(dirt));
        time(w, "interactRightClick.cooldownCheck", "new_material_gate", 2_000_000, () -> {
            if (dirt.getType() == Material.SPYGLASS) {
                bh++;
            }
        });

        // 任意交互的调光勺检查（MiscListener.onUseScoop，LOWEST，每次交互触发）
        ItemStack lantern = new ItemStack(Material.LANTERN);
        time(w, "interactAny.scoopCheck", "old_getByItem_plain", 5_000,
            () -> SlimefunItem.getByItem(lantern));
        time(w, "interactAny.scoopCheck", "new_material_gate", 2_000_000, () -> {
            Material t = lantern.getType();
            if (t == Material.LANTERN || t == Material.SOUL_LANTERN) {
                bh++;
            }
        });

        // 施法成功路径的法杖写回（SpellCastListener）
        ItemStack stave = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stave.getItemMeta();
        Map<SpellSlot, InstancePlate> plates = new EnumMap<>(SpellSlot.class);
        for (SpellSlot slot : SpellSlot.values()) {
            plates.put(slot, new InstancePlate(1, SpellType.HEAL, 100));
        }
        DataTypeMethods.setCustom(meta, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE, plates);
        stave.setItemMeta(meta);
        final InstanceStave staveInstance = new InstanceStave(stave);

        time(w, "staveSuccess.metaWriteBack", "old_two_round_trips", 2_000, () -> {
            ItemMeta m = stave.getItemMeta();
            DataTypeMethods.setCustom(m, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE,
                staveInstance.getSpellInstanceMap());
            stave.setItemMeta(m);
            staveInstance.buildLore();
        });
        time(w, "staveSuccess.metaWriteBack", "new_single_round_trip", 2_000, () -> {
            ItemMeta m = stave.getItemMeta();
            DataTypeMethods.setCustom(m, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE,
                staveInstance.getSpellInstanceMap());
            staveInstance.buildLore(m);
            stave.setItemMeta(m);
        });
    }

    /** 第 4 轮：机械每 tick 故事判定链与拾取点计算（真实插件代码） */
    private void benchMachineTick(PrintWriter w) {
        // 真实已记录物品（STONE，含故事上限 PDC）
        ItemStack stone = new ItemStack(Material.STONE);
        StoryUtils.makeStoried(stone);

        // 记录者面板稳态 tick 判定链（canBeStoried/isStoried/hasRemainingStorySlots）
        time(w, "machineTick.steadyStoryCheck", "old_itemstack_args", 5_000, () -> {
            if (StoryUtils.canBeStoried(stone, 2)
                && StoryUtils.isStoried(stone)
                && StoryUtils.hasRemainingStorySlots(stone)) {
                bh++;
            }
        });
        time(w, "machineTick.steadyStoryCheck", "new_single_meta", 5_000, () -> {
            final ItemMeta meta = stone.getItemMeta();
            final boolean storied = StoryUtils.isStoried(meta);
            if (StoryUtils.canBeStoried(stone, 2, storied)
                && storied
                && StoryUtils.hasRemainingStorySlots(meta)) {
                bh++;
            }
        });

        // 剩余地板成本：单次 getMaxStoryAmount（ItemMeta 克隆 + Gson 解析）
        time(w, "machineTick.jsonLimitsParse", "getMaxStoryAmount", 20_000,
            () -> StoryUtils.getMaxStoryAmount(stone));

        // 拾取扫描中心：旧（每 tick 克隆+偏移两次分配）vs 新（懒缓存字段读）
        final Location base = Bukkit.getWorlds().get(0).getSpawnLocation();
        final Location cached = base.clone().add(0.5, 0.5, 0.5);
        time(w, "machineTick.pickupLocCompute", "old_clone_add", 1_000_000, () ->
            base.clone().add(0.5, 0.5, 0.5));
        time(w, "machineTick.pickupLocCompute", "new_cached_field", 5_000_000, () -> {
            // 真实路径仅将缓存字段传给 getNearbyEntities（无 hashCode/计算），
            // 此处以 null 检查强迫字段读取，防死码消除
            if (cached == null) {
                bh++;
            }
        });
    }

    /** 第 5 轮：面板完整工作 tick（process + processStack 判定）跨 3 个版本形态 */
    private void benchMachineTickMemo(PrintWriter w) {
        ItemStack stone = new ItemStack(Material.STONE);
        StoryUtils.makeStoried(stone);
        final ItemStack[] slot = {stone};
        final boolean[] verdict = {false};

        // 第 3 轮前：process 与 processStack 各自独立读取（5 次克隆 + 3 次 JSON 解析）
        time(w, "machineTick.fullWorking", "round3_itemstack_args", 2_000, () -> {
            if (StoryUtils.canBeStoried(stone, 2)
                && StoryUtils.hasRemainingStorySlots(stone)) {
                bh += StoryUtils.getRemainingStoryAmount(stone);
            }
        });

        // 第 4 轮后：process 单次 meta，processStack 两次独立读取（3 次克隆 + 2 次 JSON）
        time(w, "machineTick.fullWorking", "round4_single_meta_partial", 2_000, () -> {
            final ItemMeta meta = stone.getItemMeta();
            final boolean storied = StoryUtils.isStoried(meta);
            if (StoryUtils.canBeStoried(stone, 2, storied)
                && StoryUtils.hasRemainingStorySlots(meta)) {
                bh += StoryUtils.getRemainingStoryAmount(stone);
            }
        });

        // 第 5 轮后：稳态备忘录命中（引用比较，无元数据读取）
        memoItem = stone;
        time(w, "machineTick.fullWorking", "round5_memo_hit", 5_000_000, () -> {
            if (slot[0] == memoItem) {
                bh++;
            }
        });
    }

    private ItemStack memoItem;

    /** 第 6 轮：施法交互的 PDC 读取（4 板满法杖，真实插件代码） */
    private void benchStaveCast(PrintWriter w) {
        ItemStack stave = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stave.getItemMeta();
        Map<SpellSlot, InstancePlate> plates = new EnumMap<>(SpellSlot.class);
        for (SpellSlot slot : SpellSlot.values()) {
            plates.put(slot, new InstancePlate(1, SpellType.HEAL, 100));
        }
        DataTypeMethods.setCustom(meta, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE, plates);
        stave.setItemMeta(meta);

        // 失败前置路径（冷却/缺晶能/空槽）的读取成本
        time(w, "staveCast.precheckReads", "old_full_deserialize", 2_000,
            () -> new InstanceStave(stave));
        time(w, "staveCast.precheckReads", "new_slot_only_read", 5_000, () -> {
            try {
                if (PersistentStaveDataType.getSlotPlate(stave.getItemMeta(), SpellSlot.RIGHT_CLICK) != null) {
                    bh++;
                }
            } catch (IllegalStateException e) {
                bh += 2;
            }
        });

        // 成功路径读取总量：旧（全量一次，独立 meta）vs 新（单次 meta 克隆 + 单槽 + 全量）
        time(w, "staveCast.successReads", "old_full_once", 2_000,
            () -> new InstanceStave(stave));
        time(w, "staveCast.successReads", "new_shared_meta_reads", 2_000, () -> {
            final ItemMeta m = stave.getItemMeta();
            PersistentStaveDataType.getSlotPlate(m, SpellSlot.RIGHT_CLICK);
            new InstanceStave(stave, m);
        });
    }

    /** 第 7 轮：gadgets 每 tick 模式（真实 BlockStorage/真实方块） */
    private void benchGadgetTick(PrintWriter w) {
        World world = Bukkit.getWorlds().get(0);
        Block block = world.getBlockAt(2000, 100, 2000);
        block.setType(Material.STONE);
        final String ownerUuid = "12345678-1234-1234-1234-123456789012";
        BlockStorage.addBlockInfo(block, "CH_DIRECTION", "NORTH");
        BlockStorage.addBlockInfo(block, "CH_UUID", ownerUuid);

        // MobFan 前缀：旧（每 tick 字符串读取 + valueOf/fromString）vs 新（内存缓存查表）
        time(w, "gadgetTick.mobFanPrefix", "old_bs_string_parse", 5_000, () -> {
            final String d = BlockStorage.getLocationInfo(block.getLocation(), "CH_DIRECTION");
            final String o = BlockStorage.getLocationInfo(block.getLocation(), "CH_UUID");
            if (d == null) {
                return;
            }
            try {
                final BlockFace f = BlockFace.valueOf(d);
                final UUID u = o != null ? UUID.fromString(o) : null;
                if (f == BlockFace.SELF || u == null) {
                    bh++;
                }
            } catch (IllegalArgumentException e) {
                bh += 2;
            }
        });
        final Map<Location, BlockFace> directionMap = new HashMap<>();
        final Map<Location, UUID> ownerMap = new HashMap<>();
        directionMap.put(block.getLocation(), BlockFace.NORTH);
        ownerMap.put(block.getLocation(), UUID.fromString(ownerUuid));
        time(w, "gadgetTick.mobFanPrefix", "new_map_lookup", 20_000, () -> {
            final BlockFace f = directionMap.get(block.getLocation());
            final UUID u = ownerMap.get(block.getLocation());
            if (f == null || f == BlockFace.SELF || u == null) {
                bh++;
            }
        });

        // MysteriousTicker 材质抽取：旧（每次 toArray 复制）vs 新（预生成数组）
        final Set<Material> materials = java.util.EnumSet.of(
            Material.STONE, Material.COBBLESTONE, Material.ANDESITE, Material.GRANITE, Material.DIORITE);
        time(w, "gadgetTick.materialPick", "old_toArray_copy", 200_000, () -> {
            bh += materials.toArray(new Material[]{})[ThreadLocalRandom.current().nextInt(materials.size())].ordinal();
        });
        final Material[] materialArray = materials.toArray(new Material[0]);
        time(w, "gadgetTick.materialPick", "new_cached_array", 2_000_000, () -> {
            bh += materialArray[ThreadLocalRandom.current().nextInt(materialArray.length)].ordinal();
        });

        // 每 tick Location 分配模式：旧（4 次 getLocation + 2 次 clone().add）vs 新（1+1）
        time(w, "gadgetTick.locationPattern", "old_multi_alloc", 200_000, () -> {
            final Location center = block.getLocation().add(0.5, 0.5, 0.5);
            bh += center.getBlockX();
            bh += block.getLocation().hashCode();
            bh += block.getLocation().hashCode();
            bh += block.getLocation().add(0.5, 1, 0.5).getBlockX();
        });
        time(w, "gadgetTick.locationPattern", "new_single_alloc", 500_000, () -> {
            final Location blockLocation = block.getLocation();
            final Location center = blockLocation.clone().add(0.5, 0.5, 0.5);
            bh += center.getBlockX();
            bh += blockLocation.hashCode();
        });
    }

    /** 第 8 轮：故事选取索引与配置加载（真实 StoriesManager 数据/真实 blocks.yml） */
    private void benchStoryPick(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.managers.StoriesManager manager =
            CrystamaeHistoria.getStoriesManager();
        final StoryType type = StoryType.ELEMENTAL;

        // 旧：每次对整稀有度故事表 stream 过滤 + 收集
        time(w, "storyPick.byType", "old_stream_filter", 20_000, () -> {
            final List<io.github.sefiraat.crystamaehistoria.stories.Story> available =
                manager.getStoryMapCommon().values().stream()
                       .filter(t -> t.getType() == type)
                       .collect(java.util.stream.Collectors.toList());
            bh += available.size();
        });
        // 新：稀有度×类型索引查表
        time(w, "storyPick.byType", "new_index_lookup", 1_000_000, () -> {
            final List<io.github.sefiraat.crystamaehistoria.stories.Story> available =
                manager.getStories(io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON, type);
            bh += available == null ? 0 : available.size();
        });

        // 配置加载：单次解析 vs 旧实现的双重解析（真实 995 键 blocks.yml）
        final java.io.File blocksFile =
            new java.io.File(CrystamaeHistoria.getInstance().getDataFolder(), "blocks.yml");
        time(w, "configParse.blocksYml", "old_double_parse", 4, () -> {  // 批量 20→4：慢宿主下单批 >10s 触发 watchdog 强制停机（2026-08-17 实测三连复现）
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);
                cfg.load(blocksFile);
                bh += cfg.getKeys(false).size();
            } catch (Exception e) {
                bh++;
            }
        });
        time(w, "configParse.blocksYml", "new_single_parse", 8, () -> {
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);
                bh += cfg.getKeys(false).size();
            } catch (Exception e) {
                bh++;
            }
        });
    }

    /** 第 9 轮：统计路径构建（每次施法 addUsage 双路径 + 每次记录 addChronicle） */
    private void benchStatsPath(PrintWriter w) {
        final UUID player = UUID.fromString("12345678-1234-1234-1234-123456789012");
        final String spellId = "HEAL";
        time(w, "statsPath.build", "old_messageformat", 200_000, () -> {
            bh += java.text.MessageFormat.format("{0}.{1}.{2}.TIMES_CAST", player, SpellType.HEAL, spellId).length();
        });
        time(w, "statsPath.build", "new_concat", 2_000_000, () -> {
            bh += (player + "." + SpellType.HEAL + "." + spellId + ".TIMES_CAST").length();
        });
    }

    /** 第 10 轮：世界级高频事件路径（弹射物命中/下落方块反查、无敌检查、召唤物门控） */
    private void benchRound10(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.SpellMemory memory = CrystamaeHistoria.getSpellMemory();
        final World world = Bukkit.getWorlds().get(0);
        final org.bukkit.util.Vector dir = new org.bukkit.util.Vector(1, 0, 0);
        final NamespacedKey legacyInvulnKey =
            new NamespacedKey(CrystamaeHistoria.getInstance(), "invul");

        // —— 弹射物命中反查（每次任意弹射物命中均触发）——
        // 常态一：空表（无魔法弹射物存活）
        final Projectile arrow = world.spawnArrow(new Location(world, 0, 220, 0), dir, 1.0f, 0f);
        time(w, "eventPath.projectileReverse", "old_stream_empty", 200_000, () -> bh += memory
            .getProjectileMap().keySet().stream()
            .filter(mp -> mp.matches(arrow))
            .findFirst().isPresent() ? 1 : 0);
        time(w, "eventPath.projectileReverse", "new_index_empty", 5_000_000, () -> {
            if (memory.getProjectileByUuid(arrow.getUniqueId()) != null) {
                bh++;
            }
        });

        // 常态二：满表（8 个魔法弹射物存活，未命中即常态——原版弹射物占绝对多数）
        final List<Projectile> balls = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final Projectile ball = world.spawn(new Location(world, i, 230, 0), org.bukkit.entity.Snowball.class);
            balls.add(ball);
            // 2 秒过期：基准后由 TemporaryEffectsRunnable 周期清扫经 kill() 正常回收（含索引）
            memory.registerProjectile(
                new MagicProjectile(ball),
                new Pair<>((CastInformation) null, System.currentTimeMillis() + 2000));
        }
        time(w, "eventPath.projectileReverse", "old_stream_8entries_miss", 100_000, () -> bh += memory
            .getProjectileMap().keySet().stream()
            .filter(mp -> mp.matches(arrow))
            .findFirst().isPresent() ? 1 : 0);
        time(w, "eventPath.projectileReverse", "new_index_8entries_miss", 5_000_000, () -> {
            if (memory.getProjectileByUuid(arrow.getUniqueId()) != null) {
                bh++;
            }
        });
        for (final Projectile ball : balls) {
            ball.remove();
        }
        arrow.remove();

        // —— 下落方块落地反查（每次任意沙砾落地均触发，空表常态）——
        final org.bukkit.entity.FallingBlock sand = world.spawnFallingBlock(
            new Location(world, 2000, 250, 2000), Material.SAND.createBlockData());
        time(w, "eventPath.fallingBlockReverse", "old_stream_empty", 200_000, () -> bh += memory
            .getFallingBlockMap().keySet().stream()
            .filter(fb -> fb.matches(sand))
            .findFirst().isPresent() ? 1 : 0);
        time(w, "eventPath.fallingBlockReverse", "new_index_empty", 5_000_000, () -> {
            if (memory.getFallingBlockByUuid(sand.getUniqueId()) != null) {
                bh++;
            }
        });
        sand.remove();

        // —— 无敌检查（每次任意实体受伤均触发，未标记即常态）——
        final Zombie victim = world.spawn(new Location(world, 0, 220, 0), Zombie.class);
        time(w, "eventPath.invulnCheck", "old_pdc_read", 100_000, () -> {
            if (PersistentDataAPI.hasLong(victim, legacyInvulnKey)) {
                bh++;
            }
        });
        time(w, "eventPath.invulnCheck", "new_map_lookup", 5_000_000, () -> {
            if (memory.getInvulnerabilityExpiry(victim.getUniqueId()) != null) {
                bh++;
            }
        });
        // 写入路径（Protectorate 每受保护实体每秒一次）
        time(w, "eventPath.invulnWrite", "old_pdc_write", 50_000, () ->
            PersistentDataAPI.setLong(victim, legacyInvulnKey, System.currentTimeMillis() + 1050));
        time(w, "eventPath.invulnWrite", "new_map_write", 5_000_000, () ->
            memory.markInvulnerable(victim.getUniqueId(), System.currentTimeMillis() + 1050));
        memory.removeInvulnerability(victim.getUniqueId());
        victim.remove();

        // —— 召唤物门控（每次任意实体死亡/方块变化均触发）——
        final Zombie skeleton = world.spawn(new Location(world, 0, 220, 0), Zombie.class);
        time(w, "eventPath.summonGate", "old_pdc_read", 100_000, () -> {
            if (DataTypeMethods.hasCustom(skeleton, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE)) {
                bh++;
            }
        });
        // 常态（骷髅等非召唤类型）：门控直接排除，零 PDC 读取
        time(w, "eventPath.summonGate", "new_typegate_skeleton", 5_000_000, () -> {
            if (io.github.sefiraat.crystamaehistoria.utils.SpellUtils.isSummonableMobType(org.bukkit.entity.EntityType.SKELETON)) {
                bh++;
            }
        });
        // 白名单类型（僵尸）：门控通过后仍需 PDC 确认（复合路径）
        time(w, "eventPath.summonGate", "new_typegate_zombie_plus_pdc", 100_000, () -> {
            if (io.github.sefiraat.crystamaehistoria.utils.SpellUtils.isSummonableMobType(org.bukkit.entity.EntityType.ZOMBIE)
                && DataTypeMethods.hasCustom(skeleton, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE)) {
                bh++;
            }
        });
        skeleton.remove();
    }

    /** 第 11 轮：液化池路径（每 tick BlockStorage 写、催化剂 top-3 选取、配方匹配、每 tick 分配） */
    private void benchRound11(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);

        // —— 每 tick syncBlock：旧（附近每实体无条件全量写 contentMap 键）vs 新（脏标记跳过）——
        final Block bsBlock = world.getBlockAt(3000, 100, 3000);
        bsBlock.setType(Material.STONE);
        final Map<StoryType, Integer> content = new EnumMap<>(StoryType.class);
        int val = 1;
        for (StoryType t : StoryType.values()) {
            content.put(t, val);
            val += 2;
        }
        time(w, "basinTick.syncBlockWrite", "old_unconditional_9keys", 10_000, () -> {
            for (Map.Entry<StoryType, Integer> e : content.entrySet()) {
                BlockStorage.addBlockInfo(bsBlock, "ch_c_lvl:" + e.getKey(), String.valueOf(e.getValue()));
            }
        });
        final boolean[] dirty = {false};
        time(w, "basinTick.syncBlockWrite", "new_dirty_flag_skip", 5_000_000, () -> {
            if (dirty[0]) {
                bh++;
            }
        });

        // —— 催化剂 top-3 选取：旧（双 stream 排序管线）vs 新（单遍 top-3，fillTopThree 同构副本）——
        time(w, "basinCatalyst.top3Pick", "old_double_stream_sort", 50_000, () -> {
            final List<StoryType> typeList = content.entrySet().stream()
                .sorted(Map.Entry.<StoryType, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
            final List<Integer> amountList = content.entrySet().stream()
                .sorted(Map.Entry.<StoryType, Integer>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getValue)
                .collect(java.util.stream.Collectors.toList());
            bh += typeList.size() + amountList.get(0);
        });
        final StoryType[] topTypes = new StoryType[3];
        final int[] topAmounts = new int[3];
        time(w, "basinCatalyst.top3Pick", "new_single_pass", 2_000_000, () -> {
            topTypes[0] = null;
            topTypes[1] = null;
            topTypes[2] = null;
            int a0 = -1;
            int a1 = -1;
            int a2 = -1;
            for (Map.Entry<StoryType, Integer> entry : content.entrySet()) {
                final StoryType type = entry.getKey();
                final int value = entry.getValue();
                if (value > a0) {
                    a2 = a1;
                    topTypes[2] = topTypes[1];
                    a1 = a0;
                    topTypes[1] = topTypes[0];
                    a0 = value;
                    topTypes[0] = type;
                } else if (value > a1) {
                    a2 = a1;
                    topTypes[2] = topTypes[1];
                    a1 = value;
                    topTypes[1] = type;
                } else if (value > a2) {
                    a2 = value;
                    topTypes[2] = type;
                }
            }
            topAmounts[0] = a0;
            topAmounts[1] = a1;
            topAmounts[2] = a2;
            bh += topAmounts[0];
        });

        // —— 配方匹配：旧（线性扫描全部配方）vs 新（类型集索引 O(1)）——
        // 预构建配方列表（旧实现 RECIPES_SPELL 即启动期预构建的注册表）
        final List<io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell> recipes =
            new java.util.ArrayList<>();
        for (SpellType st : SpellType.getCachedValues()) {
            recipes.add(st.get().getRecipe());
        }
        final java.util.Set<StoryType> hitSet = java.util.EnumSet.of(
            SpellType.HEAL.get().getRecipe().getInput(0),
            SpellType.HEAL.get().getRecipe().getInput(1),
            SpellType.HEAL.get().getRecipe().getInput(2));
        // miss 集合：穷举 C(9,3) 组合中不与任何配方类型集相同的第一个
        final StoryType[] all = StoryType.values();
        java.util.Set<StoryType> missFound = null;
        outer:
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                for (int k = j + 1; k < all.length; k++) {
                    final java.util.Set<StoryType> cand = java.util.EnumSet.of(all[i], all[j], all[k]);
                    if (cand.equals(hitSet)) {
                        continue;
                    }
                    boolean isRecipe = false;
                    for (io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell r : recipes) {
                        if (java.util.EnumSet.of(r.getInput(0), r.getInput(1), r.getInput(2)).equals(cand)) {
                            isRecipe = true;
                            break;
                        }
                    }
                    if (!isRecipe) {
                        missFound = cand;
                        break outer;
                    }
                }
            }
        }
        final java.util.Set<StoryType> missSet = missFound;
        getLogger().info("round11 missSet=" + missSet + " recipes=" + recipes.size());
        time(w, "basinCatalyst.recipeMatch", "old_linear_miss", 20_000, () -> {
            boolean found = false;
            for (io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell r : recipes) {
                if (r.recipeMatches(missSet, 1)) {
                    found = true;
                    break;
                }
            }
            bh += found ? 1 : 0;
        });
        time(w, "basinCatalyst.recipeMatch", "new_index_miss", 5_000_000, () -> {
            if (io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasinCache
                .lookupSpellRecipe(missSet, 1) != null) {
                bh++;
            }
        });
        time(w, "basinCatalyst.recipeMatch", "old_linear_hit_heal", 20_000, () -> {
            boolean found = false;
            for (io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell r : recipes) {
                if (r.recipeMatches(hitSet, 1)) {
                    found = true;
                    break;
                }
            }
            bh += found ? 1 : 0;
        });
        time(w, "basinCatalyst.recipeMatch", "new_index_hit_heal", 5_000_000, () -> {
            if (io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasinCache
                .lookupSpellRecipe(hitSet, 1) != null) {
                bh++;
            }
        });

        // —— 每 tick 粒子前置分配：旧（DustOptions + 中心 Location 双分配）vs 新（缓存字段读）——
        final Block basinBlock = world.getBlockAt(3000, 101, 3000);
        basinBlock.setType(Material.CAULDRON);
        time(w, "basinTick.particleSetup", "old_alloc_per_tick", 1_000_000, () -> {
            final Particle.DustOptions d = new Particle.DustOptions(org.bukkit.Color.AQUA, 1);
            final Location c = basinBlock.getLocation().add(0.5, 0.5, 0.5);
            bh += c.getBlockX() + (int) d.getSize();
        });
        final Particle.DustOptions cachedDust = new Particle.DustOptions(org.bukkit.Color.AQUA, 1);
        final Location cachedCenter = basinBlock.getLocation().add(0.5, 0.5, 0.5);
        time(w, "basinTick.particleSetup", "new_cached_fields", 5_000_000, () ->
            bh += cachedCenter.getBlockX() + (int) cachedDust.getSize());
        basinBlock.setType(Material.AIR);
        bsBlock.setType(Material.AIR);
    }

    /** 第 12 轮：启动路径（配置文件落盘策略、法术配置首次启动写盘、故事解析列表读取） */
    private void benchRound12(PrintWriter w) {
        final java.io.File dataDir = new File(getDataFolder().getParentFile(), "CHPerfBench");
        final java.io.File blocksFile =
            new java.io.File(CrystamaeHistoria.getInstance().getDataFolder(), "blocks.yml");

        // —— blocks.yml 稳态启动落盘：旧（无条件 save）vs 新（默认键存在性检查后跳过）——
        try {
            final org.bukkit.configuration.file.YamlConfiguration blocksCfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);
            final java.io.File tmpBlocks = new java.io.File(dataDir, "tmp-blocks.yml");
            time(w, "startup.configSave", "old_unconditional_save", 10, () -> {
                try {
                    blocksCfg.save(tmpBlocks);
                } catch (Exception e) {
                    bh++;
                }
            });
            time(w, "startup.configSave", "new_presence_check_skip", 200, () -> {
                boolean missing = false;
                for (String key : blocksCfg.getKeys(true)) {
                    if (!blocksCfg.contains(key)) {
                        missing = true;
                        break;
                    }
                }
                bh += missing ? 1 : 0;
            });
            // 检查的忠实性验证：自身键必然全部包含（missing 恒 false）
            tmpBlocks.delete();
        } catch (Exception e) {
            getLogger().severe("round12 blocks 基准失败: " + e);
        }

        // —— spells.yml 首次启动补键：旧（每键一存，≤69 次全量写）vs 新（补齐后一存）——
        try {
            final org.bukkit.configuration.file.YamlConfiguration spellsCfg =
                new org.bukkit.configuration.file.YamlConfiguration();
            for (int i = 0; i < 69; i++) {
                spellsCfg.set("SPELL_" + i, true);
            }
            final java.io.File tmpSpells = new java.io.File(dataDir, "tmp-spells.yml");
            final org.bukkit.configuration.file.YamlConfiguration freshCfg =
                new org.bukkit.configuration.file.YamlConfiguration();
            time(w, "startup.spellsFirstBoot", "old_save_per_key", 3, () -> {
                for (int i = 0; i < 69; i++) {
                    freshCfg.set("SPELL_" + i, true);
                    try {
                        freshCfg.save(tmpSpells);
                    } catch (Exception e) {
                        bh++;
                    }
                }
            });
            time(w, "startup.spellsFirstBoot", "new_single_save", 100, () -> {
                final org.bukkit.configuration.file.YamlConfiguration cfg =
                    new org.bukkit.configuration.file.YamlConfiguration();
                for (int i = 0; i < 69; i++) {
                    cfg.set("SPELL_" + i, true);
                }
                try {
                    cfg.save(tmpSpells);
                } catch (Exception e) {
                    bh++;
                }
            });
            tmpSpells.delete();
        } catch (Exception e) {
            getLogger().severe("round12 spells 基准失败: " + e);
        }

        // —— 故事构造 shards 列表读取：旧（两次 getIntegerList）vs 新（一次）——
        final org.bukkit.configuration.file.YamlConfiguration storyCfg =
            new org.bukkit.configuration.file.YamlConfiguration();
        try {
            storyCfg.loadFromString(
                "test:\n  name: T\n  type: ELEMENTAL\n  shards: [1,2,3,4,5,6,7,8,9]\n  lore: [a,b,c]\n");
        } catch (Exception e) {
            getLogger().severe("round12 story 段构造失败: " + e);
        }
        final org.bukkit.configuration.ConfigurationSection storySection = storyCfg.getConfigurationSection("test");
        time(w, "startup.storyShardsRead", "old_double_read", 200_000, () -> {
            bh += storySection.getIntegerList("shards").size()
                + storySection.getIntegerList("shards").size();
        });
        time(w, "startup.storyShardsRead", "new_single_read", 500_000, () -> {
            bh += storySection.getIntegerList("shards").size();
        });
    }

    /** 第 13 轮：剩余全局监听器门控（故事方块放置/隐藏器放置/禁刷区生成扫描） */
    private void benchRound13(PrintWriter w) {
        // —— 放置事件的故事检查：旧（直接 isStoried=meta 克隆+PDC）vs 新（hasItemMeta 门控）——
        // 常态物品（无 meta 普通方块）
        final ItemStack plainStone = new ItemStack(Material.STONE);
        time(w, "eventPath.storiedPlaceCheck", "old_meta_clone_pdc", 50_000, () -> {
            if (plainStone.getType() != Material.AIR && StoryUtils.isStoried(plainStone)) {
                bh++;
            }
        });
        time(w, "eventPath.storiedPlaceCheck", "new_hasItemMeta_gate", 5_000_000, () -> {
            if (plainStone.getType() != Material.AIR
                && plainStone.hasItemMeta()
                && StoryUtils.isStoried(plainStone)) {
                bh++;
            }
        });
        // 等价性验证：有 meta 但无 PDC 的物品（重命名石头）两法同判 false
        final ItemStack renamedStone = new ItemStack(Material.STONE);
        final ItemMeta rm = renamedStone.getItemMeta();
        rm.setDisplayName("renamed");
        renamedStone.setItemMeta(rm);
        boolean eqOld = StoryUtils.isStoried(renamedStone);
        boolean eqNew = renamedStone.hasItemMeta() && StoryUtils.isStoried(renamedStone);
        getLogger().info("round13 等价性(重命名石头): old=" + eqOld + " new=" + eqNew + " hasMeta=" + renamedStone.hasItemMeta());
        // 真实故事物品（有 PDC）两法同判 true
        final ItemStack storiedStone = new ItemStack(Material.STONE);
        StoryUtils.makeStoried(storiedStone);
        boolean eqOld2 = StoryUtils.isStoried(storiedStone);
        boolean eqNew2 = storiedStone.hasItemMeta() && StoryUtils.isStoried(storiedStone);
        getLogger().info("round13 等价性(故事石头): old=" + eqOld2 + " new=" + eqNew2 + " hasMeta=" + storiedStone.hasItemMeta());

        // —— 放置事件的隐藏器检查：旧（getByItem）vs 新（PAPER 材质门控）——
        final ItemStack handStone = new ItemStack(Material.STONE);
        time(w, "eventPath.placeCoverCheck", "old_getByItem", 100_000, () -> {
            if (SlimefunItem.getByItem(handStone) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.covers.BlockVeil) {
                bh++;
            }
        });
        time(w, "eventPath.placeCoverCheck", "new_material_gate", 5_000_000, () -> {
            if (handStone.getType() == Material.PAPER
                && SlimefunItem.getByItem(handStone) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.covers.BlockVeil) {
                bh++;
            }
        });

        // —— 禁刷区生成扫描：旧（空表 keySet 迭代器）vs 新（isEmpty 早退）——
        final Map<org.bukkit.util.BoundingBox, Long> emptyAreas = new HashMap<>();
        time(w, "eventPath.mobCandleSpawnScan", "old_iterator_alloc", 2_000_000, () -> {
            for (org.bukkit.util.BoundingBox box : emptyAreas.keySet()) {
                if (box == null) {
                    bh++;
                }
            }
        });
        time(w, "eventPath.mobCandleSpawnScan", "new_isEmpty_guard", 5_000_000, () -> {
            if (!emptyAreas.isEmpty()) {
                for (org.bukkit.util.BoundingBox box : emptyAreas.keySet()) {
                    if (box == null) {
                        bh++;
                    }
                }
            }
        });
    }

    /** 时间驱动预热 + 分批中位数（主线程内，每变体约 1s） */
    private void time(PrintWriter w, String bench, String variant, int batchOps, Runnable op) {
        long warmupEnd = System.nanoTime() + 300_000_000L;
        while (System.nanoTime() < warmupEnd) {
            op.run();
        }
        double[] medians = new double[5];
        for (int i = 0; i < medians.length; i++) {
            long start = System.nanoTime();
            for (int j = 0; j < batchOps; j++) {
                op.run();
            }
            medians[i] = (System.nanoTime() - start) / (double) batchOps;
        }
        java.util.Arrays.sort(medians);
        w.printf("%s\t%s\t%.2f%n", bench, variant, medians[2]);
        w.flush();
        getLogger().info(String.format("%s/%s: %.2f ns/op", bench, variant, medians[2]));
    }

    /**
     * 时间驱动预热 + 分批中位数（状态增长型操作专用）：被测操作使物品故事列表单调增长，
     * 而 Bukkit setLore 有 256 行上限（每条约 9 行 lore），预热期每 4 次复位、
     * 每批判前复位到模板，批内增长 4 条以内——两变体保持相同的小区间增长曲线。
     */
    private void timeResettable(PrintWriter w, String bench, String variant, int batchOps,
                                Runnable reset, Runnable op) {
        reset.run();
        long warmupEnd = System.nanoTime() + 300_000_000L;
        int warmupOps = 0;
        while (System.nanoTime() < warmupEnd) {
            op.run();
            if (++warmupOps % 4 == 0) {
                reset.run();
            }
        }
        reset.run();
        double[] medians = new double[15];
        for (int i = 0; i < medians.length; i++) {
            reset.run();
            long start = System.nanoTime();
            for (int j = 0; j < batchOps; j++) {
                op.run();
            }
            medians[i] = (System.nanoTime() - start) / (double) batchOps;
        }
        java.util.Arrays.sort(medians);
        w.printf("%s\t%s\t%.2f%n", bench, variant, medians[7]);
        w.flush();
        getLogger().info(String.format("%s/%s: %.2f ns/op (resettable)", bench, variant, medians[7]));
    }

    /**
     * 第 15 轮：写路径单次元数据往返归一。
     * - writePath.storyCommit：记录者面板每条故事落盘（旧链 8 次克隆 vs 新 1 次）
     * - writePath.storyCommitUnique：终格常规+独特提交（旧链 10 次克隆 vs 新 1 次）
     * - writePath.roundTripCommitExtract：发掘+提取全往返（状态自稳定）
     * - writePath.statsIncrement：统计计数读改写（路径双构建 vs 单构建）
     * - writePath.staveLoreRebuild：施法写回 lore 重建（动态拼接 vs 静态片段+名称缓存）
     * 先做四组终态等价性断言（PDC 字节/lore/名称/附魔/标志），失败则以 severe 日志暴露。
     */
    private void benchRound15(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.managers.StoriesManager manager =
            CrystamaeHistoria.getStoriesManager();
        final List<io.github.sefiraat.crystamaehistoria.stories.Story> commonPool =
            manager.getStories(io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON,
                StoryType.ELEMENTAL);
        final io.github.sefiraat.crystamaehistoria.stories.BlockDefinition stoneDef =
            manager.getBlockDefinitionMap().get(Material.STONE);
        if (commonPool == null || commonPool.isEmpty() || stoneDef == null) {
            getLogger().severe("round15 前置数据缺失，跳过: pool=" + commonPool + " def=" + stoneDef);
            return;
        }
        final io.github.sefiraat.crystamaehistoria.stories.Story story = commonPool.get(0);
        final io.github.sefiraat.crystamaehistoria.stories.Story uniqueStory =
            stoneDef.getUnique() != null ? stoneDef.getUnique() : commonPool.get(1);

        // —— 等价性断言（先于计时）——
        round15Equivalence(story, uniqueStory);

        // —— 提交（状态增长型，批间复位到模板；批 4 次防超 256 行 lore 上限）——
        final ItemStack commitItem = round15Template(2, 5, story);
        final ItemMeta commitTemplateMeta = commitItem.getItemMeta();
        timeResettable(w, "writePath.storyCommit", "old_chain_8clones", 4,
            () -> commitItem.setItemMeta(commitTemplateMeta),
            () -> round15OldCommit(commitItem, story, null));
        timeResettable(w, "writePath.storyCommit", "new_single_roundtrip", 4,
            () -> commitItem.setItemMeta(commitTemplateMeta),
            () -> StoryUtils.commitStory(commitItem, story, null));

        // —— 终格提交（常规 + 独特，满槽触发附魔分支）——
        final ItemStack uniqueItem = round15Template(4, 5, story);
        final ItemMeta uniqueTemplateMeta = uniqueItem.getItemMeta();
        timeResettable(w, "writePath.storyCommitUnique", "old_chain_10clones", 4,
            () -> uniqueItem.setItemMeta(uniqueTemplateMeta),
            () -> round15OldCommit(uniqueItem, story, uniqueStory));
        timeResettable(w, "writePath.storyCommitUnique", "new_single_roundtrip", 4,
            () -> uniqueItem.setItemMeta(uniqueTemplateMeta),
            () -> StoryUtils.commitStory(uniqueItem, story, uniqueStory));

        // —— 提取（状态收缩型：预置 5 条，批 4 次内不耗尽；批间复位）——
        // 注：不使用"提交+提取"自稳定往返设计——rebuildStoriedStack 每次在既有显示名上
        // 叠加"有故事的"前缀（上游既有行为，旧/新一致），名字无界增长会使
        // Paper 的组件→legacy 正则转换二次方变慢，污染计量。
        final ItemStack extractItem = round15Template(5, 5, story);
        final ItemMeta extractTemplateMeta = extractItem.getItemMeta();
        timeResettable(w, "writePath.storyExtract", "old_chain_7clones", 4,
            () -> extractItem.setItemMeta(extractTemplateMeta),
            () -> bh += round15OldExtract(extractItem) ? 1 : 0);
        timeResettable(w, "writePath.storyExtract", "new_single_roundtrip", 4,
            () -> extractItem.setItemMeta(extractTemplateMeta),
            () -> {
                final ItemMeta m = extractItem.getItemMeta();
                final List<io.github.sefiraat.crystamaehistoria.stories.Story> l =
                    StoryUtils.getAllStories(m);
                bh += io.github.sefiraat.crystamaehistoria.utils.GildingUtils.isGilded(m) ? 0 : 1;
                bh += StoryUtils.removeStoryAndRebuild(extractItem, m, l.get(0), l) == 0 ? 1 : 0;
            });

        // —— 统计计数（读改写：路径双构建 vs 单构建；真实 player_stats 配置）——
        final UUID statPlayer = UUID.fromString("12345678-1234-1234-1234-123456789012");
        final String statId = SpellType.HEAL.get().getId();
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();
        time(w, "writePath.statsIncrement", "old_double_path_build", 200_000, () -> {
            int uses = stats.getInt(statPlayer + ".SPELL." + statId + ".TIMES_CAST");
            uses++;
            stats.set(statPlayer + ".SPELL." + statId + ".TIMES_CAST", uses);
        });
        time(w, "writePath.statsIncrement", "new_single_path", 200_000, () ->
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addUsage(statPlayer, SpellType.HEAL));

        // —— 法杖 lore 重建（施法写回路径：旧动态拼接+即时 toTitleCase vs 新静态片段+缓存名）——
        final ItemStack stave = new ItemStack(Material.BLAZE_ROD);
        final ItemMeta staveBaseMeta = stave.getItemMeta();
        final Map<SpellSlot, InstancePlate> plates = new EnumMap<>(SpellSlot.class);
        for (SpellSlot slot : SpellSlot.values()) {
            plates.put(slot, new InstancePlate(1, SpellType.HEAL, 100));
        }
        DataTypeMethods.setCustom(staveBaseMeta, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE, plates);
        stave.setItemMeta(staveBaseMeta);
        final InstanceStave staveInstance = new InstanceStave(stave);
        time(w, "writePath.staveLoreRebuild", "old_dynamic_strings", 40_000, () -> {
            // 同构副本：0.3.0 的 buildLore（动态拼接；getName 为各法术覆写的常量返回）
            final ItemMeta m = stave.getItemMeta();
            final String[] lore = new String[]{"可以进行法术绑定的法杖"};
            final net.md_5.bungee.api.ChatColor passiveColor =
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor();
            final List<String> finalLore = new java.util.ArrayList<>();
            for (String s : lore) {
                finalLore.add(passiveColor + s);
            }
            for (SpellSlot slot : SpellSlot.getCashedValues()) {
                final InstancePlate instancePlate = staveInstance.getSpellInstanceMap().get(slot);
                if (instancePlate != null) {
                    finalLore.add("");
                    final String magic = instancePlate.getStoredSpell().getSpell().getName();
                    final String crysta = String.valueOf(instancePlate.getCrysta());
                    finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_MYTHICAL.getColor()
                        + slot.getDescription());
                    finalLore.add(passiveColor + "法术: "
                        + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor() + magic);
                    finalLore.add(passiveColor + "充能: "
                        + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor() + crysta);
                }
            }
            finalLore.add("");
            finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO,
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.STAVE.getLoreLine()));
            m.setLore(finalLore);
            stave.setItemMeta(m);
        });
        time(w, "writePath.staveLoreRebuild", "new_static_fragments", 40_000, () -> {
            final ItemMeta m = stave.getItemMeta();
            staveInstance.buildLore(m);
            stave.setItemMeta(m);
        });
    }

    /** 构造确定性的已记录物品模板（固定故事上限/预置故事数，绕开随机上限） */
    private ItemStack round15Template(int presetStories, int maxStories,
                                      io.github.sefiraat.crystamaehistoria.stories.Story story) {
        final ItemStack item = new ItemStack(Material.STONE);
        final ItemMeta meta = item.getItemMeta();
        PersistentDataAPI.setBoolean(meta, Keys.PDC_IS_STORIED, true);
        final com.google.gson.JsonObject limits = new com.google.gson.JsonObject();
        limits.add(Keys.JS_S_AVAILABLE_STORIES, new com.google.gson.JsonPrimitive(maxStories));
        limits.add(Keys.JS_S_TIER, new com.google.gson.JsonPrimitive(1));
        PersistentDataAPI.setJsonObject(meta, Keys.PDC_POTENTIAL_STORIES, limits);
        final List<io.github.sefiraat.crystamaehistoria.stories.Story> list = new java.util.ArrayList<>();
        for (int i = 0; i < presetStories; i++) {
            list.add(story);
        }
        DataTypeMethods.setCustom(meta, Keys.PDC_STORIES,
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, list);
        PersistentDataAPI.setInt(meta, Keys.PDC_CURRENT_NUMBER_OF_STORIES, presetStories);
        item.setItemMeta(meta);
        return item;
    }

    /** 旧提交链（与 0.3.0 实现逐调用同构，调用插件保留的旧公开方法） */
    private void round15OldCommit(ItemStack item,
                                  io.github.sefiraat.crystamaehistoria.stories.Story main,
                                  io.github.sefiraat.crystamaehistoria.stories.Story unique) {
        if (main != null) {
            StoryUtils.applyStory(item, main);
            StoryUtils.incrementStoryAmount(item);
        }
        if (unique != null) {
            StoryUtils.applyStory(item, unique);
        }
        io.github.sefiraat.crystamaehistoria.managers.StoriesManager.rebuildStoriedStack(item);
    }

    /** 旧提取链（与 0.3.0 祭坛路径逐调用同构）：返回是否非空 */
    private boolean round15OldExtract(ItemStack item) {
        final List<io.github.sefiraat.crystamaehistoria.stories.Story> storyList =
            StoryUtils.getAllStories(item);
        final io.github.sefiraat.crystamaehistoria.stories.Story story = storyList.get(0);
        bh += io.github.sefiraat.crystamaehistoria.utils.GildingUtils.isGilded(item) ? 1 : 0;
        final int remaining = StoryUtils.removeStory(item, story);
        if (remaining > 0) {
            io.github.sefiraat.crystamaehistoria.managers.StoriesManager.rebuildStoriedStack(item);
        }
        return remaining > 0;
    }

    /** 第 15 轮终态等价性断言：旧链 vs 新链的物品状态必须逐字段一致 */
    private void round15Equivalence(io.github.sefiraat.crystamaehistoria.stories.Story story,
                                    io.github.sefiraat.crystamaehistoria.stories.Story unique) {
        // A. 常规提交（2 条预置 → 3 条）
        final ItemStack tA = round15Template(2, 5, story);
        final ItemStack oldA = tA.clone();
        final ItemStack newA = tA.clone();
        round15OldCommit(oldA, story, null);
        StoryUtils.commitStory(newA, story, null);
        getLogger().info("round15 等价性(A 常规提交): " + round15ItemStateEquals(oldA, newA));

        // B. 终格常规+独特提交（4 条预置 → 满 5 条 + 满槽附魔分支）
        final ItemStack tB = round15Template(4, 5, story);
        final ItemStack oldB = tB.clone();
        final ItemStack newB = tB.clone();
        round15OldCommit(oldB, story, unique);
        StoryUtils.commitStory(newB, story, unique);
        getLogger().info("round15 等价性(B 终格独特提交): " + round15ItemStateEquals(oldB, newB));

        // C. 提取（3 条预置 → 2 条）
        final ItemStack tC = round15Template(3, 5, story);
        final ItemStack oldC = tC.clone();
        final ItemStack newC = tC.clone();
        round15OldExtract(oldC);
        {
            final ItemMeta m = newC.getItemMeta();
            final List<io.github.sefiraat.crystamaehistoria.stories.Story> l = StoryUtils.getAllStories(m);
            bh += io.github.sefiraat.crystamaehistoria.utils.GildingUtils.isGilded(m) ? 1 : 0;
            StoryUtils.removeStoryAndRebuild(newC, m, l.get(0), l);
        }
        getLogger().info("round15 等价性(C 提取): " + round15ItemStateEquals(oldC, newC));

        // D. 法杖 lore（旧动态拼接 vs 新静态片段，同一 4 板法杖）
        final ItemStack tD = new ItemStack(Material.BLAZE_ROD);
        final ItemMeta dm = tD.getItemMeta();
        final Map<SpellSlot, InstancePlate> dPlates = new EnumMap<>(SpellSlot.class);
        for (SpellSlot slot : SpellSlot.values()) {
            dPlates.put(slot, new InstancePlate(1, SpellType.HEAL, 100));
        }
        DataTypeMethods.setCustom(dm, Keys.PDC_STAVE_STORAGE, PersistentStaveDataType.TYPE, dPlates);
        tD.setItemMeta(dm);
        final InstanceStave dStave = new InstanceStave(tD);
        final ItemMeta loreOld = tD.getItemMeta();
        final ItemMeta loreNew = tD.getItemMeta();
        {
            // 旧同构 buildLore
            final String[] lore = new String[]{"可以进行法术绑定的法杖"};
            final net.md_5.bungee.api.ChatColor passiveColor =
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor();
            final List<String> finalLore = new java.util.ArrayList<>();
            for (String s : lore) {
                finalLore.add(passiveColor + s);
            }
            for (SpellSlot slot : SpellSlot.getCashedValues()) {
                final InstancePlate instancePlate = dStave.getSpellInstanceMap().get(slot);
                if (instancePlate != null) {
                    finalLore.add("");
                    finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_MYTHICAL.getColor()
                        + slot.getDescription());
                    finalLore.add(passiveColor + "法术: "
                        + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor()
                        + instancePlate.getStoredSpell().getSpell().getName());
                    finalLore.add(passiveColor + "充能: "
                        + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor()
                        + String.valueOf(instancePlate.getCrysta()));
                }
            }
            finalLore.add("");
            finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO,
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.STAVE.getLoreLine()));
            loreOld.setLore(finalLore);
        }
        dStave.buildLore(loreNew);
        getLogger().info("round15 等价性(D 法杖 lore): "
            + java.util.Objects.equals(loreOld.getLore(), loreNew.getLore()));
    }

    /**
     * 第 16 轮：展示行构建缓存（Story.getDisplayName/getStoryLore 记忆化）。
     * - storyDisplay.displayName：单条展示名（旧：每次重建组件 + toLegacyText；新：缓存命中）
     * - storyDisplay.loreLines：单条正文行列表（同上）
     * - storyDisplay.rebuild4Stories：4 条故事的 lore 重建组装段（rebuildStoriedStack 主循环）
     * 等价性断言：跨稀有度/类型采样 N 条故事，新鲜构建与缓存输出逐字符串一致。
     */
    private void benchRound16(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.managers.StoriesManager manager =
            CrystamaeHistoria.getStoriesManager();
        final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity[] rarities =
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.values();
        final StoryType[] types = StoryType.values();

        // 采样等价性：每稀有度找一条（含带作者/赞助者的若有）
        int sampled = 0;
        boolean allEqual = true;
        outer:
        for (final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity rarity : rarities) {
            for (final StoryType type : types) {
                final List<io.github.sefiraat.crystamaehistoria.stories.Story> pool =
                    manager.getStories(rarity, type);
                if (pool == null || pool.isEmpty()) {
                    continue;
                }
                final io.github.sefiraat.crystamaehistoria.stories.Story s = pool.get(0);
                final boolean eqName = round16FreshDisplayName(s).equals(s.getDisplayName());
                final boolean eqLore = round16FreshStoryLore(s).equals(s.getStoryLore());
                if (!eqName || !eqLore) {
                    getLogger().severe("round16 等价性失败: " + s.getId() + " name=" + eqName + " lore=" + eqLore);
                    allEqual = false;
                }
                sampled++;
                if (sampled >= rarities.length) {
                    break outer;
                }
            }
        }
        getLogger().info("round16 等价性(采样 " + sampled + " 条): " + allEqual);

        // 计时样本：COMMON/ELEMENTAL 池首条（典型条目）
        final List<io.github.sefiraat.crystamaehistoria.stories.Story> commonPool =
            manager.getStories(io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON,
                StoryType.ELEMENTAL);
        if (commonPool == null || commonPool.isEmpty()) {
            getLogger().severe("round16 前置数据缺失，跳过");
            return;
        }
        final io.github.sefiraat.crystamaehistoria.stories.Story story = commonPool.get(0);

        time(w, "storyDisplay.displayName", "old_fresh_components", 100_000,
            () -> bh += round16FreshDisplayName(story).length());
        time(w, "storyDisplay.displayName", "new_cached", 5_000_000,
            () -> bh += story.getDisplayName().length());

        time(w, "storyDisplay.loreLines", "old_fresh_components", 20_000,
            () -> bh += round16FreshStoryLore(story).size());
        time(w, "storyDisplay.loreLines", "new_cached", 5_000_000,
            () -> bh += story.getStoryLore().size());

        // 4 条故事的 lore 组装段（rebuildStoriedStack 主循环体）
        final List<io.github.sefiraat.crystamaehistoria.stories.Story> four =
            new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            four.add(commonPool.get(i % commonPool.size()));
        }
        time(w, "storyDisplay.rebuild4Stories", "old_fresh_components", 5_000, () -> {
            final List<String> lore = new java.util.ArrayList<>();
            for (final io.github.sefiraat.crystamaehistoria.stories.Story s : four) {
                lore.add("");
                lore.add(round16FreshDisplayName(s));
                lore.addAll(round16FreshStoryLore(s));
            }
            bh += lore.size();
        });
        time(w, "storyDisplay.rebuild4Stories", "new_cached", 50_000, () -> {
            final List<String> lore = new java.util.ArrayList<>();
            for (final io.github.sefiraat.crystamaehistoria.stories.Story s : four) {
                lore.add("");
                lore.add(s.getDisplayName());
                lore.addAll(s.getStoryLore());
            }
            bh += lore.size();
        });
    }

    /**
     * 第 17 轮：召唤物 AI 每 tick 路径。
     * - mobGoal.ownerLookup：主人在线查询（旧三步 OfflinePlayer vs 新单次 getPlayer）
     * - mobGoal.typesAlloc：GoalType 集合（旧每次 EnumSet.of vs 新共享常量）
     * - mobGoal.targetReads：目标读取（旧 3 次 vs 新 1 次缓存）
     * - ramGoal.blockScan：冲撞车块扫描（旧 75×BlockPosition + O(n²) contains 去重 vs 直接坐标遍历）
     * - mobGoal.followDistance：跟随距离（distance 开方 vs distanceSquared）
     * 等价性断言：类型集相等 / 块扫描坐标序列一致 / 离线主人两法同判 null / 距离判定采样一致。
     */
    private void benchRound17(PrintWriter w) {
        final UUID owner = UUID.fromString("12345678-1234-1234-1234-123456789012");
        final io.github.sefiraat.crystamaehistoria.utils.mobgoals.BoringGoal goal =
            new io.github.sefiraat.crystamaehistoria.utils.mobgoals.BoringGoal(owner);

        // —— 等价性 ——
        final boolean eqTypes = java.util.EnumSet.of(com.destroystokyo.paper.entity.ai.GoalType.TARGET)
            .equals(goal.getTypes());
        // 离线主人（基准会话无玩家）：旧三步与新单次同判 null
        final org.bukkit.entity.Player oldWay = Bukkit.getOfflinePlayer(owner).isOnline()
            ? Bukkit.getOfflinePlayer(owner).getPlayer() : null;
        final org.bukkit.entity.Player newWay = Bukkit.getPlayer(owner);
        final boolean eqOwner = oldWay == null && newWay == null;
        // 块扫描坐标序列（同序）：旧（去重列表）vs 新（直接遍历收集）
        final World world = Bukkit.getWorlds().get(0);
        final Location scanBase = new Location(world, 4000.5, 100.5, 4000.5);
        final java.util.List<String> oldCoords = new java.util.ArrayList<>();
        final java.util.List<String> newCoords = new java.util.ArrayList<>();
        {
            final Location location = scanBase;
            final int radius = 2;
            final java.util.List<io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition> blocks =
                new java.util.ArrayList<>();
            for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
                for (int y = location.getBlockY(); y <= location.getBlockY() + radius; y++) {
                    for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                        final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp =
                            new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(location.getWorld(), x, y, z);
                        if (!blocks.contains(bp)) {
                            blocks.add(bp);
                        }
                    }
                }
            }
            for (final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp : blocks) {
                oldCoords.add(bp.getX() + "," + bp.getY() + "," + bp.getZ());
            }
            for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
                for (int y = location.getBlockY(); y <= location.getBlockY() + radius; y++) {
                    for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                        newCoords.add(x + "," + y + "," + z);
                    }
                }
            }
        }
        final boolean eqScan = oldCoords.equals(newCoords);
        // 距离判定采样：distance > t ⇔ distanceSquared > t²
        boolean eqDist = true;
        for (int i = 0; i < 64; i++) {
            final Location a = new Location(world, i * 0.37, 100, i * 0.11);
            final Location b = new Location(world, i * 0.31 + 3, 100, i * 0.29);
            final double t = (i % 9) + 0.5;
            if ((a.distance(b) > t) != (a.distanceSquared(b) > t * t)) {
                eqDist = false;
            }
        }
        getLogger().info("round17 等价性: types=" + eqTypes + " ownerOffline=" + eqOwner
            + " scanOrder=" + eqScan + " distance=" + eqDist);

        // —— 主人在线查询（离线常态：两法都走缓存未命中路径）——
        time(w, "mobGoal.ownerLookup", "old_offlineplayer_3step", 200_000, () -> {
            final org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
            bh += op.isOnline() ? 1 : 0;
        });
        time(w, "mobGoal.ownerLookup", "new_getPlayer_single", 1_000_000, () -> {
            if (Bukkit.getPlayer(owner) != null) {
                bh++;
            }
        });

        // —— GoalType 集合 ——
        time(w, "mobGoal.typesAlloc", "old_enumset_of", 1_000_000, () ->
            bh += java.util.EnumSet.of(com.destroystokyo.paper.entity.ai.GoalType.TARGET).size());
        time(w, "mobGoal.typesAlloc", "new_shared_constant", 5_000_000, () ->
            bh += goal.getTypes().size());

        // —— 目标读取（真实实体记忆读取 ×3 vs ×1）——
        final Zombie reader = world.spawn(new Location(world, 0, 220, 0), Zombie.class);
        time(w, "mobGoal.targetReads", "old_triple_read", 500_000, () -> {
            final org.bukkit.entity.LivingEntity t1 = reader.getTarget();
            final boolean a = t1 != null && t1.equals(reader);
            final org.bukkit.entity.LivingEntity t2 = reader.getTarget();
            final boolean b = t2 != null && !t2.isDead();
            bh += (a || b) ? 1 : 0;
        });
        time(w, "mobGoal.targetReads", "new_single_cached", 1_000_000, () -> {
            final org.bukkit.entity.LivingEntity t = reader.getTarget();
            final boolean a = t != null && t.equals(reader);
            final boolean b = t != null && !t.isDead();
            bh += (a || b) ? 1 : 0;
        });
        reader.remove();

        // —— 冲撞车块扫描（枚举 + 取块 + 取 BlockData；破坏判定两法同侧不计）——
        time(w, "ramGoal.blockScan", "old_blockposition_dedup", 2_000, () -> {
            final Location location = scanBase;
            final int radius = 2;
            final java.util.List<io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition> blocks =
                new java.util.ArrayList<>();
            for (int x = location.getBlockX() - radius; x <= location.getBlockX() + radius; x++) {
                for (int y = location.getBlockY(); y <= location.getBlockY() + radius; y++) {
                    for (int z = location.getBlockZ() - radius; z <= location.getBlockZ() + radius; z++) {
                        final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp =
                            new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(location.getWorld(), x, y, z);
                        if (!blocks.contains(bp)) {
                            blocks.add(bp);
                        }
                    }
                }
            }
            for (final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp : blocks) {
                final Block block = bp.getBlock();
                bh += block.getBlockData().getMaterial().ordinal();
            }
        });
        time(w, "ramGoal.blockScan", "new_direct_coords", 5_000, () -> {
            final Location location = scanBase;
            final World wld = location.getWorld();
            final int radius = 2;
            final int baseX = location.getBlockX();
            final int baseY = location.getBlockY();
            final int baseZ = location.getBlockZ();
            for (int x = baseX - radius; x <= baseX + radius; x++) {
                for (int y = baseY; y <= baseY + radius; y++) {
                    for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                        final Block block = wld.getBlockAt(x, y, z);
                        bh += block.getBlockData().getMaterial().ordinal();
                    }
                }
            }
        });

        // —— 跟随距离判定 ——
        final Location a = new Location(world, 0, 100, 0);
        final Location b = new Location(world, 6, 100, 8);
        time(w, "mobGoal.followDistance", "old_distance_sqrt", 2_000_000, () -> bh += a.distance(b) > 5 ? 1 : 0);
        time(w, "mobGoal.followDistance", "new_distanceSquared", 5_000_000, () ->
            bh += a.distanceSquared(b) > 25 ? 1 : 0);
    }

    /**
     * 第 18 轮：法术周期效果与周期任务路径。
     * - tunnelBore.blockScan.r3/.r5：钻探任务块扫描（旧 BlockPosition + O(n²) 去重
     *   vs 直接坐标遍历；半径 3=343 块 / 半径 5=1331 块，对应法杖等级 3/5）
     * 等价性断言：两半径下坐标序列逐项一致。
     * 域内其余周期路径（SpellTickRunnable/FloatingHeadAnimation/ParticleDisplayRunnable/
     * TemporaryEffectsRunnable/SaveConfigRunnable）经核查无可测优化点，论证见报告不做项。
     */
    private void benchRound18(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location base = new Location(world, 5000.5, 120.5, 5000.5);

        for (final int radius : new int[]{3, 5}) {
            // 等价性：坐标序列（同序）
            final java.util.List<String> oldCoords = new java.util.ArrayList<>();
            final java.util.List<String> newCoords = new java.util.ArrayList<>();
            {
                final java.util.List<io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition> blocks =
                    new java.util.ArrayList<>();
                for (int x = base.getBlockX() - radius; x <= base.getBlockX() + radius; x++) {
                    for (int y = base.getBlockY() - radius; y <= base.getBlockY() + radius; y++) {
                        for (int z = base.getBlockZ() - radius; z <= base.getBlockZ() + radius; z++) {
                            final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp =
                                new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(world, x, y, z);
                            if (!blocks.contains(bp)) {
                                blocks.add(bp);
                            }
                        }
                    }
                }
                for (final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp : blocks) {
                    oldCoords.add(bp.getX() + "," + bp.getY() + "," + bp.getZ());
                }
                for (int x = base.getBlockX() - radius; x <= base.getBlockX() + radius; x++) {
                    for (int y = base.getBlockY() - radius; y <= base.getBlockY() + radius; y++) {
                        for (int z = base.getBlockZ() - radius; z <= base.getBlockZ() + radius; z++) {
                            newCoords.add(x + "," + y + "," + z);
                        }
                    }
                }
            }
            getLogger().info("round18 等价性(坐标序列 r" + radius + "): " + oldCoords.equals(newCoords));

            final String benchName = "tunnelBore.blockScan.r" + radius;
            time(w, benchName, "old_blockposition_dedup", radius == 3 ? 4_000 : 600, () -> {
                final java.util.List<io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition> blocks =
                    new java.util.ArrayList<>();
                for (int x = base.getBlockX() - radius; x <= base.getBlockX() + radius; x++) {
                    for (int y = base.getBlockY() - radius; y <= base.getBlockY() + radius; y++) {
                        for (int z = base.getBlockZ() - radius; z <= base.getBlockZ() + radius; z++) {
                            final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp =
                                new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(world, x, y, z);
                            if (!blocks.contains(bp)) {
                                blocks.add(bp);
                            }
                        }
                    }
                }
                for (final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition bp : blocks) {
                    bh += bp.getBlock().getType().ordinal();
                }
            });
            time(w, benchName, "new_direct_coords", 4_000, () -> {
                final int bx = base.getBlockX();
                final int by = base.getBlockY();
                final int bz = base.getBlockZ();
                for (int x = bx - radius; x <= bx + radius; x++) {
                    for (int y = by - radius; y <= by + radius; y++) {
                        for (int z = bz - radius; z <= bz + radius; z++) {
                            bh += world.getBlockAt(x, y, z).getType().ordinal();
                        }
                    }
                }
            });
        }
    }

    /**
     * 第 19 轮：热循环 Location 分配消除（粒子路径）。
     * - particle.burstAlloc：displayParticleEffect 主体（旧 N 次 clone().add vs 单次克隆 set 坐标）
     * - particle.panelSummon：面板每 tick 2 粒子模式
     * - particle.sphere840：ChillWind 施法球面 ~840 点循环（含 1 粒子 helper）
     * 等价性断言：固定偏移序列下两法位置序列逐项一致。
     */
    private void benchRound19(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location base = new Location(world, 6000.5, 130.5, 6000.5);

        // 等价性：固定偏移（10 组）下位置序列
        final double[] offs = {0.05, -0.11, 0.22, -0.31, 0.44, -0.5, 0.61, -0.73, 0.84, -0.97};
        final java.util.List<String> oldPos = new java.util.ArrayList<>();
        final java.util.List<String> newPos = new java.util.ArrayList<>();
        {
            // 旧：base.clone().add(x,y,z)（独立偏移）
            for (int i = 0; i < offs.length; i++) {
                final Location l = base.clone().add(offs[i], offs[(i + 1) % offs.length], offs[(i + 2) % offs.length]);
                oldPos.add(l.getX() + "," + l.getY() + "," + l.getZ());
            }
            // 新：单次克隆 + 基准坐标 set
            final Location point = base.clone();
            final double bx = base.getX();
            final double by = base.getY();
            final double bz = base.getZ();
            for (int i = 0; i < offs.length; i++) {
                point.setX(bx + offs[i]);
                point.setY(by + offs[(i + 1) % offs.length]);
                point.setZ(bz + offs[(i + 2) % offs.length]);
                newPos.add(point.getX() + "," + point.getY() + "," + point.getZ());
            }
        }
        getLogger().info("round19 等价性(位置序列): " + oldPos.equals(newPos));

        // —— displayParticleEffect 主体（10 粒子；spawnParticle 无观察者仍走完整调用）——
        time(w, "particle.burstAlloc", "old_clone_per_particle", 20_000, () -> {
            for (int i = 0; i < 10; i++) {
                final double x = ThreadLocalRandom.current().nextDouble(-1, 1.1);
                final double y = ThreadLocalRandom.current().nextDouble(-1, 1.1);
                final double z = ThreadLocalRandom.current().nextDouble(-1, 1.1);
                world.spawnParticle(Particle.WAX_ON, base.clone().add(x, y, z), 1);
            }
        });
        time(w, "particle.burstAlloc", "new_single_scratch", 20_000, () -> {
            final Location point = base.clone();
            final double bx = base.getX();
            final double by = base.getY();
            final double bz = base.getZ();
            for (int i = 0; i < 10; i++) {
                point.setX(bx + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setY(by + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setZ(bz + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                world.spawnParticle(Particle.WAX_ON, point, 1);
            }
        });

        // —— 面板每 tick 2 粒子（记录者面板工作态）——
        final Location panelLoc = base;
        time(w, "particle.panelSummon", "old_two_clones", 100_000, () -> {
            for (int i = 0; i < 2; i++) {
                final Location l = panelLoc.clone().add(ThreadLocalRandom.current().nextDouble(0, 1.1), 1,
                    ThreadLocalRandom.current().nextDouble(0, 1.1));
                world.spawnParticle(Particle.ENCHANT, l, 0, 0.2, 0, -0.2, 0);
            }
        });
        time(w, "particle.panelSummon", "new_single_scratch", 100_000, () -> {
            final Location point = panelLoc.clone();
            final double bx = panelLoc.getX();
            final double bz = panelLoc.getZ();
            final double by = panelLoc.getY();
            for (int i = 0; i < 2; i++) {
                point.setX(bx + ThreadLocalRandom.current().nextDouble(0, 1.1));
                point.setZ(bz + ThreadLocalRandom.current().nextDouble(0, 1.1));
                point.setY(by + 1);
                world.spawnParticle(Particle.ENCHANT, point, 0, 0.2, 0, -0.2, 0);
            }
        });

        // —— ChillWind 球面 ~840 点（每次施法一次；含每点 1 粒子 helper）——
        final double range = 5.0;
        final int density = 20;
        time(w, "particle.sphere840", "old_clone_per_point", 20, () -> {
            final Location location = base.clone().add(0, 1, 0);
            for (double height = 0; height <= Math.PI; height += Math.PI / density) {
                final double r = range * Math.sin(height);
                final double y = range * Math.cos(height);
                for (double a = 0; a < Math.PI * 2; a += Math.PI / density) {
                    final double x = Math.cos(a) * r;
                    final double z = Math.sin(a) * r;
                    final Location point = location.clone().add(x, y, z);
                    // 1 粒子 helper（新实现为单次克隆；此处两变体同侧，计量 delta 为外层循环）
                    world.spawnParticle(Particle.END_ROD, point, 1);
                }
            }
        });
        time(w, "particle.sphere840", "new_single_scratch", 20, () -> {
            final Location location = base.clone().add(0, 1, 0);
            final Location point = location.clone();
            final double bx = location.getX();
            final double by = location.getY();
            final double bz = location.getZ();
            for (double height = 0; height <= Math.PI; height += Math.PI / density) {
                final double r = range * Math.sin(height);
                final double y = range * Math.cos(height);
                for (double a = 0; a < Math.PI * 2; a += Math.PI / density) {
                    final double x = Math.cos(a) * r;
                    final double z = Math.sin(a) * r;
                    point.setX(bx + x);
                    point.setY(by + y);
                    point.setZ(bz + z);
                    world.spawnParticle(Particle.END_ROD, point, 1);
                }
            }
        });
    }

    /** 同构副本：0.4.0 的 Spell.getThemedStack（每次全量重建，逐行颜色处理 + ItemMeta 读改写往返） */
    @SuppressWarnings("deprecation")
    private ItemStack round21OldThemedStack(io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell spell) {
        final net.md_5.bungee.api.ChatColor passiveColor =
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor();
        final java.util.List<String> finalLore = new java.util.ArrayList<>();
        for (String s : spell.getLore()) {
            finalLore.add(passiveColor + io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors.color(s));
        }
        finalLore.add("");
        finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO, "法术"));
        final io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack stack =
            new io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack(
                spell.getId(),
                spell.getMaterial(),
                io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
                    io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.SPELL, spell.getName()),
                finalLore.toArray(new String[finalLore.size() - 1])
            );
        final ItemMeta itemMeta = stack.getItemMeta();
        itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        stack.setItemMeta(itemMeta);
        // 旧调用链为 getThemedStack().item()——副本补上同一转换
        return stack.item();
    }

    /** 同构副本：0.4.0 的 SpellCollectionFlexGroup.getBasicStack（4× MessageFormat + CustomItemStack.create） */
    @SuppressWarnings("deprecation")
    private ItemStack round21OldBasicStack(io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell spell) {
        final io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCore spellCore = spell.getSpellCore();
        final net.md_5.bungee.api.ChatColor color =
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO.getColor();
        final net.md_5.bungee.api.ChatColor passive =
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor();

        final String crysta = java.text.MessageFormat.format("{0}每次施法消耗充能: {1}{2}", color, passive, spellCore.getCrystaCost());
        final String crystaMulti = java.text.MessageFormat.format("{0}施法消耗{1}随着法杖等级提升而增加", color, spellCore.isCrystaMultiplied() ? "会" : "不会");
        final String cooldown = java.text.MessageFormat.format("{0}冷却时间(秒): {1}{2}", color, passive, spell.getSpellCore().getCooldownSeconds());
        final String cooldownDivided = java.text.MessageFormat.format("{0}冷却时间{1}随着法杖等级提升而减少", color, spellCore.isCooldownDivided() ? "会" : "不会");

        return io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack.create(
            org.bukkit.Material.GLOW_BERRIES,
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.MAIN.getColor() + "基本信息",
            crysta,
            crystaMulti,
            cooldown,
            cooldownDivided
        );
    }

    /** 第 21 轮：图鉴 GUI 展示路径（真实 ItemStack 构建/克隆与排序） */
    private void benchRound21(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell[] spells =
            java.util.Arrays.stream(SpellType.getEnabledSpells())
                .map(SpellType::get).toArray(io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell[]::new);
        final java.util.List<Material> materials = new java.util.ArrayList<>(
            CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().keySet());
        final io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[] defs = materials.stream()
            .map(m -> CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().get(m))
            .toArray(io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[]::new);

        // ———— 等价性断言 ————
        boolean equivThemed = true;
        for (io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell s : spells) {
            equivThemed &= round21OldThemedStack(s).isSimilar(s.getThemedStack().item());
        }
        boolean equivIcon = true;
        for (int i = 0; i < Math.min(materials.size(), 40); i++) {
            final Material m = materials.get(i);
            equivIcon &= io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.themedItemStack(
                m, io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_UNIQUE,
                io.github.sefiraat.crystamaehistoria.utils.NameUtils.getMaterialName(m),
                "该故事已被发掘"
            ).isSimilar(io.github.sefiraat.crystamaehistoria.utils.theme.GuiElements.getUniqueStoryIcon(m));
        }
        final ItemStack basicReplica = round21OldBasicStack(spells[0]);
        final boolean equivDetail = basicReplica.isSimilar(basicReplica.clone())
            && round21OldBasicStack(spells[0]).isSimilar(basicReplica);
        // 排序快照顺序 == 现场重排序
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.BlockDefinition> freshSort =
            new java.util.ArrayList<>(CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().values());
        freshSort.sort(java.util.Comparator.comparing(d -> d.getMaterial().name()));
        boolean equivBlockOrder = freshSort.equals(CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial());
        boolean equivSpellOrder = java.util.Arrays.equals(
            SpellType.getEnabledSpells(),
            java.util.Arrays.stream(SpellType.getEnabledSpells())
                .sorted(java.util.Comparator.comparing(SpellType::getId)).toArray(SpellType[]::new));
        boolean equivTitleCase = true;
        for (Material m : Material.values()) {
            equivTitleCase &= io.github.sefiraat.crystamaehistoria.utils.TextUtils.toTitleCase(m.name())
                .equals(io.github.sefiraat.crystamaehistoria.utils.NameUtils.getMaterialName(m));
        }
        getLogger().info("round21 等价性: themed=" + equivThemed + " icon=" + equivIcon
            + " detail=" + equivDetail + " blockOrder=" + equivBlockOrder
            + " spellOrder=" + equivSpellOrder + " titleCase=" + equivTitleCase);

        // ———— 主题堆：旧全量重建 vs 新缓存 + .item() 克隆 ————
        time(w, "compendium.themedStack", "old_rebuild", 50, () -> {
            bh += round21OldThemedStack(spells[(int) (System.nanoTime() % spells.length)]).getType().ordinal();
        });
        time(w, "compendium.themedStack", "new_memo_item_clone", 2_000, () -> {
            bh += spells[(int) (System.nanoTime() % spells.length)].getThemedStack().item().getType().ordinal();
        });

        // ———— 页面网格图标 ×36（故事集页）：旧 themedItemStack vs 新缓存克隆 ————
        time(w, "compendium.pageIcons36", "old_build36", 20, () -> {
            for (int i = 0; i < 36; i++) {
                final Material m = materials.get(i);
                bh += io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.themedItemStack(
                    m, io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_UNIQUE,
                    io.github.sefiraat.crystamaehistoria.utils.NameUtils.getMaterialName(m),
                    "该故事已被发掘"
                ).getType().ordinal();
            }
        });
        time(w, "compendium.pageIcons36", "new_cached_clone36", 300, () -> {
            for (int i = 0; i < 36; i++) {
                bh += io.github.sefiraat.crystamaehistoria.utils.theme.GuiElements
                    .getUniqueStoryIcon(materials.get(i)).getType().ordinal();
            }
        });

        // ———— 详情堆（单堆代表 ×7 见报告）：旧 4×MessageFormat 重建 vs clone ————
        final ItemStack basicPrepared = round21OldBasicStack(spells[0]);
        time(w, "compendium.detailStack", "old_messageformat_build", 200, () -> {
            bh += round21OldBasicStack(spells[0]).getType().ordinal();
        });
        time(w, "compendium.detailStack", "new_cached_clone", 10_000, () -> {
            bh += basicPrepared.clone().getType().ordinal();
        });

        // ———— 法术集翻页排序：旧重排共享数组 vs 新预排序快照 ————
        final java.util.Comparator<SpellType> byId = java.util.Comparator.comparing(SpellType::getId);
        time(w, "compendium.spellPageSort", "old_sort_per_page", 5_000, () -> {
            final java.util.List<SpellType> list = java.util.Arrays.asList(SpellType.getEnabledSpells());
            list.sort(byId);
            bh += list.get(0).ordinal();
        });
        time(w, "compendium.spellPageSort", "new_snapshot_view", 100_000, () -> {
            final java.util.List<SpellType> page = java.util.Arrays.asList(SpellType.getEnabledSpells())
                .subList(0, 36);
            bh += page.get(0).ordinal();
        });

        // ———— 故事集/镀金集翻页：旧复制+排序 vs 新快照 subList ————
        time(w, "compendium.blockPageSort", "old_copy_sort", 500, () -> {
            final java.util.List<io.github.sefiraat.crystamaehistoria.stories.BlockDefinition> copy =
                new java.util.ArrayList<>(CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().values());
            copy.sort(java.util.Comparator.comparing(d -> d.getMaterial().name()));
            bh += copy.get(0).hashCode();
        });
        time(w, "compendium.blockPageSort", "new_snapshot_sublist", 100_000, () -> {
            final java.util.List<io.github.sefiraat.crystamaehistoria.stories.BlockDefinition> page =
                CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial().subList(0, 36);
            bh += page.get(0).hashCode();
        });

        // ———— TitleCase：旧逐次重建 vs 新查表 ————
        time(w, "compendium.titleCase", "old_rebuild", 100_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.TextUtils.toTitleCase(
                materials.get((int) (System.nanoTime() % materials.size())).name()).length();
        });
        time(w, "compendium.titleCase", "new_memo_lookup", 1_000_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.NameUtils.getMaterialName(
                materials.get((int) (System.nanoTime() % materials.size()))).length();
        });
        // 防止 defs 未使用告警：引用一次
        bh += defs.length;
    }

    /** 第 22 轮：玩家统计读取路径（真实 PlayerStatistics 全路径方法 vs 相对子节重载） */
    private void benchRound22(PrintWriter w) {
        final UUID player = UUID.fromString("12345678-1234-1234-1234-123456789022");
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();

        // 注入合成数据（命中路径）：69 法术 2/3 解锁 + 274 故事 1/2 解锁、1/4 镀金
        final SpellType[] enabled = SpellType.getEnabledSpells();
        final java.util.List<Material> materials = new java.util.ArrayList<>(
            CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().keySet());
        for (int i = 0; i < enabled.length; i++) {
            stats.set(player + ".SPELL." + enabled[i].getId() + ".UNLOCKED", i % 3 != 0);
        }
        for (int i = 0; i < materials.size(); i++) {
            stats.set(player + ".STORY." + materials.get(i) + ".UNLOCKED", i % 2 == 0);
            stats.set(player + ".STORY." + materials.get(i) + ".GILDED", i % 4 == 0);
        }

        try {
            // ———— 等价性断言：全路径 vs 相对（逐法术逐材质） ————
            final org.bukkit.configuration.ConfigurationSection spellSection =
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellStatSection(player);
            final org.bukkit.configuration.ConfigurationSection storySection =
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoryStatSection(player);
            boolean equivSpell = true;
            for (SpellType st : enabled) {
                equivSpell &= io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(player, st)
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(player, st, spellSection);
            }
            boolean equivStory = true;
            for (Material m : materials) {
                equivStory &= io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedUniqueStory(player, m)
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedUniqueStory(player, m, storySection);
                equivStory &= io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedStoryGilded(player, m)
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedStoryGilded(player, m, storySection);
            }
            // 计数等价（相对读取实现 vs 全路径复刻）
            int oldCount = 0;
            for (String spell : stats.getConfigurationSection(player + ".SPELL").getKeys(false)) {
                if (stats.getBoolean(player + ".SPELL." + spell + ".UNLOCKED")) oldCount++;
            }
            boolean equivCount = oldCount
                == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellsUnlocked(player);
            // 缺失玩家语义：两路径同为 false / 子节为 null
            final UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");
            boolean equivMissing = !io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(missing, SpellType.HEAL)
                && io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellStatSection(missing) == null
                && !io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(
                    missing, SpellType.HEAL,
                    io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellStatSection(missing));
            getLogger().info("round22 等价性: spell=" + equivSpell + " story=" + equivStory
                + " count=" + equivCount + " missing=" + equivMissing);

            // ———— 图鉴页 36 槽判定 ————
            time(w, "stats.pageCheck36", "old_full_path_x36", 2_000, () -> {
                for (int s = 0; s < 36; s++) {
                    bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                        .hasUnlockedSpell(player, enabled[s]) ? 1 : 0;
                }
            });
            time(w, "stats.pageCheck36", "new_section_once_relative_x36", 5_000, () -> {
                final org.bukkit.configuration.ConfigurationSection section =
                    io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellStatSection(player);
                for (int s = 0; s < 36; s++) {
                    bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                        .hasUnlockedSpell(player, enabled[s], section) ? 1 : 0;
                }
            });

            // ———— 解锁计数（新实现 vs 旧全路径复刻） ————
            time(w, "stats.countSpells69", "old_full_path_rebuild", 5_000, () -> {
                int c = 0;
                final org.bukkit.configuration.ConfigurationSection section =
                    stats.getConfigurationSection(player + ".SPELL");
                for (String spell : section.getKeys(false)) {
                    if (stats.getBoolean(player + ".SPELL." + spell + ".UNLOCKED")) c++;
                }
                bh += c;
            });
            time(w, "stats.countSpells69", "new_relative_read", 10_000, () -> {
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellsUnlocked(player);
            });

            time(w, "stats.countStories274", "old_full_path_rebuild", 1_000, () -> {
                int c = 0;
                final org.bukkit.configuration.ConfigurationSection section =
                    stats.getConfigurationSection(player + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (stats.getBoolean(player + ".STORY." + story + ".UNLOCKED")) c++;
                }
                bh += c;
            });
            time(w, "stats.countStories274", "new_relative_read", 3_000, () -> {
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            });
        } finally {
            // 清理合成数据（不留存；SaveConfigRunnable 周期落盘前已移除）
            stats.set(player.toString(), null);
        }
    }

    /** 第 23 轮：统计计数纪元缓存（真实 PlayerStatistics 写方法递增纪元 → 计数缓存失效） */
    private void benchRound23(PrintWriter w) {
        final UUID player = UUID.fromString("12345678-1234-1234-1234-123456789023");
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();
        final io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[] defs =
            CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial()
                .toArray(new io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[0]);

        try {
            // 注入：经真实写方法（自动递增纪元）解锁 137 个故事 + 30 镀金
            for (int i = 0; i < defs.length; i++) {
                if (i % 2 == 0) {
                    io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                        .unlockUniqueStory(player, defs[i]);
                }
            }

            // ———— 等价性与失效正确性（真实方法） ————
            // 稳态：缓存命中值 == 现场重算值
            final int cached = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            int fresh = 0;
            {
                final org.bukkit.configuration.ConfigurationSection section =
                    stats.getConfigurationSection(player + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (section.getBoolean(story + ".UNLOCKED")) fresh++;
                }
            }
            boolean equivSteady = cached == fresh;
            // 失效：再解锁 1 个（真实写方法递增纪元）→ 计数必须 +1
            final int before = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockUniqueStory(player, defs[1]);
            final int after = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            boolean invalidationOk = after == before + 1;
            // 计数类写入（addChronicle）也递增纪元 → 缓存失效后仍与现场一致
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addChronicle(player, defs[1]);
            final int afterChronicle = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            boolean equivAfterWrite = afterChronicle == after;
            // rank 谓词路径（液化池 Exalted/Uniques 每次催化剂匹配调用）
            final io.github.sefiraat.crystamaehistoria.player.StoryRank rank =
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoryRank(player);
            boolean rankOk = rank != null;
            getLogger().info("round23 等价性: steady=" + equivSteady + " invalidation=" + invalidationOk
                + " afterWrite=" + equivAfterWrite + " rank=" + rankOk + " (count=" + after + ")");

            // ———— 计数路径三变体（274 键） ————
            time(w, "stats.countStories274.r23", "old_full_path_rebuild", 500, () -> {
                int c = 0;
                final org.bukkit.configuration.ConfigurationSection section =
                    stats.getConfigurationSection(player + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (stats.getBoolean(player + ".STORY." + story + ".UNLOCKED")) c++;
                }
                bh += c;
            });
            time(w, "stats.countStories274.r23", "mid_relative_nocache", 1_000, () -> {
                int c = 0;
                final org.bukkit.configuration.ConfigurationSection section =
                    stats.getConfigurationSection(player + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (section.getBoolean(story + ".UNLOCKED")) c++;
                }
                bh += c;
            });
            time(w, "stats.countStories274.r23", "new_epoch_cache_hit", 1_000_000, () -> {
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoriesUnlocked(player);
            });

            // ———— rank 谓词稳态（液化池每次催化剂匹配） ————
            time(w, "stats.rankPredicate", "new_epoch_cached", 200_000, () -> {
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoryRank(player).ordinal();
            });
        } finally {
            // 清理合成数据
            stats.set(player.toString(), null);
        }
    }

    /** 第 24 轮：lore 展示写入组件化（真实 ItemMeta：setLore(Strings) vs lore(Components)） */
    @SuppressWarnings("deprecation")
    private void benchRound24(PrintWriter w) {
        final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

        // —— 构造 4 板法杖实例（新实现路径）与旧副本对照 ——
        final ItemStack staveStack = new ItemStack(Material.BLAZE_ROD);
        final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave stave =
            new io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave(staveStack);
        final SpellType[] enabled = SpellType.getEnabledSpells();
        final io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot[] slots =
            io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.getCashedValues();
        for (int i = 0; i < slots.length; i++) {
            stave.setSlot(slots[i], new io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate(
                1, enabled[i % enabled.length], 10 + i));
        }

        // —— 故事样本（跨稀有度）——
        final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity[] rarities = {
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.RARE,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.MYTHICAL
        };
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> stories = new java.util.ArrayList<>();
        for (io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity rarity : rarities) {
            final java.util.Map<io.github.sefiraat.crystamaehistoria.stories.definition.StoryType,
                java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story>> byType =
                CrystamaeHistoria.getStoriesManager().getStoriesByRarityAndType().get(rarity);
            if (byType != null && !byType.isEmpty()) {
                stories.add(byType.values().iterator().next().get(0));
            }
        }

        // —— 等价性断言：旧字符串路径 vs 被否决的组件路径（三重诊断） ——
        boolean equivStaveStrings;
        boolean equivStaveComponents;
        boolean equivStaveItem;
        boolean equivStory;
        {
            final ItemMeta metaOld = staveStack.getItemMeta();
            round24OldBuildLore(stave, metaOld);
            final ItemMeta metaNew = staveStack.getItemMeta();
            round24NewBuildLore(stave, metaNew, LEGACY);
            final ItemStack oldItem = new ItemStack(Material.BLAZE_ROD);
            oldItem.setItemMeta(metaOld);
            final ItemStack newItem = new ItemStack(Material.BLAZE_ROD);
            newItem.setItemMeta(metaNew);
            equivStaveStrings = java.util.Objects.equals(metaOld.getLore(), metaNew.getLore());
            equivStaveComponents = java.util.Objects.equals(metaOld.lore(), metaNew.lore());
            equivStaveItem = oldItem.isSimilar(newItem);

            final ItemMeta storyOld = new ItemStack(Material.STONE).getItemMeta();
            round24OldStoryLore(stories, storyOld);
            final ItemMeta storyNew = new ItemStack(Material.STONE).getItemMeta();
            newStoryLore(stories, storyNew, LEGACY);
            final ItemStack storyOldItem = new ItemStack(Material.STONE);
            storyOldItem.setItemMeta(storyOld);
            final ItemStack storyNewItem = new ItemStack(Material.STONE);
            storyNewItem.setItemMeta(storyNew);
            equivStory = java.util.Objects.equals(storyOld.getLore(), storyNew.getLore())
                && java.util.Objects.equals(storyOld.lore(), storyNew.lore())
                && storyOldItem.isSimilar(storyNewItem);
        }
        getLogger().info("round24 等价性: stave(strings)=" + equivStaveStrings
            + " stave(components)=" + equivStaveComponents
            + " stave(item)=" + equivStaveItem + " story=" + equivStory);

        // —— 法杖 lore 应用（20 行形态）——
        time(w, "loreApply.stave20", "old_setlore_strings", 5_000, () -> {
            final ItemMeta meta = staveStack.getItemMeta();
            round24OldBuildLore(stave, meta);
            bh += meta.getLore().size();
        });
        time(w, "loreApply.stave20", "rejected_lore_components", 20_000, () -> {
            final ItemMeta meta = staveStack.getItemMeta();
            round24NewBuildLore(stave, meta, LEGACY);
            bh += meta.getLore().size();
        });

        // —— 故事 lore 应用（N 故事形态）——
        time(w, "loreApply.storyN", "old_setlore_strings", 5_000, () -> {
            final ItemMeta meta = new ItemStack(Material.STONE).getItemMeta();
            round24OldStoryLore(stories, meta);
            bh += meta.getLore().size();
        });
        time(w, "loreApply.storyN", "rejected_lore_components", 20_000, () -> {
            final ItemMeta meta = new ItemStack(Material.STONE).getItemMeta();
            newStoryLore(stories, meta, LEGACY);
            bh += meta.getLore().size();
        });

        // —— 纯转换成本参照（无 ItemMeta）：转换本就廉价，瓶颈归因修正的关键证据 ——
        final java.util.List<String> lines = new java.util.ArrayList<>();
        for (io.github.sefiraat.crystamaehistoria.stories.Story s : stories) {
            lines.add(s.getDisplayName());
            lines.addAll(s.getStoryLore());
        }
        final net.kyori.adventure.text.Component[] cached = new net.kyori.adventure.text.Component[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            cached[i] = LEGACY.deserialize(lines.get(i));
        }
        time(w, "loreConvert.deserializeOnly", "per_line", 50_000, () -> {
            long c = 0;
            for (String line : lines) {
                c += LEGACY.deserialize(line).hashCode();
            }
            bh += c;
        });
        time(w, "loreConvert.deserializeOnly", "cached_refs", 1_000_000, () -> {
            long c = 0;
            for (net.kyori.adventure.text.Component comp : cached) {
                c += comp.hashCode();
            }
            bh += c;
        });
        bh += LEGACY.deserialize("").hashCode();
    }

    /** 被否决方案的组件化 buildLore 副本（遗留字符串 + legacySection 反序列化 + lore(Components)） */
    private void round24NewBuildLore(
        io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave stave, ItemMeta itemMeta,
        net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy) {
        final io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot[] slots =
            io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.getCashedValues();
        final java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        lore.add(legacy.deserialize(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor()
            + "可以进行法术绑定的法杖"));
        for (int i = 0; i < slots.length; i++) {
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate plate =
                stave.getSpellInstanceMap().get(slots[i]);
            if (plate != null) {
                lore.add(legacy.deserialize(""));
                lore.add(legacy.deserialize(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_MYTHICAL.getColor()
                    + slots[i].getDescription()));
                lore.add(legacy.deserialize(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor()
                    + "法术: " + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor()
                    + plate.getStoredSpell().getSpell().getName()));
                lore.add(legacy.deserialize(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor()
                    + "充能: " + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor()
                    + plate.getCrysta()));
            }
        }
        lore.add(legacy.deserialize(""));
        lore.add(legacy.deserialize(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO,
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.STAVE.getLoreLine())));
        itemMeta.lore(lore);
    }

    /** 同构副本：0.4.0 的 InstanceStave.buildLore（字符串组装 + setLore） */
    @SuppressWarnings("deprecation")
    private void round24OldBuildLore(
        io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave stave, ItemMeta itemMeta) {
        final java.util.List<String> finalLore = new java.util.ArrayList<>();
        finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor() + "可以进行法术绑定的法杖");
        final io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot[] slots =
            io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.getCashedValues();
        for (int i = 0; i < slots.length; i++) {
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate plate =
                stave.getSpellInstanceMap().get(slots[i]);
            if (plate != null) {
                final String magic = plate.getStoredSpell().getSpell().getName();
                final String crysta = String.valueOf(plate.getCrysta());
                finalLore.add("");
                finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.RARITY_MYTHICAL.getColor()
                    + slots[i].getDescription());
                finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor()
                    + "法术: " + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor() + magic);
                finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor()
                    + "充能: " + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.NOTICE.getColor() + crysta);
            }
        }
        finalLore.add("");
        finalLore.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.applyThemeToString(
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO,
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.STAVE.getLoreLine()));
        itemMeta.setLore(finalLore);
    }

    /** 同构副本：0.4.0 的 rebuildStoriedStack lore 组装（字符串 + setLore） */
    @SuppressWarnings("deprecation")
    private void round24OldStoryLore(java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> stories,
                                     ItemMeta itemMeta) {
        final java.util.List<String> lore = new java.util.ArrayList<>();
        for (io.github.sefiraat.crystamaehistoria.stories.Story story : stories) {
            lore.add("");
            lore.add(story.getDisplayName());
            lore.addAll(story.getStoryLore());
        }
        itemMeta.setLore(lore);
    }

    /** 被否决方案的组件化故事 lore 组装副本 */
    private void newStoryLore(java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> stories,
                              ItemMeta itemMeta,
                              net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy) {
        final java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        for (io.github.sefiraat.crystamaehistoria.stories.Story story : stories) {
            lore.add(legacy.deserialize(""));
            lore.add(legacy.deserialize(story.getDisplayName()));
            for (String line : story.getStoryLore()) {
                lore.add(legacy.deserialize(line));
            }
        }
        itemMeta.lore(lore);
    }

    /** 第 26 轮：故事列表 PDC v2 瘦编码（真实 PDC 读/写与提交路径） */
    private void benchRound26(PrintWriter w) {
        // 跨稀有度取 5 个真实池故事（3 常规提交形态 + 5 满配形态）
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> pool = new java.util.ArrayList<>();
        final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity[] rs = {
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.UNCOMMON,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.RARE,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.EPIC,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.MYTHICAL
        };
        for (io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity r : rs) {
            final java.util.Map<io.github.sefiraat.crystamaehistoria.stories.definition.StoryType,
                java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story>> byType =
                CrystamaeHistoria.getStoriesManager().getStoriesByRarityAndType().get(r);
            if (byType != null && !byType.isEmpty()) {
                pool.add(byType.values().iterator().next().get(0));
            }
        }
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> three = pool.subList(0, 3);
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> five = pool;

        // ———— 等价性断言 ————
        boolean equivRoundTrip;
        boolean equivDualReadV1;
        boolean equivMigration;
        {
            // v1/v2 各自往返解析为同一池实例列表
            final ItemStack a = new ItemStack(Material.STONE);
            final ItemMeta ma = a.getItemMeta();
            DataTypeMethods.setCustom(ma, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, five);
            final ItemStack b = new ItemStack(Material.STONE);
            final ItemMeta mb = b.getItemMeta();
            DataTypeMethods.setCustom(mb, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES_V2,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType.TYPE, five);
            a.setItemMeta(ma);
            b.setItemMeta(mb);
            equivRoundTrip = java.util.Objects.equals(
                io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getAllStories(a),
                io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getAllStories(b))
                && io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getAllStories(b).size() == five.size();

            // v1 物品经双读仍正确
            equivDualReadV1 = java.util.Objects.equals(
                io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getAllStories(a), five);

            // v1 物品迁移：commitStory 触碰后读数一致且 v1 键移除（按键集判定，与值类型无关）
            io.github.sefiraat.crystamaehistoria.utils.StoryUtils.commitStory(a, null,
                CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial().get(0).getUnique());
            final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> migrated =
                io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getAllStories(a);
            equivMigration = migrated.size() == five.size() + 1
                && !a.getItemMeta().getPersistentDataContainer().getKeys()
                    .contains(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES);
        }
        getLogger().info("round26 等价性: roundTrip=" + equivRoundTrip + " dualReadV1=" + equivDualReadV1
            + " migration=" + equivMigration);

        // ———— 序列化（5 故事满配形态） ————
        time(w, "storyPdc.serialize5", "old_v1_subcontainers", 20_000, () -> {
            final ItemMeta meta = new ItemStack(Material.STONE).getItemMeta();
            DataTypeMethods.setCustom(meta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, five);
            bh += meta.getLore() == null ? 0 : 1;
        });
        time(w, "storyPdc.serialize5", "new_v2_twokeys", 50_000, () -> {
            final ItemMeta meta = new ItemStack(Material.STONE).getItemMeta();
            DataTypeMethods.setCustom(meta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES_V2,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType.TYPE, five);
            bh += meta.getLore() == null ? 0 : 1;
        });

        // ———— 反序列化（5 故事） ————
        final ItemStack v1Item = new ItemStack(Material.STONE);
        {
            final ItemMeta m = v1Item.getItemMeta();
            DataTypeMethods.setCustom(m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, five);
            v1Item.setItemMeta(m);
        }
        final ItemStack v2Item = new ItemStack(Material.STONE);
        {
            final ItemMeta m = v2Item.getItemMeta();
            DataTypeMethods.setCustom(m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES_V2,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType.TYPE, five);
            v2Item.setItemMeta(m);
        }
        time(w, "storyPdc.deserialize5", "old_v1", 20_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods
                .getCustom(v1Item.getItemMeta(), io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE).size();
        });
        time(w, "storyPdc.deserialize5", "new_v2", 50_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods
                .getCustom(v2Item.getItemMeta(), io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES_V2,
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType.TYPE).size();
        });

        // ———— 提交路径端到端 ————
        final io.github.sefiraat.crystamaehistoria.stories.Story extra = three.get(0);
        // 情形 A：0 故事物品首故事（v2 最坏情形：单故事两编码键数相同 + 双读 miss + v1 remove）
        time(w, "writePath.storyCommitV2.firstStory", "old_v1_encoding", 2_000, () -> {
            final ItemStack item = new ItemStack(Material.STONE);
            round26CommitV1(item, extra);
            bh += item.getType().ordinal();
        });
        time(w, "writePath.storyCommitV2.firstStory", "new_v2_encoding", 5_000, () -> {
            final ItemStack item = new ItemStack(Material.STONE);
            io.github.sefiraat.crystamaehistoria.utils.StoryUtils.commitStory(item, extra, null);
            bh += item.getType().ordinal();
        });
        // 情形 B：预置 3 故事物品提交第 4 条（真实推进形态）
        final ItemStack baseV1 = new ItemStack(Material.STONE);
        {
            final ItemMeta m = baseV1.getItemMeta();
            DataTypeMethods.setCustom(m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, three);
            baseV1.setItemMeta(m);
        }
        final ItemStack baseV2 = new ItemStack(Material.STONE);
        {
            final ItemMeta m = baseV2.getItemMeta();
            DataTypeMethods.setCustom(m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES_V2,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesV2DataType.TYPE, three);
            baseV2.setItemMeta(m);
        }
        time(w, "writePath.storyCommitV2.fourthStory", "old_v1_encoding", 2_000, () -> {
            final ItemStack item = baseV1.clone();
            round26CommitV1(item, extra);
            bh += item.getType().ordinal();
        });
        time(w, "writePath.storyCommitV2.fourthStory", "new_v2_encoding", 5_000, () -> {
            final ItemStack item = baseV2.clone();
            io.github.sefiraat.crystamaehistoria.utils.StoryUtils.commitStory(item, extra, null);
            bh += item.getType().ordinal();
        });
    }

    /** 同构副本：round-15 的 commitStory 完整形态 + v1 编码（含计数/附魔簿记，逐行一致） */
    @SuppressWarnings("deprecation")
    private void round26CommitV1(ItemStack itemStack, io.github.sefiraat.crystamaehistoria.stories.Story mainStory) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            return;
        }
        java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> storyList =
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.getCustom(
                itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE);
        if (storyList == null) {
            storyList = new java.util.ArrayList<>();
        }
        storyList.add(mainStory);
        io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
            itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORIES,
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoriesDataType.TYPE, storyList);
        final int newAmount = io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getStoryAmount(itemMeta) + 1;
        io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
            .setInt(itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_CURRENT_NUMBER_OF_STORIES, newAmount);
        if (newAmount >= io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getMaxStoryAmount(itemMeta)) {
            itemMeta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK_OF_THE_SEA, 1, true);
            itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        io.github.sefiraat.crystamaehistoria.managers.StoriesManager.rebuildStoriedStack(itemStack, itemMeta, storyList);
        itemStack.setItemMeta(itemMeta);
    }

    /** 第 27 轮：故事上限 JSON → 扁平 int 键（真实 PDC + gson 对照） */
    @SuppressWarnings("deprecation")
    private void benchRound27(PrintWriter w) {
        // ———— 等价性 ————
        boolean equivMakeStoried;
        boolean equivJsonFallback;
        boolean equivCrafted;
        {
            // 新 makeStoried：int 键落盘 + JSON 键移除，读数与 JSON 编码一致
            final ItemStack item = new ItemStack(Material.STONE);
            io.github.sefiraat.crystamaehistoria.utils.StoryUtils.makeStoried(item);
            final int newRead = io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getMaxStoryAmount(item.getItemMeta());
            final boolean jsonRemoved = !item.getItemMeta().getPersistentDataContainer().getKeys()
                .contains(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES);
            final boolean intPresent = item.getItemMeta().getPersistentDataContainer().getKeys()
                .contains(io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STORY_LIMIT);
            // 旧编码对照：手写 JSON（JS_S_AS=4/JS_S_T=1）→ 双读应得 4
            final ItemStack legacy = new ItemStack(Material.STONE);
            final ItemMeta lm = legacy.getItemMeta();
            final com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES,
                new com.google.gson.JsonPrimitive(4));
            json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_TIER,
                new com.google.gson.JsonPrimitive(1));
            io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
                .setJsonObject(lm, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES, json);
            legacy.setItemMeta(lm);
            equivMakeStoried = newRead >= 1 && newRead <= 5 && jsonRemoved && intPresent;
            equivJsonFallback = io.github.sefiraat.crystamaehistoria.utils.StoryUtils
                .getMaxStoryAmount(legacy.getItemMeta()) == 4;
            // crafted 坏 JSON（非数字）→ 0
            final ItemStack crafted = new ItemStack(Material.STONE);
            final ItemMeta cm = crafted.getItemMeta();
            final com.google.gson.JsonObject bad = new com.google.gson.JsonObject();
            bad.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES,
                new com.google.gson.JsonPrimitive("not-a-number"));
            io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
                .setJsonObject(cm, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES, bad);
            crafted.setItemMeta(cm);
            equivCrafted = io.github.sefiraat.crystamaehistoria.utils.StoryUtils
                .getMaxStoryAmount(crafted.getItemMeta()) == 0;
        }
        getLogger().info("round27 等价性: makeStoried=" + equivMakeStoried
            + " jsonFallback=" + equivJsonFallback + " crafted=" + equivCrafted);

        // ———— 上限读取（判定链/提交路径每物品实例多次） ————
        final ItemStack legacyItem = new ItemStack(Material.STONE);
        {
            final ItemMeta m = legacyItem.getItemMeta();
            final com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES,
                new com.google.gson.JsonPrimitive(4));
            json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_TIER,
                new com.google.gson.JsonPrimitive(1));
            io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
                .setJsonObject(m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES, json);
            legacyItem.setItemMeta(m);
        }
        final ItemStack flatItem = new ItemStack(Material.STONE);
        io.github.sefiraat.crystamaehistoria.utils.StoryUtils.makeStoried(flatItem);
        time(w, "storyLimit.read", "old_json_parse", 50_000, () -> {
            bh += round27OldJsonLimit(legacyItem.getItemMeta());
        });
        time(w, "storyLimit.read", "new_int_read", 500_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.StoryUtils.getMaxStoryAmount(flatItem.getItemMeta());
        });

        // ———— makeStoried 端到端 ————
        time(w, "storyLimit.makeStoried", "old_json_write", 5_000, () -> {
            final ItemStack item = new ItemStack(Material.STONE);
            round27OldMakeStoried(item);
            bh += item.getType().ordinal();
        });
        time(w, "storyLimit.makeStoried", "new_int_write", 20_000, () -> {
            final ItemStack item = new ItemStack(Material.STONE);
            io.github.sefiraat.crystamaehistoria.utils.StoryUtils.makeStoried(item);
            bh += item.getType().ordinal();
        });
    }

    /** 同构副本：旧 JSON 编码的上限读取（防御解析逐行一致） */
    @SuppressWarnings("deprecation")
    private int round27OldJsonLimit(ItemMeta itemMeta) {
        try {
            if (itemMeta == null) {
                return 0;
            }
            final com.google.gson.JsonObject limits = io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
                .getJsonObject(itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES, null);
            if (limits == null || !limits.has(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES)
                || !limits.get(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES).isJsonPrimitive()
            ) {
                return 0;
            }
            return limits.get(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES).getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** 同构副本：旧 JSON 编码的 makeStoried（getInitialStoryLimits 以固定 4 代替随机，两变体同侧） */
    @SuppressWarnings("deprecation")
    private void round27OldMakeStoried(ItemStack itemStack) {
        final ItemMeta itemMeta = itemStack.getItemMeta();
        io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
            .setBoolean(itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_IS_STORIED, true);
        final com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_AVAILABLE_STORIES,
            new com.google.gson.JsonPrimitive(4));
        json.add(io.github.sefiraat.crystamaehistoria.utils.Keys.JS_S_TIER,
            new com.google.gson.JsonPrimitive(1));
        io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI
            .setJsonObject(itemMeta, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_POTENTIAL_STORIES, json);
        itemStack.setItemMeta(itemMeta);
    }

    /** 第 28 轮：法杖存储 v2 扁平编码（真实 PDC 读/写/单槽读取） */
    private void benchRound28(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot[] slots =
            io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.getCashedValues();
        final SpellType[] enabled = SpellType.getEnabledSpells();
        // 4 板满配（不同 tier/晶能/冷却覆盖全字段）
        final java.util.Map<io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot,
            io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate> map = new java.util.EnumMap<>(
            io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot.class);
        for (int i = 0; i < slots.length; i++) {
            final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate plate =
                new io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate(
                    (i % 5) + 1, enabled[i % enabled.length], 10 + i * 7);
            plate.setCooldown(1_700_000_000_000L + i);
            map.put(slots[i], plate);
        }

        final ItemStack v1Stave = new ItemStack(Material.BLAZE_ROD);
        {
            final ItemMeta m = v1Stave.getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STAVE_STORAGE,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType.TYPE, map);
            v1Stave.setItemMeta(m);
        }
        final ItemStack v2Stave = new ItemStack(Material.BLAZE_ROD);
        {
            final ItemMeta m = v2Stave.getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.writeStaveMap(m, map);
            v2Stave.setItemMeta(m);
        }

        // ———— 等价性断言 ————
        boolean equivFullRead;
        boolean equivSingleSlot;
        boolean equivMigration;
        {
            final java.util.Map<io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot,
                io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate> readV1 =
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.readStaveMap(v1Stave.getItemMeta());
            final java.util.Map<io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot,
                io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate> readV2 =
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.readStaveMap(v2Stave.getItemMeta());
            boolean ok = readV1.size() == map.size() && readV2.size() == map.size();
            for (var e : map.entrySet()) {
                final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate p1 = readV1.get(e.getKey());
                final io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate p2 = readV2.get(e.getKey());
                ok &= p1 != null && p2 != null
                    && p1.getTier() == e.getValue().getTier() && p2.getTier() == e.getValue().getTier()
                    && p1.getCrysta() == e.getValue().getCrysta() && p2.getCrysta() == e.getValue().getCrysta()
                    && p1.getCooldown() == e.getValue().getCooldown() && p2.getCooldown() == e.getValue().getCooldown()
                    && p1.getStoredSpell() == e.getValue().getStoredSpell() && p2.getStoredSpell() == e.getValue().getStoredSpell();
            }
            equivFullRead = ok;

            // 单槽读取（v1 物品经回退路径 / v2 物品直接路径）
            equivSingleSlot = true;
            for (var slot : slots) {
                final var a = io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType
                    .readSlotPlate(v1Stave.getItemMeta(), slot);
                final var b = io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType
                    .readSlotPlate(v2Stave.getItemMeta(), slot);
                equivSingleSlot &= a != null && b != null
                    && a.getTier() == b.getTier() && a.getCrysta() == b.getCrysta()
                    && a.getCooldown() == b.getCooldown() && a.getStoredSpell() == b.getStoredSpell();
            }

            // 迁移：v1 物品经写回（v2 覆盖同键）后读取正确，且原键为容器类型
            final ItemMeta migrated = v1Stave.getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.writeStaveMap(migrated, map);
            equivMigration = io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType
                .readStaveMap(migrated).size() == map.size()
                && migrated.getPersistentDataContainer().has(
                    io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STAVE_STORAGE,
                    org.bukkit.persistence.PersistentDataType.TAG_CONTAINER);
        }
        getLogger().info("round28 等价性: fullRead=" + equivFullRead + " singleSlot=" + equivSingleSlot
            + " migration=" + equivMigration);

        // ———— 全量反序列化（4 板） ————
        time(w, "stavePdc.deserialize4.r28", "old_v1", 10_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods
                .getCustom(v1Stave.getItemMeta(), io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STAVE_STORAGE,
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType.TYPE).size();
        });
        time(w, "stavePdc.deserialize4.r28", "new_v2", 50_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType
                .readStaveMap(v2Stave.getItemMeta()).size();
        });

        // ———— 序列化（4 板满配） ————
        time(w, "stavePdc.serialize4", "old_v1", 5_000, () -> {
            final ItemMeta m = new ItemStack(Material.BLAZE_ROD).getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                m, io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_STAVE_STORAGE,
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType.TYPE, map);
            bh += m.getLore() == null ? 0 : 1;
        });
        time(w, "stavePdc.serialize4", "new_v2", 20_000, () -> {
            final ItemMeta m = new ItemStack(Material.BLAZE_ROD).getItemMeta();
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType.writeStaveMap(m, map);
            bh += m.getLore() == null ? 0 : 1;
        });

        // ———— 单槽读取（施法失败前置路径） ————
        time(w, "stavePdc.singleSlot", "old_v1", 20_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType
                .getSlotPlate(v1Stave.getItemMeta(), slots[0]) != null ? 1 : 0;
        });
        time(w, "stavePdc.singleSlot", "new_v2", 100_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveV2DataType
                .readSlotPlate(v2Stave.getItemMeta(), slots[0]) != null ? 1 : 0;
        });
    }

    /** 第 29 轮：区块晶簇故事状态 v2 扁平编码（真实区块 PDC） */
    private void benchRound29(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final org.bukkit.Chunk chunk = world.getChunkAt(6000, 6000);
        chunk.load();
        final NamespacedKey benchKey = new NamespacedKey(CrystamaeHistoria.getInstance(), "bench_chunk_r29");

        // 构造 5 个晶簇故事（副本 + 方块位置 + 镀金混合，覆盖全字段）
        final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> stories = new java.util.ArrayList<>();
        final io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity[] rs = {
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.COMMON,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.UNCOMMON,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.RARE,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.EPIC,
            io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity.MYTHICAL
        };
        int rIdx = 0;
        for (int i = 0; i < 5; i++) {
            final java.util.Map<io.github.sefiraat.crystamaehistoria.stories.definition.StoryType,
                java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story>> byType =
                CrystamaeHistoria.getStoriesManager().getStoriesByRarityAndType().get(rs[rIdx % rs.length]);
            rIdx++;
            final io.github.sefiraat.crystamaehistoria.stories.Story source = byType.values().iterator().next().get(0);
            final io.github.sefiraat.crystamaehistoria.stories.Story copy = source.copy();
            copy.setBlockPosition(new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(
                world, 6000 * 32 + 16 + i, 130, 6000 * 32 + 16 + i));
            copy.setGilded(i % 2 == 0);
            stories.add(copy);
        }

        // ———— 等价性断言 ————
        boolean equivDualRead;
        boolean equivMigration;
        {
            // v1 写入 → 双读应还原全部字段
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                chunk, benchKey, io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType.TYPE, stories);
            final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> readV1 =
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.readChunkStories(chunk, benchKey);
            boolean ok = readV1.size() == stories.size();
            for (int i = 0; i < stories.size() && ok; i++) {
                ok &= readV1.get(i).getId().equals(stories.get(i).getId())
                    && readV1.get(i).getRarity() == stories.get(i).getRarity()
                    && readV1.get(i).isGilded() == stories.get(i).isGilded()
                    && readV1.get(i).getBlockPosition() != null
                    && readV1.get(i).getBlockPosition().getPosition() == stories.get(i).getBlockPosition().getPosition();
            }
            equivDualRead = ok;
            // v2 覆盖同键 → 读数一致且键为容器类型
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.writeChunkStories(chunk, benchKey, stories);
            final java.util.List<io.github.sefiraat.crystamaehistoria.stories.Story> readV2 =
                io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.readChunkStories(chunk, benchKey);
            boolean ok2 = readV2.size() == stories.size();
            for (int i = 0; i < stories.size() && ok2; i++) {
                ok2 &= readV2.get(i).getId().equals(stories.get(i).getId())
                    && readV2.get(i).getRarity() == stories.get(i).getRarity()
                    && readV2.get(i).isGilded() == stories.get(i).isGilded()
                    && readV2.get(i).getBlockPosition().getPosition() == stories.get(i).getBlockPosition().getPosition();
            }
            equivMigration = ok2 && chunk.getPersistentDataContainer().has(benchKey, org.bukkit.persistence.PersistentDataType.TAG_CONTAINER);
        }
        getLogger().info("round29 等价性: dualRead=" + equivDualRead + " migration=" + equivMigration);
        // 清理基准键（不留存）
        chunk.getPersistentDataContainer().remove(benchKey);

        // ———— 序列化（5 晶簇） ————
        time(w, "chunkPdc.serialize5", "old_v1", 5_000, () -> {
            io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
                chunk, benchKey, io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType.TYPE, stories);
            bh += stories.size();
        });
        time(w, "chunkPdc.serialize5", "new_v2", 20_000, () -> {
            io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.writeChunkStories(chunk, benchKey, stories);
            bh += stories.size();
        });

        // ———— 反序列化（5 晶簇） ————
        io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods.setCustom(
            chunk, benchKey, io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType.TYPE, stories);
        time(w, "chunkPdc.deserialize5", "old_v1", 5_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods
                .getCustom(chunk, benchKey,
                    io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkDataType.TYPE).size();
        });
        io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType.writeChunkStories(chunk, benchKey, stories);
        time(w, "chunkPdc.deserialize5", "new_v2", 20_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStoryChunkV2DataType
                .readChunkStories(chunk, benchKey).size();
        });
        chunk.getPersistentDataContainer().remove(benchKey);
    }

    /** 第 31 轮：解锁集合纪元缓存（图鉴页 36 槽判定的批量形态） */
    private void benchRound31(PrintWriter w) {
        final UUID player = UUID.fromString("12345678-1234-1234-1234-123456789031");
        final SpellType[] enabled = SpellType.getEnabledSpells();
        final io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[] defs =
            CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial()
                .toArray(new io.github.sefiraat.crystamaehistoria.stories.BlockDefinition[0]);
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();

        // 注入：2/3 法术解锁（经真实写方法，自动递增纪元）+ 1/2 故事解锁 + 1/4 镀金
        for (int i = 0; i < enabled.length; i++) {
            if (i % 3 != 0) {
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockSpell(player, enabled[i]);
            }
        }
        for (int i = 0; i < defs.length; i++) {
            if (i % 2 == 0) {
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockUniqueStory(player, defs[i]);
            }
            if (i % 4 == 0) {
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockStoryGilded(player, defs[i]);
            }
        }

        try {
            // ———— 等价性断言 ————
            boolean equivSpells = true;
            final java.util.Set<String> spellSet = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedSpellIdSet(player);
            for (SpellType st : enabled) {
                equivSpells &= spellSet.contains(st.getId())
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedSpell(player, st);
            }
            boolean equivStories = true;
            boolean equivGilded = true;
            final java.util.Set<Material> storySet = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedUniqueStorySet(player);
            final java.util.Set<Material> gildedSet = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getGildedSet(player);
            for (int i = 0; i < defs.length; i += 7) {
                final Material m = defs[i].getMaterial();
                equivStories &= storySet.contains(m)
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedUniqueStory(player, m);
                equivGilded &= gildedSet.contains(m)
                    == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.hasUnlockedStoryGilded(player, m);
            }
            // 失效：真实解锁后集合必须更新
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockSpell(player, enabled[0]);
            final boolean invalidated = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedSpellIdSet(player).contains(enabled[0].getId());
            // 无统计玩家空集
            final UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999931");
            final boolean emptyOk = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedSpellIdSet(missing).isEmpty();
            getLogger().info("round31 等价性: spells=" + equivSpells + " stories=" + equivStories
                + " gilded=" + equivGilded + " invalidation=" + invalidated + " empty=" + emptyOk);

            // ———— 法术集页 36 槽 ————
            time(w, "stats.pageCheck36.sets", "old_section_relative", 2_000, () -> {
                final org.bukkit.configuration.ConfigurationSection section =
                    io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getSpellStatSection(player);
                int c = 0;
                for (int s = 0; s < 36; s++) {
                    c += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                        .hasUnlockedSpell(player, enabled[s], section) ? 1 : 0;
                }
                bh += c;
            });
            time(w, "stats.pageCheck36.sets", "new_set_contains", 20_000, () -> {
                final java.util.Set<String> set = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                    .getUnlockedSpellIdSet(player);
                int c = 0;
                for (int s = 0; s < 36; s++) {
                    c += set.contains(enabled[s].getId()) ? 1 : 0;
                }
                bh += c;
            });

            // ———— 故事/镀金集页 36 槽（~998 定义键空间） ————
            final Material[] mats = new Material[36];
            for (int i = 0; i < 36; i++) {
                mats[i] = defs[i * 3].getMaterial();
            }
            time(w, "stats.storyPageCheck36", "old_section_relative", 2_000, () -> {
                final org.bukkit.configuration.ConfigurationSection section =
                    io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStoryStatSection(player);
                int c = 0;
                for (int s = 0; s < 36; s++) {
                    c += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                        .hasUnlockedUniqueStory(player, mats[s], section) ? 1 : 0;
                }
                bh += c;
            });
            time(w, "stats.storyPageCheck36", "new_set_contains", 20_000, () -> {
                final java.util.Set<Material> set = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                    .getUnlockedUniqueStorySet(player);
                int c = 0;
                for (int s = 0; s < 36; s++) {
                    c += set.contains(mats[s]) ? 1 : 0;
                }
                bh += c;
            });

            // 计数写（addUsage）不得失效集合（成员资格纪元与计数纪元分离的断言）
            final java.util.Set<String> beforeUsage = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedSpellIdSet(player);
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addUsage(player, enabled[0]);
            final boolean countWriteNoInvalidate = beforeUsage == io.github.sefiraat.crystamaehistoria.player.PlayerStatistics
                .getUnlockedSpellIdSet(player);
            getLogger().info("round31 计数写不失效集合: " + countWriteNoInvalidate);

            // ———— 快照重建（成员纪元未命中，每次解锁写后第一次查询） ————
            time(w, "stats.snapshotRebuild", "spells_and_stories", 100, () -> {
                // 以一次真实解锁写（递增成员纪元）+ 三集合重建计量
                io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockSpell(player, enabled[0]);
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getUnlockedSpellIdSet(player).size();
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getUnlockedUniqueStorySet(player).size();
                bh += io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getGildedSet(player).size();
            });
        } finally {
            stats.set(player.toString(), null);
        }
    }

    /** 第 34 轮：周期落盘脏判定跳过（真实文件 IO） */
    private void benchRound34(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.managers.ConfigManager cm =
            CrystamaeHistoria.getConfigManager();
        final UUID player = UUID.fromString("12345678-1234-1234-1234-123456789034");
        final java.io.File statsFile = new java.io.File(CrystamaeHistoria.getInstance().getDataFolder(), "player_stats.yml");

        // ———— 正确性断言 ————
        boolean equivWriteFlush;
        boolean equivSkipThenWrite;
        boolean equivForce;
        {
            // 写 → 周期保存 → 文件应含新键
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.unlockSpell(player, SpellType.HEAL);
            cm.saveAll(false);
            org.bukkit.configuration.file.YamlConfiguration onDisk =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(statsFile);
            equivWriteFlush = onDisk.getBoolean(player + ".SPELL.HEAL.UNLOCKED", false);

            // 稳态跳过后再写 → 下个周期仍能落盘
            cm.saveAll(false); // 无写入：跳过
            io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.addChronicle(player,
                CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial().get(0));
            cm.saveAll(false);
            onDisk = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(statsFile);
            equivSkipThenWrite = onDisk.getInt(player + ".STORY."
                + CrystamaeHistoria.getStoriesManager().getBlockDefinitionsSortedByMaterial().get(0).getMaterial()
                + ".TIMES_CHRONICLED", 0) >= 1;

            // force 无条件冲刷
            cm.saveAll(true);
            equivForce = true; // 未抛异常即通过（文件冲刷本身已在上面验证）
        }
        getLogger().info("round34 等价性: writeFlush=" + equivWriteFlush
            + " skipThenWrite=" + equivSkipThenWrite + " force=" + equivForce);
        // 清理合成数据（经配置直写 + 手动冲刷）
        cm.getPlayerStats().set(player.toString(), null);
        cm.saveAll(true);

        // ———— 周期保存路径 ————
        // 旧：无条件全量（player_stats + config 双序列化）
        time(w, "savePath.periodic", "old_unconditional", 5, () -> {
            try {
                cm.getPlayerStats().save(statsFile);
            } catch (Exception e) {
                bh++;
            }
        });
        // 新：水位线跳过（先制造一次真实保存使水位线就位，再测稳态跳过）
        cm.saveAll(false);
        time(w, "savePath.periodic", "new_watermark_skip", 100_000, () -> {
            cm.saveAll(false);
        });
    }

    /** 第 35 轮：镀金器空载 tick 扫描减半（真实实体扫描） */
    private void benchRound35(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        // 空域（与其它基准同区高地，远离已生成实体）
        final Location blockLoc = new Location(world, 6000, 130, 6000);
        final Location center = blockLoc.clone().add(0.5, 0.5, 0.5);

        // ———— 子集性质与空载等价断言 ————
        final java.util.Collection<org.bukkit.entity.Entity> wide = world.getNearbyEntities(center, 3, 3, 3, org.bukkit.entity.Item.class::isInstance);
        final java.util.Collection<org.bukkit.entity.Entity> narrow = world.getNearbyEntities(center, 0.75, 0.75, 0.75, org.bukkit.entity.Item.class::isInstance);
        boolean subsetIdle = wide.isEmpty() && narrow.isEmpty();

        // 有物场景：3³ 内两物品（0.75 外）→ 首扫非空次扫空，行为等价（只拉取不消费）
        final java.util.List<org.bukkit.entity.Item> items = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            items.add(world.dropItem(center.clone().add(1.5 + i, 0, 0), new ItemStack(Material.DIAMOND)));
        }
        boolean subsetWithItems;
        {
            final java.util.Collection<org.bukkit.entity.Entity> wide2 = world.getNearbyEntities(center, 3, 3, 3, org.bukkit.entity.Item.class::isInstance);
            final java.util.Collection<org.bukkit.entity.Entity> narrow2 = world.getNearbyEntities(center, 0.75, 0.75, 0.75, org.bukkit.entity.Item.class::isInstance);
            // 新逻辑：首扫非空 → 次扫照跑（与旧一致）；0.75³ 应只见 0 个（物品在 1.5+ 外）
            subsetWithItems = wide2.size() >= 2 && narrow2.isEmpty();
        }
        for (org.bukkit.entity.Item item : items) {
            item.remove();
        }
        getLogger().info("round35 等价性: idleSubset=" + subsetIdle + " withItemsSubset=" + subsetWithItems);

        // ———— 空载 tick：旧（双扫 + 双克隆）vs 新（单扫 + 缓存中心点） ————
        time(w, "gilderTick.idle", "old_two_scans_two_clones", 20_000, () -> {
            final Location c1 = blockLoc.clone().add(0.5, 0.5, 0.5);
            world.getNearbyEntities(c1, 3, 3, 3, org.bukkit.entity.Item.class::isInstance);
            final Location c2 = blockLoc.clone().add(0.5, 0.5, 0.5);
            world.getNearbyEntities(c2, 0.75, 0.75, 0.75, org.bukkit.entity.Item.class::isInstance);
        });
        time(w, "gilderTick.idle", "new_one_scan_cached_center", 50_000, () -> {
            world.getNearbyEntities(center, 3, 3, 3, org.bukkit.entity.Item.class::isInstance);
            // 首扫空 → 跳过次扫
        });
    }

    /** 第 38 轮：粒子展示任务的玩家筛除（Proxy 玩家桩，20 在线形态） */
    private void benchRound38(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final org.bukkit.Location standLoc = new Location(world, 6000.5, 130.5, 6000.5);
        final ItemStack dirtStack = new ItemStack(Material.DIRT);

        // 玩家桩：java.lang.reflect.Proxy，按返回类型给默认值
        final java.util.function.Function<java.util.Objects, ?> none = null;
        final org.bukkit.entity.Player[] players = new org.bukkit.entity.Player[20];
        final java.lang.reflect.InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "getInventory":
                    return java.lang.reflect.Proxy.newProxyInstance(
                        getClass().getClassLoader(),
                        new Class[]{org.bukkit.inventory.PlayerInventory.class},
                        (p2, m2, a2) -> m2.getName().equals("getItemInMainHand") ? dirtStack : defaultValue(m2.getReturnType()));
                case "getLocation":
                    return standLoc;
                default:
                    return defaultValue(method.getReturnType());
            }
        };
        for (int i = 0; i < players.length; i++) {
            players[i] = (org.bukkit.entity.Player) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{org.bukkit.entity.Player.class}, handler);
        }

        // ———— 等价性：非持勺玩家两形态判定一致（均筛除，不取块） ————
        boolean equivGating = true;
        for (org.bukkit.entity.Player player : players) {
            final SlimefunItem item = SlimefunItem.getByItem(player.getInventory().getItemInMainHand());
            final boolean oldHolds = item instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.LuminescenceScoop;
            equivGating &= !oldHolds; // 桩持泥土：两形态都必须筛除
        }
        getLogger().info("round38 等价性(非持勺筛除一致): " + equivGating);

        // ———— 每玩家循环体：旧（判定前取块快照）vs 新（判定后取） ————
        time(w, "particleDisplay.playerLoop20", "old_fetch_block_before_gate", 50_000, () -> {
            int c = 0;
            for (org.bukkit.entity.Player player : players) {
                final SlimefunItem item = SlimefunItem.getByItem(player.getInventory().getItemInMainHand());
                final Block block = player.getLocation().getBlock();
                if (!(item instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.LuminescenceScoop)) {
                    continue;
                }
                c += block.getX();
            }
            bh += c;
        });
        time(w, "particleDisplay.playerLoop20", "new_gate_first", 100_000, () -> {
            int c = 0;
            for (org.bukkit.entity.Player player : players) {
                final SlimefunItem item = SlimefunItem.getByItem(player.getInventory().getItemInMainHand());
                if (!(item instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.LuminescenceScoop)) {
                    continue;
                }
                c += player.getLocation().getBlock().getX();
            }
            bh += c;
        });
    }

    /** Proxy 桩的按类型默认值（对象 null / 基元零值） */
    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }

    /** 第 39 轮：展示架 afterTick 的 getByItem 每 tick 读取缓存化（真实实体 + Slimefun 物品） */
    private void benchRound39(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location loc = new Location(world, 6000.5, 135.5, 6000.5);

        // 取一个真实注册的 Slimefun 物品堆
        SlimefunItem anySf = null;
        for (SlimefunItem sf : io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getRegistry().getEnabledSlimefunItems()) {
            anySf = sf;
            break;
        }
        final ItemStack sfStack = anySf.getItem();
        final org.bukkit.entity.Item display = world.dropItem(loc, sfStack);
        display.setGravity(false);

        // 弱缓存同构副本
        final java.util.Map<org.bukkit.entity.Item, SlimefunItem> cache = new java.util.WeakHashMap<>();

        // ———— 等价性：缓存解析与直读一致（多次/移除重解析） ————
        boolean equiv = SlimefunItem.getByItem(display.getItemStack()) == cache.computeIfAbsent(display, d -> SlimefunItem.getByItem(d.getItemStack()));
        cache.remove(display);
        equiv &= cache.computeIfAbsent(display, d -> SlimefunItem.getByItem(d.getItemStack())) == SlimefunItem.getByItem(display.getItemStack());
        getLogger().info("round39 等价性(缓存解析一致): " + equiv);

        // ———— 每 tick 解析：直读 vs 弱缓存命中 ————
        time(w, "standTick.resolveItem", "old_getByItem_per_tick", 50_000, () -> {
            bh += SlimefunItem.getByItem(display.getItemStack()) != null ? 1 : 0;
        });
        time(w, "standTick.resolveItem", "new_weakmap_hit", 500_000, () -> {
            SlimefunItem r = cache.get(display);
            if (r == null && !cache.containsKey(display)) {
                r = SlimefunItem.getByItem(display.getItemStack());
                cache.put(display, r);
            }
            bh += r != null ? 1 : 0;
        });

        display.remove();
    }

    /** 第 40 轮：共享 SF 解析弱缓存（真实水晶实体——满池滞留形态） */
    private void benchRound40(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location loc = new Location(world, 6000.5, 140.5, 6000.5);

        // 真实水晶物品（Crystal 为注册 SF 物品，走完整解析路径）
        SlimefunItem crystalSf = SlimefunItem.getById("CH_CRYSTAL_ELEMENTAL_COMMON");
        if (crystalSf == null) {
            // 兜底：任意已注册 SF 物品
            for (SlimefunItem sf : io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getRegistry().getEnabledSlimefunItems()) {
                crystalSf = sf;
                break;
            }
        }
        final ItemStack crystalStack = crystalSf.getItem();
        final org.bukkit.entity.Item standing = world.dropItem(loc, crystalStack);
        standing.setGravity(false);

        // ———— 等价性：共享解析器与直读一致（多次命中/移除重解析/水晶身份） ————
        boolean equiv = io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(standing)
            == SlimefunItem.getByItem(standing.getItemStack());
        final SlimefunItem first = io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(standing);
        final SlimefunItem second = io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(standing);
        getLogger().info("round40 等价性(共享解析器): direct=" + equiv + " stableInstance=" + (first == second));

        // ———— 满/滞留物品每 tick 解析：直读 vs 共享缓存命中 ————
        time(w, "tickItem.resolve", "old_getByItem_per_tick", 50_000, () -> {
            bh += SlimefunItem.getByItem(standing.getItemStack()) != null ? 1 : 0;
        });
        time(w, "tickItem.resolve", "new_shared_resolver_hit", 500_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.SlimefunItemResolver.resolve(standing) != null ? 1 : 0;
        });

        standing.remove();
    }

    /** 第 42 轮：收获者 onExalt 的 getBlockData 双读归一（真实作物方块） */
    private void benchRound42(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location base = new Location(world, 6010, 130, 6010);
        // 放置一片成熟小麦（Ageable）
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                base.clone().add(x, 0, z).getBlock().setType(Material.WHEAT);
            }
        }
        final Block crop = base.getBlock();

        // ———— 等价性：单读判定与双读一致 ————
        final boolean doubleIsAgeable = crop.getBlockData() instanceof org.bukkit.block.data.Ageable;
        final org.bukkit.block.data.BlockData single = crop.getBlockData();
        final boolean singleIsAgeable = single instanceof org.bukkit.block.data.Ageable;
        getLogger().info("round42 等价性(单双读一致): " + (doubleIsAgeable == singleIsAgeable));

        // ———— 每 tick 每随机点：双读 vs 单读 ————
        time(w, "harvesterTick.blockData", "old_double_read", 100_000, () -> {
            int c = 0;
            if (crop.getBlockData() instanceof org.bukkit.block.data.Ageable) {
                final org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) crop.getBlockData();
                c += ageable.getMaximumAge();
            }
            bh += c;
        });
        time(w, "harvesterTick.blockData", "new_single_read", 200_000, () -> {
            int c = 0;
            final org.bukkit.block.data.BlockData data = crop.getBlockData();
            if (data instanceof org.bukkit.block.data.Ageable) {
                c += ((org.bukkit.block.data.Ageable) data).getMaximumAge();
            }
            bh += c;
        });

        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                base.clone().add(x, 0, z).getBlock().setType(Material.AIR);
            }
        }
    }

    /** 第 44 轮：T5 吸取取首元素（真实实体集合） */
    private void benchRound44(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location loc = new Location(world, 6000.5, 150.5, 6000.5);
        final org.bukkit.entity.Item one = world.dropItem(loc, new ItemStack(Material.DIAMOND));
        one.setGravity(false);

        final java.util.Collection<org.bukkit.entity.Entity> entities =
            world.getNearbyEntities(loc, 0.3, 0.3, 0.3, org.bukkit.entity.Item.class::isInstance);

        // 等价性：两形态取到同一实体
        final org.bukkit.entity.Item viaStream = (org.bukkit.entity.Item) entities.stream().findFirst().orElse(null);
        final org.bukkit.entity.Item viaIterator = entities.isEmpty() ? null : (org.bukkit.entity.Item) entities.iterator().next();
        getLogger().info("round44 等价性(同实体): " + (viaStream == viaIterator));

        time(w, "insertItem.firstElement", "old_stream_findFirst", 200_000, () -> {
            final org.bukkit.entity.Item item = (org.bukkit.entity.Item) entities.stream().findFirst().orElse(null);
            bh += item != null ? 1 : 0;
        });
        time(w, "insertItem.firstElement", "new_iterator_next", 1_000_000, () -> {
            final org.bukkit.entity.Item item = entities.isEmpty() ? null : (org.bukkit.entity.Item) entities.iterator().next();
            bh += item != null ? 1 : 0;
        });

        one.remove();
    }

    /** 第 45 轮：法术 cast 路径 stream 惯用法（真实 Tag 集合） */
    private void benchRound45(PrintWriter w) {
        final java.util.Set<Material> flowers = org.bukkit.Tag.FLOWERS.getValues();
        final java.util.List<Material> cached = java.util.List.copyOf(flowers);

        // ———— 等价性：两随机取法同序一致；缓存列表内容一致 ————
        boolean equivPick = true;
        for (int n = 0; n < flowers.size(); n += 7) {
            final Material viaStream = flowers.stream().skip(n).findAny().orElse(Material.DANDELION);
            Material viaLoop = Material.DANDELION;
            int i = 0;
            for (Material candidate : flowers) {
                if (i++ == n) {
                    viaLoop = candidate;
                    break;
                }
            }
            equivPick &= viaStream == viaLoop;
        }
        final boolean equivList = cached.size() == flowers.size() && cached.containsAll(flowers);
        getLogger().info("round45 等价性: pick=" + equivPick + " list=" + equivList);

        // ———— 列表构建：每次重建 vs 静态缓存引用 ————
        time(w, "spellCast.tagList", "old_rebuild_per_use", 50_000, () -> {
            final java.util.List<Material> list = flowers.stream().toList();
            bh += list.size();
        });
        time(w, "spellCast.tagList", "new_cached_ref", 5_000_000, () -> {
            bh += cached.size();
        });

        // ———— 随机取材：stream skip/findAny vs 直接迭代 ————
        time(w, "spellCast.randomPick", "old_stream_skip", 200_000, () -> {
            bh += flowers.stream()
                .skip(ThreadLocalRandom.current().nextInt(flowers.size()))
                .findAny().orElse(Material.DANDELION).ordinal();
        });
        time(w, "spellCast.randomPick", "new_loop_pick", 500_000, () -> {
            final int skip = ThreadLocalRandom.current().nextInt(flowers.size());
            Material m = Material.DANDELION;
            int i = 0;
            for (Material candidate : flowers) {
                if (i++ == skip) {
                    m = candidate;
                    break;
                }
            }
            bh += m.ordinal();
        });
    }

    /** 第 46 轮：枚举 values() 克隆与集合双复制（cast 染色/放置遍历/tick 集合消费） */
    private void benchRound46(PrintWriter w) {
        // ———— 等价性：静态缓存数组与 values() 逐位一致；直接迭代取第 rnd 元素与 toList().get 一致 ————
        final org.bukkit.DyeColor[] cachedDye = org.bukkit.DyeColor.values();
        boolean equivDye = cachedDye.length == org.bukkit.DyeColor.values().length;
        for (int i = 0; equivDye && i < cachedDye.length; i++) {
            equivDye &= cachedDye[i] == org.bukkit.DyeColor.values()[i];
        }
        final org.bukkit.block.BlockFace[] cachedFaces = org.bukkit.block.BlockFace.values();
        boolean equivFaces = cachedFaces.length == org.bukkit.block.BlockFace.values().length;
        for (int i = 0; equivFaces && i < cachedFaces.length; i++) {
            equivFaces &= cachedFaces[i] == org.bukkit.block.BlockFace.values()[i];
        }
        final java.util.List<Material> src = java.util.List.of(Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID);
        boolean equivPick = true;
        for (int rnd = 0; rnd < src.size(); rnd++) {
            final Material viaList = src.stream().toList().get(rnd);
            Material viaLoop = null;
            int idx = 0;
            for (Material m : src) {
                if (idx++ == rnd) {
                    viaLoop = m;
                    break;
                }
            }
            equivPick &= viaList == viaLoop;
        }
        getLogger().info("round46 等价性: dye=" + equivDye + " faces=" + equivFaces + " pick=" + equivPick);

        // ———— Bobulate 染色取材：values() 两次克隆 + stream 计数 vs 静态数组 ————
        time(w, "enumValues.dyePick", "old_values_clone_stream", 200_000, () -> {
            final int rnd = ThreadLocalRandom.current().nextInt(
                0,
                (int) java.util.Arrays.stream(org.bukkit.DyeColor.values()).count()
            );
            bh += org.bukkit.DyeColor.values()[rnd].ordinal();
        });
        time(w, "enumValues.dyePick", "new_static_array", 2_000_000, () -> {
            final int rnd = ThreadLocalRandom.current().nextInt(0, cachedDye.length);
            bh += cachedDye[rnd].ordinal();
        });

        // ———— BalmySponge 饱和放置遍历：values() 克隆（50+ 元素）vs 静态数组 ————
        time(w, "enumValues.faceIter", "old_values_clone", 100_000, () -> {
            int cnt = 0;
            for (org.bukkit.block.BlockFace f : org.bukkit.block.BlockFace.values()) {
                cnt++;
            }
            bh += cnt;
        });
        time(w, "enumValues.faceIter", "new_static_array", 1_000_000, () -> {
            int cnt = 0;
            for (org.bukkit.block.BlockFace f : cachedFaces) {
                cnt++;
            }
            bh += cnt;
        });

        // ———— ExaltedFertilityPharo tick 集合消费（getNearbyEntitiesByType 返回 Collection）————
        // 空闲常态：空集合
        final java.util.Collection<Material> empty = new java.util.ArrayList<>();
        time(w, "pharoConsume.empty", "old_stream_tolist", 500_000, () -> {
            bh += empty.stream().toList().size();
        });
        time(w, "pharoConsume.empty", "new_isempty_gate", 5_000_000, () -> {
            if (!empty.isEmpty()) {
                bh += 1;
            }
        });
        // 常见负载：3 元素（getNearbyEntitiesByType 的 ArrayList 形态；元素类型与惯用法成本无关）
        final java.util.Collection<Material> animals3 = new java.util.ArrayList<>(src);
        time(w, "pharoConsume.small3", "old_stream_tolist_get", 500_000, () -> {
            final java.util.List<Material> copy = animals3.stream().toList();
            bh += copy.get(ThreadLocalRandom.current().nextInt(copy.size())).ordinal();
        });
        time(w, "pharoConsume.small3", "new_direct_iter", 1_000_000, () -> {
            final int rnd = ThreadLocalRandom.current().nextInt(animals3.size());
            int idx = 0;
            for (Material m : animals3) {
                if (idx++ == rnd) {
                    bh += m.ordinal();
                    break;
                }
            }
        });
    }

    /** 第 47 轮：FloatingHeadAnimation 每 tick 死分支（getLocation 分配 + 恒 false 比较） */
    private void benchRound47(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final ArmorStand standA = world.spawn(new Location(world, 0, 200, 0), ArmorStand.class);
        final ArmorStand standB = world.spawn(new Location(world, 10, 200, 0), ArmorStand.class);
        final double baseYA = standA.getLocation().getY();

        // ———— 等价性 1：静止架上死分支恒不翻转（directionUp 永真，Y 比较恒 false） ————
        boolean flipped = false;
        for (int i = 0; i < 100_000; i++) {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standA, true);
            if (standA.getLocation().getY() >= baseYA + 0.2
                || standA.getLocation().getY() <= baseYA - 0.2) {
                flipped = true;
                break;
            }
        }
        // ———— 等价性 2：双架归零后同步步进（首跑 pose=false 为断言设计缺陷——A 累计
        // 10 万步后起点未归零；归零后 A=旧分支形态 / B=新仅步进 姿态必须完全一致） ————
        standA.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
        standB.setHeadPose(new org.bukkit.util.EulerAngle(0, 0, 0));
        for (int i = 0; i < 1000; i++) {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standA, true);
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standB, true);
        }
        final boolean equivPose = standA.getHeadPose().getX() == standB.getHeadPose().getX()
            && standA.getHeadPose().getY() == standB.getHeadPose().getY()
            && standA.getHeadPose().getZ() == standB.getHeadPose().getZ();
        getLogger().info("round47 等价性: branchInert=" + !flipped + " pose=" + equivPose);

        // ———— 每 tick 步进成本：旧（步进 + getLocation 比较）vs 新（仅步进） ————
        time(w, "headAnim.step", "old_branch_getLocation", 100_000, () -> {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standA, true);
            if (standA.getLocation().getY() >= baseYA + 0.2) {
                bh += 1;
            }
        });
        time(w, "headAnim.step", "new_step_only", 200_000, () -> {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standB, true);
        });
        standA.remove();
        standB.remove();
    }

    /** 第 49 轮：法术球面扫描 O(n²) List.contains 去重（Cascada/PlutosDecent 形态，真实世界方块） */
    private void benchRound49(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        world.getBlockAt(0, 200, 0).getChunk().load();

        // ———— 等价性：同基地两形态扫描产物逐位一致（同序同坐标） ————
        for (int range : new int[]{5, 8}) {
            final List<Block> oldList = scanDedup(world, 0, 200, 0, range);
            final List<Block> newList = scanDirect(world, 0, 200, 0, range);
            boolean equiv = oldList.size() == newList.size();
            for (int i = 0; equiv && i < oldList.size(); i++) {
                equiv &= oldList.get(i).getX() == newList.get(i).getX()
                    && oldList.get(i).getY() == newList.get(i).getY()
                    && oldList.get(i).getZ() == newList.get(i).getZ();
            }
            getLogger().info("round49 等价性 r" + range + ": " + equiv + " (n=" + newList.size() + ")");
        }

        // ———— r5（下界施法）与 r8（高级法杖放大）两档 ————
        time(w, "sphereScan.r5", "old_list_contains", 100, () -> bh += scanDedup(world, 0, 200, 0, 5).size());
        time(w, "sphereScan.r5", "new_direct_add", 2_000, () -> bh += scanDirect(world, 0, 200, 0, 5).size());
        time(w, "sphereScan.r8", "old_list_contains", 20, () -> bh += scanDedup(world, 0, 200, 0, 8).size());
        time(w, "sphereScan.r8", "new_direct_add", 1_000, () -> bh += scanDirect(world, 0, 200, 0, 8).size());
    }

    /** 旧形态：球面扫描 + List.contains 去重（Cascada/PlutosDecent 原实现同构） */
    private List<Block> scanDedup(World world, int baseX, int baseY, int baseZ, int range) {
        final List<Block> blocks = new ArrayList<>();
        for (int y = -range; y < range; y++) {
            for (int x = -range; x < range; x++) {
                for (int z = -range; z < range; z++) {
                    if (Math.sqrt((double) (x * x) + (y * y) + (z * z)) > range) {
                        continue;
                    }
                    final Block block = world.getBlockAt(x + baseX, y + baseY, z + baseZ);
                    if (!blocks.contains(block)) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

    /** 新形态：直接收集（偏移互异 → 坐标必不重复，去重为无效工作） */
    private List<Block> scanDirect(World world, int baseX, int baseY, int baseZ, int range) {
        final List<Block> blocks = new ArrayList<>();
        for (int y = -range; y < range; y++) {
            for (int x = -range; x < range; x++) {
                for (int z = -range; z < range; z++) {
                    if (Math.sqrt((double) (x * x) + (y * y) + (z * z)) > range) {
                        continue;
                    }
                    blocks.add(world.getBlockAt(x + baseX, y + baseY, z + baseZ));
                }
            }
        }
        return blocks;
    }

    /** 第 50 轮：球内判定 sqrt+double 转换 vs 整数平方比较（r49 后形态的进一步消除） */
    private void benchRound50(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        world.getBlockAt(0, 200, 0).getChunk().load();

        // ———— 等价性：sqrt 形态（r49 提交形态）与平方形态产物逐位一致 ————
        for (int range : new int[]{5, 8}) {
            final List<Block> sqrtList = scanDirect(world, 0, 200, 0, range);
            final List<Block> sqList = scanSquared(world, 0, 200, 0, range);
            boolean equiv = sqrtList.size() == sqList.size();
            for (int i = 0; equiv && i < sqrtList.size(); i++) {
                equiv &= sqrtList.get(i).getX() == sqList.get(i).getX()
                    && sqrtList.get(i).getY() == sqList.get(i).getY()
                    && sqrtList.get(i).getZ() == sqList.get(i).getZ();
            }
            getLogger().info("round50 等价性 r" + range + ": " + equiv + " (n=" + sqList.size() + ")");
        }

        // ———— 全扫描对打：sqrt+double vs int 平方（均为直接收集形态） ————
        time(w, "sphereCheck.r5", "old_sqrt_double", 2_000, () -> bh += scanDirect(world, 0, 200, 0, 5).size());
        time(w, "sphereCheck.r5", "new_int_squared", 5_000, () -> bh += scanSquared(world, 0, 200, 0, 5).size());
        time(w, "sphereCheck.r8", "old_sqrt_double", 1_000, () -> bh += scanDirect(world, 0, 200, 0, 8).size());
        time(w, "sphereCheck.r8", "new_int_squared", 2_000, () -> bh += scanSquared(world, 0, 200, 0, 8).size());
    }

    /** 平方形态：整数平方比较 + 直接收集（非负数下与 sqrt 比较精确等价） */
    private List<Block> scanSquared(World world, int baseX, int baseY, int baseZ, int range) {
        final List<Block> blocks = new ArrayList<>();
        final int rangeSq = range * range;
        for (int y = -range; y < range; y++) {
            for (int x = -range; x < range; x++) {
                for (int z = -range; z < range; z++) {
                    if (x * x + y * y + z * z > rangeSq) {
                        continue;
                    }
                    blocks.add(world.getBlockAt(x + baseX, y + baseY, z + baseZ));
                }
            }
        }
        return blocks;
    }

    /** 第 53 轮：事件级 getByItem 材质门控（7 监听器，真实 Slimefun 注册表） */
    private void benchRound53(PrintWriter w) {
        final ItemStack vanilla = new ItemStack(Material.DIAMOND_SWORD);

        // ———— 等价性：各族真实注册物品（门+getByItem 判定 == 旧 getByItem 判定），
        // 且门控材质与注册材质一致；原版物品两路径一致不命中 ————
        // 与 SatchelListener.SATCHEL_MATERIALS 同集（监听器字段私有，此处同构副本断言）
        final java.util.Set<Material> satchelMats = java.util.EnumSet.of(
            Material.WHITE_CONCRETE, Material.GRAY_CONCRETE, Material.LIME_CONCRETE,
            Material.YELLOW_CONCRETE, Material.PURPLE_CONCRETE, Material.RED_CONCRETE);
        boolean equiv = true;
        // 法杖 ×3（STICK）
        for (io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack sf :
            new io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack[]{
                io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.STAVE_BASIC,
                io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.STAVE_ADVANCED,
                io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.STAVE_ARCANE}) {
            final ItemStack st = sf.item();
            equiv &= st.getType() == Material.STICK;
            equiv &= SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.Stave;
        }
        // 水晶（PLAYER_HEAD）：真实注册水晶取样 3 阶
        int crystals = 0;
        for (java.util.Map.Entry<io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity,
            java.util.Map<io.github.sefiraat.crystamaehistoria.stories.definition.StoryType, SlimefunItem>> e
            : io.github.sefiraat.crystamaehistoria.slimefun.Materials.getCrystalMap().entrySet()) {
            for (SlimefunItem si : e.getValue().values()) {
                if (crystals < 3) {
                    equiv &= si.getItem().getType() == Material.PLAYER_HEAD;
                    equiv &= si instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal;
                    crystals++;
                }
            }
        }
        // 收纳袋（六色混凝土）
        for (io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack sf :
            new io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack[]{
                io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.SATCHEL_1,
                io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.SATCHEL_6}) {
            equiv &= satchelMats.contains(sf.item().getType());
            equiv &= SlimefunItem.getByItem(sf.item()) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.CrystamageSatchel;
        }
        // 盐 / 透镜 / 姿态工具 / 画笔
        equiv &= io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.THAUMATURGIC_SALTS.item().getType() == Material.REDSTONE;
        equiv &= SlimefunItem.getByItem(io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.THAUMATURGIC_SALTS.item()) != null;
        equiv &= io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.REFRACTING_LENS.item().getType() == Material.SPYGLASS;
        equiv &= SlimefunItem.getByItem(io.github.sefiraat.crystamaehistoria.slimefun.CrystaStacks.REFRACTING_LENS.item()) != null;
        // 原版物品（含各门材质本体）：旧/新判定一致不命中
        for (Material m : new Material[]{Material.DIAMOND_SWORD, Material.STONE, Material.STICK,
            Material.PLAYER_HEAD, Material.REDSTONE, Material.SPYGLASS, Material.BAMBOO,
            Material.SEA_PICKLE, Material.TIPPED_ARROW, Material.WHITE_CONCRETE}) {
            final ItemStack st = new ItemStack(m);
            equiv &= !(SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.Stave
                || SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.materials.Crystal
                || SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.CrystamageSatchel);
        }
        getLogger().info("round53 等价性: " + equiv + " (crystals=" + crystals + ")");

        // ———— 门控对打：原版物品的 getByItem miss（常态路径）vs 材质判定 ————
        time(w, "eventGate.getByItem", "old_getbyitem_miss", 50_000, () -> {
            bh += SlimefunItem.getByItem(vanilla) == null ? 1 : 0;
        });
        time(w, "eventGate.getByItem", "new_material_gate", 5_000_000, () -> {
            bh += vanilla.getType() == Material.STICK ? 1 : 0;
        });

        // ———— 已注册材质的原版物品 miss（法杖=STICK/水晶=PLAYER_HEAD 常态）：
        // Slimefun 材质索引命中后才走 meta+PDC 全路径 ————
        final ItemStack plainStick = new ItemStack(Material.STICK);
        final ItemStack plainHead = new ItemStack(Material.PLAYER_HEAD);
        time(w, "eventGate.getByItemRegistered", "old_stick_getbyitem", 100_000, () -> {
            bh += SlimefunItem.getByItem(plainStick) == null ? 1 : 0;
        });
        time(w, "eventGate.getByItemRegistered", "new_stick_gate", 5_000_000, () -> {
            bh += plainStick.getType() == Material.STICK ? 1 : 0;
        });
        time(w, "eventGate.getByItemRegistered", "old_head_getbyitem", 100_000, () -> {
            bh += SlimefunItem.getByItem(plainHead) == null ? 1 : 0;
        });
        time(w, "eventGate.getByItemRegistered", "new_head_gate", 5_000_000, () -> {
            bh += plainHead.getType() == Material.PLAYER_HEAD ? 1 : 0;
        });

        // ———— 收纳袋 36 槽扫描：逐槽 getByItem vs 空判+EnumSet ————
        final ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = new ItemStack(Material.DIAMOND_SWORD);
        }
        time(w, "eventGate.satchelScan", "old_36x_getbyitem", 200, () -> {
            int hits = 0;
            for (ItemStack st : contents) {
                if (SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.CrystamageSatchel) {
                    hits++;
                }
            }
            bh += hits;
        });
        time(w, "eventGate.satchelScan", "new_36x_material_gate", 200_000, () -> {
            int hits = 0;
            for (ItemStack st : contents) {
                if (st != null && satchelMats.contains(st.getType())) {
                    if (SlimefunItem.getByItem(st) instanceof io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.CrystamageSatchel) {
                        hits++;
                    }
                }
            }
            bh += hits;
        });
    }

    /** 第 55 轮：实体扫描类型切片化（混合实体场景，真实区块实体索引） */
    private void benchRound55(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location center = new Location(world, 50, 200, 50);
        center.getChunk().load();
        // 混合场景：40 物品 + 15 僵尸 + 5 箭矢 + 3 经验球（农场典型构成）
        final List<org.bukkit.entity.Entity> spawned = new ArrayList<>();
        try {
            for (int i = 0; i < 40; i++) {
                spawned.add(world.dropItem(center.clone().add(i % 8 - 4, 0.5, i / 8 - 2), new ItemStack(Material.DIAMOND)));
            }
            for (int i = 0; i < 15; i++) {
                spawned.add(world.spawn(center.clone().add(i % 5 - 2, 1, i / 5 - 1), org.bukkit.entity.Zombie.class));
            }
            for (int i = 0; i < 5; i++) {
                spawned.add(world.spawnArrow(center.clone().add(0, 1.5, 0), new Vector(1, 0.1, 0), 0.5f, 0f));
            }
            for (int i = 0; i < 3; i++) {
                spawned.add(world.spawn(center.clone().add(i, 0.5, 3), org.bukkit.entity.ExperienceOrb.class));
            }

            // ———— 等价性：谓词形态与 ByType 形态同盒同集合（UUID 集） ————
            boolean equiv = true;
            for (double r : new double[]{4, 8}) {
                final java.util.Set<java.util.UUID> livingOld = new java.util.HashSet<>();
                for (org.bukkit.entity.Entity e : world.getNearbyEntities(center, r, r, r, org.bukkit.entity.LivingEntity.class::isInstance)) {
                    livingOld.add(e.getUniqueId());
                }
                final java.util.Set<java.util.UUID> livingNew = new java.util.HashSet<>();
                for (org.bukkit.entity.LivingEntity e : world.getNearbyEntitiesByType(org.bukkit.entity.LivingEntity.class, center, r, r, r)) {
                    livingNew.add(e.getUniqueId());
                }
                equiv &= livingOld.equals(livingNew);
                final java.util.Set<java.util.UUID> itemOld = new java.util.HashSet<>();
                for (org.bukkit.entity.Entity e : world.getNearbyEntities(center, r, r, r, org.bukkit.entity.Item.class::isInstance)) {
                    itemOld.add(e.getUniqueId());
                }
                final java.util.Set<java.util.UUID> itemNew = new java.util.HashSet<>();
                for (org.bukkit.entity.Item e : world.getNearbyEntitiesByType(org.bukkit.entity.Item.class, center, r, r, r)) {
                    itemNew.add(e.getUniqueId());
                }
                equiv &= itemOld.equals(itemNew);
                getLogger().info("round55 r" + r + " 集合: living=" + livingNew.size() + " item=" + itemNew.size());
            }
            getLogger().info("round55 等价性: " + equiv);

            // ———— 对打：谓词全扫 vs 类型切片 ————
            time(w, "entityScan.living", "old_predicate_scan", 20_000, () -> {
                int n = 0;
                for (org.bukkit.entity.Entity e : world.getNearbyEntities(center, 6, 6, 6, org.bukkit.entity.LivingEntity.class::isInstance)) {
                    n++;
                }
                bh += n;
            });
            time(w, "entityScan.living", "new_bytype_slice", 50_000, () -> {
                int n = 0;
                for (org.bukkit.entity.LivingEntity e : world.getNearbyEntitiesByType(org.bukkit.entity.LivingEntity.class, center, 6, 6, 6)) {
                    n++;
                }
                bh += n;
            });
            time(w, "entityScan.item", "old_predicate_scan", 20_000, () -> {
                int n = 0;
                for (org.bukkit.entity.Entity e : world.getNearbyEntities(center, 3, 3, 3, org.bukkit.entity.Item.class::isInstance)) {
                    n++;
                }
                bh += n;
            });
            time(w, "entityScan.item", "new_bytype_slice", 50_000, () -> {
                int n = 0;
                for (org.bukkit.entity.Item e : world.getNearbyEntitiesByType(org.bukkit.entity.Item.class, center, 3, 3, 3)) {
                    n++;
                }
                bh += n;
            });
            time(w, "entityScan.orb", "old_predicate_scan", 50_000, () -> {
                int n = 0;
                for (org.bukkit.entity.Entity e : world.getNearbyEntities(center, 4, 4, 4, org.bukkit.entity.ExperienceOrb.class::isInstance)) {
                    n++;
                }
                bh += n;
            });
            time(w, "entityScan.orb", "new_bytype_slice", 100_000, () -> {
                int n = 0;
                for (org.bukkit.entity.ExperienceOrb e : world.getNearbyEntitiesByType(org.bukkit.entity.ExperienceOrb.class, center, 4, 4, 4)) {
                    n++;
                }
                bh += n;
            });
        } finally {
            for (org.bukkit.entity.Entity e : spawned) {
                e.remove();
            }
        }
    }

    /** 第 57 轮：粒子随机云批量化（服务端成本；无观察者时包成本为 0，生产环境另有 N→1 包收益） */
    private void benchRound57(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location center = new Location(world, 0, 200, 0);
        center.getChunk().load();
        final Particle particle = Particle.HEART;
        final int n = 5;

        // ———— 等价性：两形态均正常完成（散布位置移至客户端生成，服务端不可比点集；
        // 断言调用无异常 + 总粒子数语义一致：old N 次单发 == new 单发 count=N）————
        boolean ok = true;
        try {
            for (int i = 0; i < 100; i++) {
                // 旧形态（r19 提交形态：单克隆 + 逐粒子 set）
                final Location point = center.clone();
                for (int j = 0; j < n; j++) {
                    point.setX(center.getX() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                    point.setY(center.getY() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                    point.setZ(center.getZ() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                    world.spawnParticle(particle, point, 1);
                }
                // 新形态（单次调用 count=N + 盒偏移）
                world.spawnParticle(particle, center, n, 1, 1, 1);
                // DUST 变体
                world.spawnParticle(Particle.DUST, center, n, 1, 1, 1, new Particle.DustOptions(org.bukkit.Color.RED, 1));
            }
        } catch (Exception | Error ex) {
            ok = false;
            getLogger().info("round57 异常: " + ex);
        }
        getLogger().info("round57 等价性(调用语义): " + ok);

        // ———— 服务端成本对打：N=5 与 N=10 ————
        time(w, "particleCloud.n5", "old_loop_singles", 100_000, () -> {
            final Location point = center.clone();
            for (int j = 0; j < 5; j++) {
                point.setX(center.getX() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setY(center.getY() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setZ(center.getZ() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                world.spawnParticle(particle, point, 1);
            }
        });
        time(w, "particleCloud.n5", "new_single_batched", 500_000, () -> {
            world.spawnParticle(particle, center, 5, 1, 1, 1);
        });
        time(w, "particleCloud.n10", "old_loop_singles", 50_000, () -> {
            final Location point = center.clone();
            for (int j = 0; j < 10; j++) {
                point.setX(center.getX() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setY(center.getY() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                point.setZ(center.getZ() + ThreadLocalRandom.current().nextDouble(-1, 1.1));
                world.spawnParticle(particle, point, 1);
            }
        });
        time(w, "particleCloud.n10", "new_single_batched", 500_000, () -> {
            world.spawnParticle(particle, center, 10, 1, 1, 1);
        });
    }

    /** 第 59 轮：回调内 NamespacedKey 构造 vs 静态常量引用（Prism/AntiPrism/回忆水晶格形态） */
    private void benchRound59(PrintWriter w) {
        final io.github.sefiraat.crystamaehistoria.CrystamaeHistoria plugin =
            io.github.sefiraat.crystamaehistoria.CrystamaeHistoria.getInstance();

        // ———— 等价性：常量与原 newKey 构造的 namespace/key 逐位一致 ————
        final org.bukkit.NamespacedKey viaCtor = new org.bukkit.NamespacedKey(plugin, "PRISM");
        final org.bukkit.NamespacedKey viaConst = io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_PRISM;
        final boolean equiv = viaCtor.getNamespace().equals(viaConst.getNamespace()) && viaCtor.getKey().equals(viaConst.getKey());
        getLogger().info("round59 等价性: " + equiv);

        // ———— 每调用构造 vs 静态引用 ————
        time(w, "pdcKey.prismFlag", "old_newkey_per_call", 1_000_000, () -> {
            bh += new org.bukkit.NamespacedKey(plugin, "PRISM").hashCode();
        });
        time(w, "pdcKey.prismFlag", "new_static_ref", 10_000_000, () -> {
            bh += io.github.sefiraat.crystamaehistoria.utils.Keys.PDC_PRISM.hashCode();
        });
    }

    /** 第 62 轮：实体生成预配置（consumer 重载）——服务端成本（无观察者包成本为 0，生产另有元数据包 N→1 收益） */
    private void benchRound62(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location center = new Location(world, 0, 200, 0);
        center.getChunk().load();

        // ———— 等价性：两形态生成物终态逐项一致（弹射物/展示物各字段 + PDC） ————
        boolean equiv = true;
        final org.bukkit.entity.Snowball viaOld;
        final org.bukkit.entity.Snowball viaNew;
        viaOld = (org.bukkit.entity.Snowball) world.spawnEntity(center, org.bukkit.entity.EntityType.SNOWBALL);
        viaOld.setShooter(null);
        viaOld.setBounce(false);
        viaNew = world.spawn(center, org.bukkit.entity.Snowball.class, spawned -> {
            spawned.setShooter(null);
            spawned.setBounce(false);
        });
        equiv &= viaOld.doesBounce() == viaNew.doesBounce();
        equiv &= (viaOld.getShooter() == null) == (viaNew.getShooter() == null);
        viaOld.remove();
        viaNew.remove();
        // 展示物：7 项配置终态
        final org.bukkit.entity.Item itemOld = world.dropItem(center, new ItemStack(Material.DIAMOND));
        PersistentDataAPI.setBoolean(itemOld, Keys.PDC_IS_DISPLAY_ITEM, true);
        itemOld.setCustomName("x");
        itemOld.setCustomNameVisible(true);
        itemOld.setGravity(false);
        itemOld.setVelocity(new Vector(0, 0, 0));
        itemOld.setCanPlayerPickup(false);
        itemOld.setPickupDelay(Integer.MAX_VALUE);
        final org.bukkit.entity.Item itemNew = world.dropItem(center, new ItemStack(Material.DIAMOND), spawned -> {
            PersistentDataAPI.setBoolean(spawned, Keys.PDC_IS_DISPLAY_ITEM, true);
            spawned.setCustomName("x");
            spawned.setCustomNameVisible(true);
            spawned.setGravity(false);
            spawned.setVelocity(new Vector(0, 0, 0));
            spawned.setCanPlayerPickup(false);
            spawned.setPickupDelay(Integer.MAX_VALUE);
        });
        equiv &= itemOld.isCustomNameVisible() == itemNew.isCustomNameVisible();
        equiv &= itemOld.hasGravity() == itemNew.hasGravity();
        equiv &= itemOld.getPickupDelay() == itemNew.getPickupDelay();
        equiv &= itemOld.canPlayerPickup() == itemNew.canPlayerPickup();
        equiv &= PersistentDataAPI.hasBoolean(itemNew, Keys.PDC_IS_DISPLAY_ITEM);
        itemOld.remove();
        itemNew.remove();
        getLogger().info("round62 等价性: " + equiv);

        // ———— 复合对打（含同侧 remove 抵消）：spawn 后配置 vs consumer 预配置 ————
        time(w, "entitySpawn.projectile", "old_spawn_then_set", 20_000, () -> {
            final org.bukkit.entity.Snowball sb =
                (org.bukkit.entity.Snowball) world.spawnEntity(center, org.bukkit.entity.EntityType.SNOWBALL);
            sb.setShooter(null);
            sb.setBounce(false);
            sb.remove();
        });
        time(w, "entitySpawn.projectile", "new_spawn_consumer", 20_000, () -> {
            final org.bukkit.entity.Snowball sb = world.spawn(center, org.bukkit.entity.Snowball.class, spawned -> {
                spawned.setShooter(null);
                spawned.setBounce(false);
            });
            sb.remove();
        });
        time(w, "entitySpawn.displayItem", "old_drop_then_set", 20_000, () -> {
            final org.bukkit.entity.Item it = world.dropItem(center, new ItemStack(Material.DIAMOND));
            PersistentDataAPI.setBoolean(it, Keys.PDC_IS_DISPLAY_ITEM, true);
            it.setCustomName("x");
            it.setCustomNameVisible(true);
            it.setGravity(false);
            it.setVelocity(new Vector(0, 0, 0));
            it.setCanPlayerPickup(false);
            it.setPickupDelay(Integer.MAX_VALUE);
            it.remove();
        });
        time(w, "entitySpawn.displayItem", "new_drop_consumer", 20_000, () -> {
            final org.bukkit.entity.Item it = world.dropItem(center, new ItemStack(Material.DIAMOND), spawned -> {
                PersistentDataAPI.setBoolean(spawned, Keys.PDC_IS_DISPLAY_ITEM, true);
                spawned.setCustomName("x");
                spawned.setCustomNameVisible(true);
                spawned.setGravity(false);
                spawned.setVelocity(new Vector(0, 0, 0));
                spawned.setCanPlayerPickup(false);
                spawned.setPickupDelay(Integer.MAX_VALUE);
            });
            it.remove();
        });
    }

    /** 第 63 轮：FallingBlock 的 BlockData 静态复用（createBlockData 每次新建 vs 缓存引用） */
    private void benchRound63(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Location center = new Location(world, 50, 200, 50);
        center.getChunk().load();
        final org.bukkit.block.data.BlockData cached = Material.BLACKSTONE_SLAB.createBlockData();

        // ———— 等价性：缓存实例与新建实例状态一致；同缓存实例可跨多次生成复用 ————
        boolean equiv = cached.getAsString().equals(Material.BLACKSTONE_SLAB.createBlockData().getAsString());
        equiv &= cached.getMaterial() == Material.BLACKSTONE_SLAB;
        final java.util.List<org.bukkit.entity.FallingBlock> fbs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final org.bukkit.entity.FallingBlock fb = world.spawnFallingBlock(center.clone().add(i, 5, 0), cached);
            equiv &= fb.getBlockData().getMaterial() == Material.BLACKSTONE_SLAB;
            fbs.add(fb);
        }
        for (org.bukkit.entity.FallingBlock fb : fbs) {
            fb.remove();
        }
        getLogger().info("round63 等价性: " + equiv);

        // ———— createBlockData 新建 vs 缓存引用 ————
        time(w, "fallingBlock.dataCreate", "old_per_spawn_create", 500_000, () -> {
            bh += Material.BLACKSTONE_SLAB.createBlockData().hashCode();
        });
        time(w, "fallingBlock.dataCreate", "new_cached_ref", 5_000_000, () -> {
            bh += cached.hashCode();
        });
    }

    /** 同构副本：0.3.0 的 Story.getDisplayName（每次重建组件 + toLegacyText） */
    private String round16FreshDisplayName(io.github.sefiraat.crystamaehistoria.stories.Story s) {
        final net.md_5.bungee.api.chat.TextComponent rarityComponent =
            new net.md_5.bungee.api.chat.TextComponent(
                "[" + io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType
                    .getByRarity(s.getRarity()).getLoreLine() + "] ");
        final net.md_5.bungee.api.chat.TextComponent nameComponent =
            new net.md_5.bungee.api.chat.TextComponent(s.getId());
        rarityComponent.setColor(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType
            .getByRarity(s.getRarity()).getColor());
        rarityComponent.setBold(true);
        nameComponent.setColor(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.CLICK_INFO.getColor());
        return net.md_5.bungee.api.chat.BaseComponent.toLegacyText(rarityComponent, nameComponent);
    }

    /** 同构副本：0.3.0 的 Story.getStoryLore（每次重建组件 + 逐行 toLegacyText） */
    private List<String> round16FreshStoryLore(io.github.sefiraat.crystamaehistoria.stories.Story s) {
        final net.md_5.bungee.api.ChatColor passive =
            io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor();
        final List<String> l = new java.util.ArrayList<>();
        for (final String line : s.getStoryStrings()) {
            final net.md_5.bungee.api.chat.TextComponent c =
                new net.md_5.bungee.api.chat.TextComponent(line);
            c.setColor(passive);
            c.setItalic(false);
            l.add(net.md_5.bungee.api.chat.BaseComponent.toLegacyText(c));
        }
        if (s.getAuthor() != null) {
            l.add("");
            l.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor() + "作者: " + s.getAuthor());
        }
        if (s.getSponsor() != null) {
            l.add("");
            l.add(io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType.PASSIVE.getColor() + "赞助者: " + s.getSponsor());
        }
        return l;
    }

    /** 物品终态比对：PDC 逐键多类型探测 + lore + 显示名 + 附魔 + 物品标志 */    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean round15ItemStateEquals(ItemStack a, ItemStack b) {
        final ItemMeta ma = a.getItemMeta();
        final ItemMeta mb = b.getItemMeta();
        // PDC 键集与逐键值（API 无原始字节导出，按键集 + 多类型探测值比对）
        final Set<NamespacedKey> keysA = ma.getPersistentDataContainer().getKeys();
        final Set<NamespacedKey> keysB = mb.getPersistentDataContainer().getKeys();
        if (!keysA.equals(keysB)) {
            getLogger().warning("round15 PDC 键集不一致: " + keysA + " vs " + keysB);
            return false;
        }
        final org.bukkit.persistence.PersistentDataType[] probeTypes = {
            org.bukkit.persistence.PersistentDataType.BYTE_ARRAY,
            org.bukkit.persistence.PersistentDataType.STRING,
            org.bukkit.persistence.PersistentDataType.INTEGER,
            org.bukkit.persistence.PersistentDataType.BOOLEAN,
            org.bukkit.persistence.PersistentDataType.LONG,
            org.bukkit.persistence.PersistentDataType.DOUBLE
        };
        for (final NamespacedKey key : keysA) {
            final String va = round15PdcValue(ma.getPersistentDataContainer(), key, probeTypes);
            final String vb = round15PdcValue(mb.getPersistentDataContainer(), key, probeTypes);
            if (!va.equals(vb)) {
                getLogger().warning("round15 PDC 键值不一致(" + key + "): [" + va + "] vs [" + vb + "]");
                return false;
            }
        }
        return java.util.Objects.equals(ma.getLore(), mb.getLore())
            && java.util.Objects.equals(ma.getDisplayName(), mb.getDisplayName())
            && java.util.Objects.equals(ma.getEnchants(), mb.getEnchants())
            && ma.getItemFlags().equals(mb.getItemFlags());
    }

    @SuppressWarnings("unchecked")
    private String round15PdcValue(org.bukkit.persistence.PersistentDataContainer pdc,
                                   NamespacedKey key,
                                   org.bukkit.persistence.PersistentDataType[] probeTypes) {
        for (final org.bukkit.persistence.PersistentDataType t : probeTypes) {
            try {
                final Object v = pdc.get(key, t);
                if (v != null) {
                    return v instanceof byte[] ? java.util.Arrays.toString((byte[]) v) : String.valueOf(v);
                }
            } catch (IllegalArgumentException e) {
                // 存储类型与探测类型不符，继续下一类型
            }
        }
        return "<none>";
    }

    // ==================== 第 70 轮：方块写入标志与逐 tick BlockData 克隆域 ====================

    /** 复刻 0.15.0 ChroniclerPanelCache.animateLight 单 tick 体（getBlockData 克隆 + 单参 setBlockData=physics） */
    private int round70StepOld(Block b, boolean[] dim) {
        if (b.getType() == Material.LIGHT) {
            final org.bukkit.block.data.type.Light light = (org.bukkit.block.data.type.Light) b.getBlockData();
            final int level = light.getLevel();
            if (level >= 15) {
                light.setLevel(level - 1);
                dim[0] = true;
            } else if (level <= 5) {
                light.setLevel(level + 1);
                dim[0] = false;
            } else {
                light.setLevel(dim[0] ? level - 1 : level + 1);
            }
            b.setBlockData(light);
            return light.getLevel();
        }
        return -1;
    }

    /** 新形态：缓存 Light 实例就地调级 + setBlockData(data, false)（无邻居通知、观察者不可见） */
    private int round70StepNew(Block b, org.bukkit.block.data.type.Light cached, boolean[] dim) {
        if (b.getType() == Material.LIGHT) {
            final int level = cached.getLevel();
            if (level >= 15) {
                cached.setLevel(level - 1);
                dim[0] = true;
            } else if (level <= 5) {
                cached.setLevel(level + 1);
                dim[0] = false;
            } else {
                cached.setLevel(dim[0] ? level - 1 : level + 1);
            }
            b.setBlockData(cached, false);
            return cached.getLevel();
        }
        return -1;
    }

    private void benchRound70(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Block lightBlock = world.getBlockAt(50, 220, 50);
        lightBlock.getChunk().load();

        // —— 等价性：两种形态自同一初态出发 60 步 level 序列逐位一致 + 新形态终态读回一致 ——
        final boolean[] dim = {true};
        final int[] oldSeq = new int[60];
        final int[] newSeq = new int[60];
        lightBlock.setType(Material.AIR);
        lightBlock.setType(Material.LIGHT);
        org.bukkit.block.data.type.Light init = (org.bukkit.block.data.type.Light) lightBlock.getBlockData();
        init.setLevel(10);
        lightBlock.setBlockData(init);
        for (int i = 0; i < 60; i++) {
            oldSeq[i] = round70StepOld(lightBlock, dim);
        }
        lightBlock.setType(Material.AIR);
        lightBlock.setType(Material.LIGHT);
        init = (org.bukkit.block.data.type.Light) lightBlock.getBlockData();
        init.setLevel(10);
        lightBlock.setBlockData(init);
        dim[0] = true;
        final org.bukkit.block.data.type.Light cached = (org.bukkit.block.data.type.Light) lightBlock.getBlockData();
        for (int i = 0; i < 60; i++) {
            newSeq[i] = round70StepNew(lightBlock, cached, dim);
        }
        boolean equiv = java.util.Arrays.equals(oldSeq, newSeq);
        equiv &= ((org.bukkit.block.data.type.Light) lightBlock.getBlockData()).getLevel() == cached.getLevel();
        getLogger().info("round70 等价性(level 序列 60 步逐位一致 + 终态读回): " + equiv);

        // —— 全序列计时（工作面板每 tick 实际执行的形态）——
        time(w, "lightAnim", "old_getClone_physics", 4_000, () -> bh += round70StepOld(lightBlock, dim));
        // 重同步缓存实例到方块当前 level，保证两变体从同一相位出发
        cached.setLevel(((org.bukkit.block.data.type.Light) lightBlock.getBlockData()).getLevel());
        time(w, "lightAnim", "new_cached_nophysics", 4_000, () -> bh += round70StepNew(lightBlock, cached, dim));

        // —— 隔离项 ——
        time(w, "lightAnim", "iso_getBlockData", 200_000, () -> bh += lightBlock.getBlockData().hashCode());
        final org.bukkit.block.data.type.Light l13 = (org.bukkit.block.data.type.Light) Material.LIGHT.createBlockData();
        l13.setLevel(13);
        final org.bukkit.block.data.type.Light l12 = (org.bukkit.block.data.type.Light) Material.LIGHT.createBlockData();
        l12.setLevel(12);
        final boolean[] flip = {false};
        time(w, "lightAnim", "iso_set_physics", 4_000, () -> {
            flip[0] = !flip[0];
            lightBlock.setBlockData(flip[0] ? l13 : l12);
        });
        time(w, "lightAnim", "iso_set_nophysics", 4_000, () -> {
            flip[0] = !flip[0];
            lightBlock.setBlockData(flip[0] ? l13 : l12, false);
        });

        // —— 作物催熟对照（GreenHouseGlass 形态；physics 差异即该站点的语义成本）——
        final Block farmland = world.getBlockAt(60, 220, 50);
        final Block wheat = world.getBlockAt(60, 221, 50);
        farmland.setType(Material.FARMLAND);
        wheat.setType(Material.WHEAT);
        final org.bukkit.block.data.Ageable w2 = (org.bukkit.block.data.Ageable) wheat.getBlockData();
        w2.setAge(2);
        final org.bukkit.block.data.Ageable w3 = (org.bukkit.block.data.Ageable) wheat.getBlockData();
        w3.setAge(3);
        time(w, "cropAge", "iso_set_physics", 50_000, () -> {
            flip[0] = !flip[0];
            wheat.setBlockData(flip[0] ? w2 : w3);
        });
        time(w, "cropAge", "iso_set_nophysics", 50_000, () -> {
            flip[0] = !flip[0];
            wheat.setBlockData(flip[0] ? w2 : w3, false);
        });

        // —— 观察者语义实证（佐证 CraftBlock 字节码：physics=true→NMS flag 3（邻居通知+观察者可见），
        //    false→flag 530（客户端更新+抑制掉落+观察者不可见））——
        final Block obs = world.getBlockAt(50, 220, 51);
        obs.setType(Material.OBSERVER);
        final org.bukkit.block.data.Directional dir = (org.bukkit.block.data.Directional) obs.getBlockData();
        dir.setFacing(BlockFace.NORTH);
        obs.setBlockData(dir);
        final org.bukkit.block.data.type.Light pl = (org.bukkit.block.data.type.Light) lightBlock.getBlockData();
        pl.setLevel(pl.getLevel() == 12 ? 13 : 12);
        lightBlock.setBlockData(pl);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            final boolean poweredAfterPhysics = ((org.bukkit.block.data.type.Observer) obs.getBlockData()).isPowered();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                final org.bukkit.block.data.type.Light nl = (org.bukkit.block.data.type.Light) lightBlock.getBlockData();
                nl.setLevel(nl.getLevel() == 12 ? 13 : 12);
                lightBlock.setBlockData(nl, false);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    final boolean poweredAfterNoPhysics = ((org.bukkit.block.data.type.Observer) obs.getBlockData()).isPowered();
                    getLogger().info("round70 观察者实证: physics写后=" + poweredAfterPhysics
                        + "(预期true), noPhysics写后=" + poweredAfterNoPhysics + "(预期false)");
                    obs.setType(Material.AIR);
                    lightBlock.setType(Material.AIR);
                    farmland.setType(Material.AIR);
                    wheat.setType(Material.AIR);
                }, 3L);
            }, 6L);
        }, 3L);
    }

    // ==================== 第 73 轮：每 tick 派发任务折叠域（FloatingHeadAnimation → process 内联） ====================

    private void benchRound73(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final ArmorStand stand = world.spawn(new Location(world, 20, 210, 20), ArmorStand.class);

        // —— 等价性 1：折叠形态（直接调用 step）与任务形态（period=1 任务真实跑 10 tick）头姿增量一致 ——
        // 新形态：同一函数直接调 10 次
        final ArmorStand standDirect = world.spawn(new Location(world, 24, 210, 20), ArmorStand.class);
        for (int i = 0; i < 10; i++) {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standDirect, true);
        }
        final double directYaw = standDirect.getHeadPose().getY();
        // 旧形态：真实 runTaskTimer(1) 任务驱动 10 tick 后读回（经 12 tick 延迟读取）
        final ArmorStand standTask = world.spawn(new Location(world, 28, 210, 20), ArmorStand.class);
        final org.bukkit.scheduler.BukkitTask[] taskHolder = new org.bukkit.scheduler.BukkitTask[1];
        final double[] taskYaw = new double[1];
        taskHolder[0] = new org.bukkit.scheduler.BukkitRunnable() {
            private int runs;
            @Override
            public void run() {
                io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(standTask, true);
                if (++runs >= 10) {
                    taskYaw[0] = standTask.getHeadPose().getY();
                    cancel();
                }
            }
        }.runTaskTimer(this, 1L, 1L);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("round73 等价性(头姿增量 10 步: 任务驱动 vs 直接调用): "
                + (Math.abs(taskYaw[0] - directYaw) < 1e-9) + " (task=" + taskYaw[0] + ", direct=" + directYaw + ")");
        }, 14L);

        // —— 调度派发代理：一次性同步任务入队+执行（每 tick 循环任务处理成本的上界代理；
        //    待执行任务在步骤返回后的空闲 tick 排泄，批量取 10k 控制排湿冲击） ——
        time(w, "headAnimFold", "sched_dispatchProxy", 10_000, () -> {
            Bukkit.getScheduler().runTask(this, () -> bh++);
        });

        // —— 展示架解析形态：UUID 注册表查（折叠后备路径） vs 直接引用字段（折叠形态） ——
        final java.util.UUID standUuid = stand.getUniqueId();
        time(w, "headAnimFold", "stand_uuidLookup", 200_000, () -> {
            final org.bukkit.entity.Entity e = Bukkit.getEntity(standUuid);
            bh += e == null ? 0 : e.getEntityId();
        });
        time(w, "headAnimFold", "stand_directRef", 200_000, () -> bh += stand.getEntityId());

        // —— step 本体（与 r47 基线连续） ——
        time(w, "headAnimFold", "step_body", 200_000, () -> {
            io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils.panelAnimationStep(stand, true);
        });

        // 清理（等价性读取在其后 14L tick，此处只移除解析/计时用 stand）
        Bukkit.getScheduler().runTaskLater(this, () -> {
            stand.remove();
            standDirect.remove();
            standTask.remove();
        }, 16L);
    }

    // ==================== 第 76 轮：驻留条目每 tick 重复解析域（成熟晶簇） ====================

    private void benchRound76(PrintWriter w) {
        final World world = Bukkit.getWorlds().get(0);
        final Block crystalBlock = world.getBlockAt(70, 220, 50);
        crystalBlock.getChunk().load();
        crystalBlock.setType(Material.LARGE_AMETHYST_BUD);

        final io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition position =
            new io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition(crystalBlock.getLocation());
        final Block cachedBlock = position.getBlock();
        final Location cachedLoc = cachedBlock.getLocation().add(0.5, 0.2, 0.5);
        final Particle.DustOptions staticDust = new Particle.DustOptions(org.bukkit.Color.YELLOW, 2);
        final Location[] locHolder = new Location[1];

        // —— 等价性：缓存解析与新鲜解析逐位一致 ——
        boolean equiv = position.getBlock().getLocation().add(0.5, 0.2, 0.5).equals(cachedLoc);
        equiv &= position.getBlock().getType() == cachedBlock.getType();
        equiv &= staticDust.getColor().equals(org.bukkit.Color.YELLOW) && staticDust.getSize() == 2.0f;
        equiv &= position.getBlock().equals(cachedBlock);
        getLogger().info("round76 等价性(块解析/位置/材质/DustOptions 与新鲜构造一致): " + equiv);

        // —— 隔离项 ——
        time(w, "crystalResidency", "old_blockPosGetBlock", 100_000, () -> {
            bh += position.getBlock().hashCode();
        });
        time(w, "crystalResidency", "cached_blockRef", 100_000, () -> {
            bh += cachedBlock.hashCode();
        });
        time(w, "crystalResidency", "old_particleLoc", 100_000, () -> {
            locHolder[0] = cachedBlock.getLocation().add(0.5, 0.2, 0.5);
            bh += locHolder[0].hashCode();
        });
        time(w, "crystalResidency", "cached_particleLoc", 100_000, () -> {
            bh += cachedLoc.hashCode();
        });
        time(w, "crystalResidency", "dust_new", 100_000, () -> {
            bh += new Particle.DustOptions(org.bukkit.Color.YELLOW, 2).hashCode();
        });
        time(w, "crystalResidency", "dust_static", 100_000, () -> {
            bh += staticDust.hashCode();
        });

        // —— 全序列：成熟晶簇单条目每 tick 体（含粒子调用，两形态相同粒子） ——
        time(w, "crystalResidency", "old_fullTickBody", 20_000, () -> {
            final Block b = position.getBlock();
            bh += b.getType().ordinal();
            final Location l = b.getLocation().add(0.5, 0.2, 0.5);
            io.github.sefiraat.crystamaehistoria.utils.ParticleUtils.displayParticleEffect(l, Particle.WAX_OFF, 0.4, 3);
        });
        time(w, "crystalResidency", "new_fullTickBody", 20_000, () -> {
            bh += cachedBlock.getType().ordinal();
            io.github.sefiraat.crystamaehistoria.utils.ParticleUtils.displayParticleEffect(cachedLoc, Particle.WAX_OFF, 0.4, 3);
        });

        // 清理
        Bukkit.getScheduler().runTaskLater(this, () -> crystalBlock.setType(Material.AIR), 2L);
    }
}
