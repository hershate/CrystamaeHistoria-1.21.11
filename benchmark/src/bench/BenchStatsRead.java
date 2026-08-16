package bench;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 基准（第 23 轮）：玩家统计计数路径——纪元缓存
 *
 * 场景与真实规模一致：1 名玩家 × 69 法术（2/3 解锁）× 274 故事
 * （1/2 解锁、1/4 镀金）。
 *
 * - 旧（round-22 前）：逐键重建全路径从根解析
 * - 中（round-22）：子节相对读取（无缓存，每次全扫描）
 * - 新（round-23）：字段级纪元缓存——写点递增纪元后失效，无写入期间命中
 *
 * 纪元缓存以本地副本建模（真实实现位于 PlayerStatistics，需插件运行时；
 * 缓存逻辑为纯 Java 结构，副本逐行同构）。
 */
public final class BenchStatsRead {

    private static final UUID PLAYER = UUID.fromString("12345678-1234-1234-1234-123456789012");
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

    /** 与 PlayerStatistics 同构的纪元缓存副本 */
    private static int statsEpoch;
    private static final class CountCache {
        int storiesEpoch = -1;
        int stories;
    }
    private static final Map<UUID, CountCache> COUNT_CACHE = new HashMap<>();

    private static int cachedStoriesUnlocked(YamlConfiguration cfg, String uuidStr) {
        final CountCache cache = COUNT_CACHE.computeIfAbsent(PLAYER, k -> new CountCache());
        if (cache.storiesEpoch == statsEpoch) {
            return cache.stories;
        }
        final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
        int unlocked = 0;
        if (section != null) {
            for (String story : section.getKeys(false)) {
                if (section.getBoolean(story + ".UNLOCKED")) unlocked++;
            }
        }
        cache.stories = unlocked;
        cache.storiesEpoch = statsEpoch;
        return unlocked;
    }

    public static void run(Harness h) throws java.io.IOException {
        final int storyCount = 274;

        new File("benchmark/build").mkdirs();
        final File file = new File("benchmark/build/stats-bench-r23.yml");
        try (PrintWriter fw = new PrintWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 0; i < SPELL_IDS.length; i++) {
                fw.println(PLAYER + ".SPELL." + SPELL_IDS[i] + ".UNLOCKED: " + (i % 3 != 0));
            }
            for (int i = 0; i < storyCount; i++) {
                fw.println(PLAYER + ".STORY.MATERIAL_" + i + ".UNLOCKED: " + (i % 2 == 0));
                fw.println(PLAYER + ".STORY.MATERIAL_" + i + ".GILDED: " + (i % 4 == 0));
            }
        }
        final YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        final String uuidStr = PLAYER.toString();
        final List<String> storyKeys = new ArrayList<>();
        {
            final ConfigurationSection storySection = cfg.getConfigurationSection(uuidStr + ".STORY");
            storyKeys.addAll(storySection.getKeys(false));
        }

        // ———— 三变体对打（274 键计数，每次迭代一次完整计数）————
        h.bench("stats.countStories274", "old_full_path_rebuild", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (cfg.getBoolean(uuidStr + ".STORY." + story + ".UNLOCKED")) bh++;
                }
            }
            return bh;
        }, 3000, 20, 2_000);

        h.bench("stats.countStories274", "mid_relative_nocache", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
                for (String story : section.getKeys(false)) {
                    if (section.getBoolean(story + ".UNLOCKED")) bh++;
                }
            }
            return bh;
        }, 3000, 20, 5_000);

        h.bench("stats.countStories274", "new_epoch_cache_hit", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                bh += cachedStoriesUnlocked(cfg, uuidStr);
            }
            return bh;
        }, 3000, 20, 1_000_000);

        // 写后重算（缓存失效路径 = 相对读取一次）
        h.bench("stats.countAfterWrite", "new_epoch_miss_recompute", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                statsEpoch++; // 模拟一次统计写入（纪元递增）
                bh += cachedStoriesUnlocked(cfg, uuidStr);
            }
            return bh;
        }, 3000, 20, 5_000);

        // ———— 等价性与失效正确性 ————
        final CountCache cache = COUNT_CACHE.get(PLAYER);
        boolean equivSteady = cachedStoriesUnlocked(cfg, uuidStr) == 137; // 274 键 1/2 解锁
        // 写入（解锁一个未解锁故事）→ 纪元递增 → 计数必须 +1
        statsEpoch++;
        final int before = cachedStoriesUnlocked(cfg, uuidStr);
        cfg.set(uuidStr + ".STORY.MATERIAL_1.UNLOCKED", true);
        statsEpoch++; // 真实实现中由写方法递增（此处模拟直接配置写 + 手动纪元）
        final int after = cachedStoriesUnlocked(cfg, uuidStr);
        boolean invalidationOk = before == 137 && after == 138;
        // 恢复数据
        cfg.set(uuidStr + ".STORY.MATERIAL_1.UNLOCKED", false);
        statsEpoch++;
        boolean equivRestore = cachedStoriesUnlocked(cfg, uuidStr) == 137;
        System.err.println("round23.statsEpochCache steady=" + equivSteady
            + " invalidation=" + invalidationOk + " restore=" + equivRestore);
    }
}
