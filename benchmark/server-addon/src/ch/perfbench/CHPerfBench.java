package ch.perfbench;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.StoryUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
        File outDir = new File(getDataFolder().getParentFile(), "CHPerfBench");
        outDir.mkdirs();
        try (PrintWriter w = new PrintWriter(new File(outDir, "results.tsv"), "UTF-8")) {
            w.println("bench\tvariant\tmedian_ns_op");
            benchRaycast(w);
            benchStavePdc(w);
            benchInteractPaths(w);
            benchMachineTick(w);
            benchMachineTickMemo(w);
            benchStaveCast(w);
            benchGadgetTick(w);
            benchStoryPick(w);
        } catch (Exception e) {
            getLogger().severe("基准失败: " + e);
            e.printStackTrace();
        }
        getLogger().info("CHPERFBENCH COMPLETE, blackhole=" + bh);
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
        time(w, "configParse.blocksYml", "old_double_parse", 20, () -> {
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);
                cfg.load(blocksFile);
                bh += cfg.getKeys(false).size();
            } catch (Exception e) {
                bh++;
            }
        });
        time(w, "configParse.blocksYml", "new_single_parse", 40, () -> {
            try {
                org.bukkit.configuration.file.YamlConfiguration cfg =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(blocksFile);
                bh += cfg.getKeys(false).size();
            } catch (Exception e) {
                bh++;
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
}
