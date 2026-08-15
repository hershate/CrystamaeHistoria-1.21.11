package ch.perfbench;

import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstanceStave;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.stave.SpellSlot;
import io.github.sefiraat.crystamaehistoria.utils.GeneralUtils;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentStaveDataType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Map;

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
