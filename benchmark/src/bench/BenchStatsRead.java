package bench;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 基准（第 22 轮）：玩家统计读取路径（真实 paper-api YamlConfiguration）
 *
 * 场景与真实规模一致：1 名玩家 × 69 法术（UNLOCKED 混合）× 274 故事
 * （UNLOCKED/GILDED 混合）。
 *
 * - 旧（每槽判定）：构建全路径 uuid.SPELL.<id>.UNLOCKED 从根逐层行走
 * - 新（页级）：单次 getConfigurationSection 解析子节 + 槽位相对路径读取
 * - 旧（解锁计数）：逐键重建全路径再从根解析（O(n) 次全路径行走）
 * - 新（解锁计数）：子节相对读取（单层行走）
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

    public static void run(Harness h) throws java.io.IOException {
        final int spellCount = SPELL_IDS.length; // 69
        final int storyCount = 274;

        // 构造真实规模统计文件
        new File("benchmark/build").mkdirs();
        final File file = new File("benchmark/build/stats-bench.yml");
        try (PrintWriter fw = new PrintWriter(file, StandardCharsets.UTF_8)) {
            // 玩家节：SPELL（69 项，2/3 UNLOCKED）+ STORY（274 项，1/2 UNLOCKED，1/4 GILDED）
            for (int i = 0; i < spellCount; i++) {
                fw.println(PLAYER + ".SPELL." + SPELL_IDS[i] + ".UNLOCKED: " + (i % 3 != 0));
                fw.println(PLAYER + ".SPELL." + SPELL_IDS[i] + ".TIMES_CAST: " + (i * 7));
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
        final ConfigurationSection spellSectionRef = cfg.getConfigurationSection(uuidStr + ".SPELL");

        // ———— 单次解锁判定 ————
        h.bench("stats.singleCheck", "old_full_path", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                if (cfg.getBoolean(uuidStr + ".SPELL." + SPELL_IDS[i % spellCount] + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 50_000);

        h.bench("stats.singleCheck", "new_relative_section", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                if (spellSectionRef.getBoolean(SPELL_IDS[i % spellCount] + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 200_000);

        // ———— 图鉴页 36 槽判定 ————
        h.bench("stats.pageCheck36", "old_full_path_x36", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final int page = (i % 2) * 36;
                for (int s = 0; s < 36; s++) {
                    final int idx = page + s;
                    if (idx < spellCount && cfg.getBoolean(uuidStr + ".SPELL." + SPELL_IDS[idx] + ".UNLOCKED")) bh++;
                }
            }
            return bh;
        }, 3000, 20, 5_000);

        h.bench("stats.pageCheck36", "new_section_once_relative_x36", size -> {
            long bh = 0;
            for (int i = 0; i < size; i++) {
                final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".SPELL");
                if (section == null) continue;
                final int page = (i % 2) * 36;
                for (int s = 0; s < 36; s++) {
                    final int idx = page + s;
                    if (idx < spellCount && section.getBoolean(SPELL_IDS[idx] + ".UNLOCKED")) bh++;
                }
            }
            return bh;
        }, 3000, 20, 20_000);

        // ———— 解锁计数（69 法术 / 274 故事）————
        h.bench("stats.countSpells69", "old_full_path_rebuild", size -> {
            long bh = 0;
            final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".SPELL");
            for (String spell : section.getKeys(false)) {
                if (cfg.getBoolean(uuidStr + ".SPELL." + spell + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 10_000);

        h.bench("stats.countSpells69", "new_relative_read", size -> {
            long bh = 0;
            final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".SPELL");
            for (String spell : section.getKeys(false)) {
                if (section.getBoolean(spell + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 20_000);

        h.bench("stats.countStories274", "old_full_path_rebuild", size -> {
            long bh = 0;
            final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
            for (String story : section.getKeys(false)) {
                if (cfg.getBoolean(uuidStr + ".STORY." + story + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 3_000);

        h.bench("stats.countStories274", "new_relative_read", size -> {
            long bh = 0;
            final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
            for (String story : section.getKeys(false)) {
                if (section.getBoolean(story + ".UNLOCKED")) bh++;
            }
            return bh;
        }, 3000, 20, 10_000);

        // ———— 等价性断言 ————
        boolean equivSingle = true;
        for (String id : SPELL_IDS) {
            equivSingle &= cfg.getBoolean(uuidStr + ".SPELL." + id + ".UNLOCKED")
                == spellSectionRef.getBoolean(id + ".UNLOCKED");
        }
        boolean equivCount = true;
        {
            int oldC = 0;
            int newC = 0;
            final ConfigurationSection section = cfg.getConfigurationSection(uuidStr + ".STORY");
            for (String story : storyKeys) {
                if (cfg.getBoolean(uuidStr + ".STORY." + story + ".UNLOCKED")) oldC++;
                if (section.getBoolean(story + ".UNLOCKED")) newC++;
            }
            equivCount = oldC == newC;
        }
        // 缺失子节语义：无统计玩家两路径同为 false
        final UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");
        final ConfigurationSection missingSection = cfg.getConfigurationSection(missing + ".SPELL");
        boolean equivMissing = !cfg.getBoolean(missing + ".SPELL.HEAL.UNLOCKED")
            && (missingSection == null);
        System.err.println("round22.statsEquivalence single=" + equivSingle
            + " count=" + equivCount + " missing=" + equivMissing);
    }
}
