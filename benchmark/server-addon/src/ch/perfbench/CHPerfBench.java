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
                () -> benchRound12(w)
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
