package bench;

import java.util.EnumMap;
import java.util.Map;

/**
 * 基准（第 21 轮）：NameUtils 枚举名 Title Case 路径
 *
 * - 旧：每次调用 TextUtils.toTitleCase（StringBuilder 逐字符 + toCharArray 分配
 *       + 4 个分隔符逐个 String.replace）
 * - 新：EnumMap 查表（纯函数，键为枚举实例）
 *
 * toTitleCase 为项目工具类的静态纯函数，此处原样复制实现（避免触发
 * ThemeType→bungee ChatColor 静态初始化链），与真实代码逐行一致。
 */
public final class BenchTitleCase {

    /** 与 TextUtils.toTitleCase(String) 逐行一致（verbatim 副本，见上） */
    static String toTitleCase(String string) {
        return toTitleCase(string, true);
    }

    static String toTitleCase(String string, boolean delimiterToSpace) {
        return toTitleCase(string, delimiterToSpace, " _'-/");
    }

    static String toTitleCase(String string, boolean delimiterToSpace, String delimiters) {
        final StringBuilder builder = new StringBuilder();
        boolean capNext = true;
        for (char character : string.toCharArray()) {
            character = (capNext) ? Character.toUpperCase(character) : Character.toLowerCase(character);
            builder.append(character);
            capNext = (delimiters.indexOf(character) >= 0);
        }
        String built = builder.toString();
        if (delimiterToSpace) {
            final char space = ' ';
            for (char c : delimiters.toCharArray()) {
                built = built.replace(c, space);
            }
        }
        return built;
    }

    /** 模型枚举：替代 org.bukkit.Material（脱离服务器运行时） */
    enum ModelMaterial {
        DEEPSLATE_BRICK_SLAB, POLISHED_GRANITE, OXIDIZED_CUT_COPPER, WAXED_OAK_PLANKS,
        CHISELED_STONE_BRICKS, INFESTED_CRACKED_STONE_BRICKS, SMOOTH_SANDSTONE,
        MOSSY_COBBLESTONE_WALL, RAW_IRON_BLOCK, TINTED_GLASS, ENDERMANS_VEIL,
        PHILOSOPHERS_STONE, GLASS_PANE, ANDESITE, DIRT, STONE, SAND, GRAVEL,
        COBBLED_DEEPSLATE_STAIRS, POLISHED_BLACKSTONE_BRICK_WALL, COPPER_ORE,
        DEEPSLATE_DIAMOND_ORE, EMERALD_BLOCK, AMETHYST_SHARD, ANCIENT_RUBBLE,
        CRACKED_NETHER_BRICKS, RED_SANDSTONE_SLAB, SPRUCE_PLANKS_STAIRS, QUARTZ_BRICK,
        GILDED_BLACKSTONE, MOSSY_STONE_BRICK_STAIRS
    }

    public static void run(Harness h) {
        final ModelMaterial[] materials = ModelMaterial.values();
        final int n = materials.length;
        final Map<ModelMaterial, String> cache = new EnumMap<>(ModelMaterial.class);
        for (ModelMaterial m : materials) {
            cache.put(m, toTitleCase(m.name()));
        }

        h.bench("compendium.titleCase", "old_rebuild_per_call", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                bh += toTitleCase(materials[i % n].name()).length();
            }
            return bh;
        }, 3000, 20, 100_000);

        h.bench("compendium.titleCase", "new_enummap_lookup", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                bh += cache.get(materials[i % n]).length();
            }
            return bh;
        }, 3000, 20, 1_000_000);

        // 等价性断言：全部枚举值两路径输出一致
        boolean equiv = true;
        for (ModelMaterial m : materials) {
            equiv &= toTitleCase(m.name()).equals(cache.get(m));
        }
        System.err.println("round21.titleCaseEquivalence=" + equiv);
    }
}
