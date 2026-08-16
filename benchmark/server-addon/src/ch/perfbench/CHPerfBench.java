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
                () -> benchRound16(w)
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
}
