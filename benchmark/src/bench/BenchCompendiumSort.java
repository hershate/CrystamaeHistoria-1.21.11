package bench;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 基准（第 21 轮）：图鉴翻页列表排序路径
 *
 * - 旧（法术集）：每次打开页面 Arrays.asList(共享数组) + sort(comparing(id))。
 *   首开后共享数组已被排为 id 序，稳态为"对已排序数组重排"（TimSort 最好情形），
 *   基准如实测稳态；旧路径同时原地污染共享缓存数组。
 * - 旧（故事集/镀金集）：每次 new ArrayList<>(map.values())（EnumMap 序，非排序序）
 *   + sort(comparing(material.name()))——输入每次同序、非排序序。
 * - 新：启动期一次排序的快照，翻页直接 subList（零排序零复制）。
 *
 * 模型说明：SpellType/BlockDefinition 需服务器运行时，以 String 键模型类
 * 复现同形态 java.util 操作序列（comparator 为 String 比较器，与真实
 * id 比较器 / material.name() 比较器同构）。
 */
public final class BenchCompendiumSort {

    /** 69 个法术 id 样本（与真实 SpellType 数量一致，取代表性命名形态） */
    private static final String[] SPELL_IDS = {
        "ABSTRACT_VOID", "AIR_NOVA", "AIR_SPRITE", "ANCIENT_DEFENCE", "ANIMANIACS",
        "ANTI_PRISM", "BATTERING_RAM", "BLOOD_MAGICS", "BOBULATE", "BREAK",
        "BRIGHT", "CALL_LIGHTNING", "CASCADA", "CHAOS", "CHILL_WIND",
        "COMPASS", "CURIFICATION_RITUAL", "DEITY", "EARTH_NOVA", "EASTER_EGG",
        "ENDERMANS_VEIL", "ESCAPE_ROPE", "ETHEREAL_FLOW", "FAN_OF_ARROWS", "FIREBALL",
        "FLAME_SPRITE", "FLAMETHROWER", "FLESH_TO_STONE", "FORTUNE_FAVOUR", "GAIA_BLESSING",
        "GENTLE_TOUCH", "GRAVITAS", "GREEN_THUMB", "HARVEST", "HEALING_WATERS",
        "HELLSCAPE", "HERMES", "HOLY_COW", "IMBUED_TOOL", "KNOCKBACK_WAVE",
        "LUMBERJACK", "MAGICAL_FLUIDS", "METEOR", "MIDAS_TOUCH", "MOLOTOV",
        "PEACFUL_PROSE", "PHILOSOPHERS_STONE", "PLUTOS_DECENT", "PUSH", "RAISE_DEAD",
        "REAP_SOULS", "REFRACTING_LENS", "RESTORATIVE_AURA", "RIVER_OF_LIFE", "SEA_OF_FLAME",
        "SHATTER", "SLEEPWALK", "SPARK_FIELD", "SPECTRAL_FAMILIAR", "STAVE_MASTER",
        "STONE_COLD", "STRIP_MINE", "SUMMON_GOLEM", "TELEKINESIS", "TEMPORAL_ANCHOR",
        "THUNDERBOLT", "TUNNEL_BORE", "VACUUM", "WITHER_WEATHER"
    };

    /** 274 个材质名样本（与 blocks.yml 定义数同量级） */
    private static final String[] MATERIAL_NAMES = buildMaterialNames();

    private static String[] buildMaterialNames() {
        final String[] prefixes = {
            "DEEPSLATE", "POLISHED", "SMOOTH", "CUT", "MOSSY", "CRACKED",
            "CHISELED", "INFESTED", "WAXED", "OXIDIZED", "RAW", "COBBLED"
        };
        final String[] bases = {
            "STONE", "GRANITE", "DIORITE", "ANDESITE", "BRICKS", "SANDSTONE",
            "QUARTZ", "COPPER", "IRON", "GOLD", "DIAMOND", "EMERALD",
            "SLATE", "BASALT", "MARBLE", "TUFF", "CALCITE", "AMETHYST",
            "OAK_PLANKS", "SPRUCE_PLANKS", "GLASS", "CONCRETE", "TERRACOTTA", "WOOL"
        };
        final List<String> names = new ArrayList<>();
        for (String prefix : prefixes) {
            for (String base : bases) {
                names.add(prefix + "_" + base);
                names.add(prefix + "_" + base + "_STAIRS");
            }
        }
        // 补足至 274 个（追加后缀变体）
        String[] extras = {"ANDESITE_WALL", "BRICK_WALL", "STONE_WALL", "COBBLE_WALL", "QUARTZ_WALL", "GLASS_PANE", "TINTED_GLASS", "SAND", "GRAVEL", "DIRT"};
        for (String extra : extras) names.add(extra);
        return names.toArray(new String[0]);
    }

    public static void run(Harness h) {
        final int spellCount = SPELL_IDS.length;   // 69
        final int blockCount = MATERIAL_NAMES.length; // ~274

        // —— 模型状态 ——
        // 法术：旧路径稳态 = 共享数组已被首开排序，后续开页对其重排
        final String[] sharedSpellArray = SPELL_IDS.clone();
        Arrays.sort(sharedSpellArray); // 模拟首开后稳态
        // 新：启动期一次排序的快照（不可变）
        final String[] preSortedSpells = SPELL_IDS.clone();
        Arrays.sort(preSortedSpells);

        // 方块：map 序（固定非排序序）每次被复制后排序
        final List<String> mapOrderBlocks = Arrays.asList(MATERIAL_NAMES);
        final List<String> snapshotBlocks = new ArrayList<>(mapOrderBlocks);
        snapshotBlocks.sort(Comparator.naturalOrder());

        final Comparator<String> byId = Comparator.comparing(s -> s);

        // —— 法术集翻页（69 项）——
        h.bench("compendium.spellSort", "old_resort_shared_array", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<String> list = Arrays.asList(sharedSpellArray);
                list.sort(byId); // 稳态：对已排序数组重排
                bh += list.get(i % spellCount).length();
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("compendium.spellSort", "new_snapshot_sublist", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<String> list = Arrays.asList(preSortedSpells);
                final List<String> page = list.subList((i % 2) * 36, Math.min((i % 2) * 36 + 36, spellCount));
                bh += page.get(i % page.size()).length();
            }
            return bh;
        }, 3000, 20, 100_000);

        // —— 故事集/镀金集翻页（274 项）——
        h.bench("compendium.blockSort", "old_copy_and_sort", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final List<String> copy = new ArrayList<>(mapOrderBlocks);
                copy.sort(byId); // 输入每次同序、非排序序（EnumMap 序）
                bh += copy.get(i % blockCount).length();
            }
            return bh;
        }, 3000, 20, 5_000);

        h.bench("compendium.blockSort", "new_snapshot_sublist", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final int start = (i % 2) * 36;
                final List<String> page = snapshotBlocks.subList(start, Math.min(start + 36, blockCount));
                bh += page.get(i % page.size()).length();
            }
            return bh;
        }, 3000, 20, 100_000);

        // —— 等价性断言：新旧排序终态一致 ——
        final List<String> oldOrder = new ArrayList<>(mapOrderBlocks);
        oldOrder.sort(byId);
        boolean equivBlocks = oldOrder.equals(snapshotBlocks);
        final List<String> spellOrder = Arrays.asList(SPELL_IDS.clone());
        spellOrder.sort(byId);
        boolean equivSpells = spellOrder.equals(Arrays.asList(preSortedSpells));
        System.err.println("round21.sortEquivalence spells=" + equivSpells + " blocks=" + equivBlocks);
    }
}
