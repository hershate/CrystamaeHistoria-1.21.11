package io.github.sefiraat.crystamaehistoria.player;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatistics {

    /**
     * 统计写入纪元：本类全部 6 个写方法（unlock×3 / addUsage / addChronicle /
     * addRealisation）在写后递增。计数缓存以此校验失效——player_stats.yml
     * 运行期不存在类外写者（已核验），文件不重载只落盘，故纪元失效完备。
     */
    private static int statsEpoch;

    private static void bumpStatsEpoch() {
        statsEpoch++;
    }

    /** 当前统计纪元（周期落盘的脏判定水位线：与上次保存时的纪元比较） */
    public static int getStatsEpoch() {
        return statsEpoch;
    }

    /**
     * 成员资格纪元：仅 3 个解锁写方法（unlockSpell/unlockUniqueStory/
     * unlockStoryGilded）递增——计数类写入（addUsage/addChronicle/
     * addRealisation）只改 TIMES_* 数值，不改变 UNLOCKED/GILDED 成员资格。
     * 解锁集合缓存以此校验失效，避免施法（addUsage）与翻页交错的玩家
     * 反复触发集合重建。
     */
    private static int membershipEpoch;

    private static void bumpMembershipEpoch() {
        membershipEpoch++;
    }

    /** 每玩家计数缓存（字段级纪元，主线程单线程访问） */
    private static final class CountCache {
        int spellsEpoch = -1;
        int spells;
        int storiesEpoch = -1;
        int stories;
        int gildedEpoch = -1;
        int gilded;
    }

    /** 键为查询过计数的玩家（图鉴打开 / 液化池 rank 谓词触发者），条目微小且有界 */
    private static final Map<UUID, CountCache> COUNT_CACHE = new HashMap<>();

    /** 每玩家解锁集合缓存（字段级纪元）：图鉴页 36 槽判定的批量形态 */
    private static final class SetCache {
        int spellsEpoch = -1;
        java.util.Set<String> spellIds = java.util.Set.of();
        int storiesEpoch = -1;
        java.util.Set<Material> uniqueStories = java.util.EnumSet.noneOf(Material.class);
        int gildedEpoch = -1;
        java.util.Set<Material> gilded = java.util.EnumSet.noneOf(Material.class);
    }

    private static final Map<UUID, SetCache> SET_CACHE = new HashMap<>();

    @ParametersAreNonnullByDefault
    public static void unlockSpell(Player player, SpellType spellType) {
        unlockSpell(player.getUniqueId(), spellType);
    }

    @ParametersAreNonnullByDefault
    public static void unlockSpell(UUID player, SpellType spellType) {
        String path = player + "." + StatType.SPELL + "." + spellType.getId() + ".UNLOCKED";
        CrystamaeHistoria.getConfigManager().getPlayerStats().set(path, true);
        bumpStatsEpoch();
        bumpMembershipEpoch();
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedSpell(Player player, SpellType spellType) {
        return hasUnlockedSpell(player.getUniqueId(), spellType);
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedSpell(UUID player, SpellType spellType) {
        String path = player + "." + StatType.SPELL + "." + spellType.getId() + ".UNLOCKED";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(path);
    }

    /**
     * 玩家法术统计子节（页级批量判定用）：图鉴翻页对 36 个槽位逐个
     * hasUnlocked 时，先取本节再做相对路径读取，避免每次从根走全路径。
     *
     * @return 对应子节；玩家无统计时 null（配合相对读取重载按未解锁处理）
     */
    @Nullable
    @ParametersAreNonnullByDefault
    public static ConfigurationSection getSpellStatSection(UUID player) {
        return CrystamaeHistoria.getConfigManager().getPlayerStats()
            .getConfigurationSection(player + "." + StatType.SPELL);
    }

    /**
     * 同 {@link #hasUnlockedSpell(UUID, SpellType)}，但接受页级预解析的
     * {@link #getSpellStatSection(UUID)} 子节做相对读取（结果等价）。
     */
    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedSpell(UUID player, SpellType spellType, @Nullable ConfigurationSection spellSection) {
        if (spellSection == null) {
            return false;
        }
        return spellSection.getBoolean(spellType.getId() + ".UNLOCKED");
    }

    @ParametersAreNonnullByDefault
    public static void addUsage(Player player, SpellType spellType) {
        addUsage(player.getUniqueId(), spellType);
    }

    @ParametersAreNonnullByDefault
    public static void addUsage(UUID player, SpellType spellType) {
        // 单次路径构建：读改写共用同一路径（原实现 getUsages 与 set 各构建一次）
        final String path = player + "." + StatType.SPELL + "." + spellType.getId() + ".TIMES_CAST";
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();
        stats.set(path, stats.getInt(path) + 1);
        bumpStatsEpoch();
    }

    @ParametersAreNonnullByDefault
    public static int getUsages(UUID player, SpellType spellType) {
        String path = player + "." + StatType.SPELL + "." + spellType.getId() + ".TIMES_CAST";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getInt(path);
    }

    @ParametersAreNonnullByDefault
    public static int getUsages(Player player, SpellType spellType) {
        return getUsages(player.getUniqueId(), spellType);
    }

    @ParametersAreNonnullByDefault
    public static void unlockUniqueStory(Player player, BlockDefinition definition) {
        unlockUniqueStory(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static void unlockUniqueStory(UUID player, BlockDefinition definition) {
        String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".UNLOCKED";
        CrystamaeHistoria.getConfigManager().getPlayerStats().set(path, true);
        bumpStatsEpoch();
        bumpMembershipEpoch();
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedUniqueStory(Player player, BlockDefinition definition) {
        return hasUnlockedUniqueStory(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedUniqueStory(UUID player, BlockDefinition definition) {
        return hasUnlockedUniqueStory(player, definition.getMaterial());
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedUniqueStory(UUID player, Material material) {
        String path = player + "." + StatType.STORY + "." + material + ".UNLOCKED";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(path);
    }

    /** 玩家故事统计子节（页级批量判定用），语义同 {@link #getSpellStatSection(UUID)} */
    @Nullable
    @ParametersAreNonnullByDefault
    public static ConfigurationSection getStoryStatSection(UUID player) {
        return CrystamaeHistoria.getConfigManager().getPlayerStats()
            .getConfigurationSection(player + "." + StatType.STORY);
    }

    /** 同 {@link #hasUnlockedUniqueStory(UUID, Material)}，相对预解析子节读取（结果等价） */
    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedUniqueStory(UUID player, Material material, @Nullable ConfigurationSection storySection) {
        if (storySection == null) {
            return false;
        }
        return storySection.getBoolean(material + ".UNLOCKED");
    }

    @ParametersAreNonnullByDefault
    public static void unlockStoryGilded(UUID player, BlockDefinition definition) {
        String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".GILDED";
        CrystamaeHistoria.getConfigManager().getPlayerStats().set(path, true);
        bumpStatsEpoch();
        bumpMembershipEpoch();
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedStoryGilded(Player player, BlockDefinition definition) {
        return hasUnlockedStoryGilded(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedStoryGilded(UUID player, BlockDefinition definition) {
        return hasUnlockedStoryGilded(player, definition.getMaterial());
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedStoryGilded(UUID player, Material material) {
        String path = player + "." + StatType.STORY + "." + material + ".GILDED";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(path);
    }

    /** 同 {@link #hasUnlockedStoryGilded(UUID, Material)}，相对预解析子节读取（结果等价） */
    @ParametersAreNonnullByDefault
    public static boolean hasUnlockedStoryGilded(UUID player, Material material, @Nullable ConfigurationSection storySection) {
        if (storySection == null) {
            return false;
        }
        return storySection.getBoolean(material + ".GILDED");
    }

    /**
     * 已解锁法术 id 集合的纪元缓存快照（不可变视图，调用方禁止变异）：
     * 图鉴翻页对 36 槽逐个判定的批量形态——集合成员判定替代逐槽
     * YAML 相对读取；纪元失效协议与计数缓存（{@link #getSpellsUnlocked}）
     * 相同（全部统计写点递增纪元）。玩家无统计时空集。
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static java.util.Set<String> getUnlockedSpellIdSet(UUID player) {
        final SetCache cache = SET_CACHE.computeIfAbsent(player, k -> new SetCache());
        if (cache.spellsEpoch != membershipEpoch) {
            final java.util.Set<String> ids = new java.util.HashSet<>();
            final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
                .getConfigurationSection(player + "." + StatType.SPELL);
            if (section != null) {
                for (String spell : section.getKeys(false)) {
                    if (section.getBoolean(spell + ".UNLOCKED")) {
                        ids.add(spell);
                    }
                }
            }
            cache.spellIds = java.util.Set.copyOf(ids);
            cache.spellsEpoch = membershipEpoch;
        }
        return cache.spellIds;
    }

    /** 已解锁独特故事材质集合的纪元缓存快照（同 {@link #getUnlockedSpellIdSet}） */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static java.util.Set<Material> getUnlockedUniqueStorySet(UUID player) {
        final SetCache cache = SET_CACHE.computeIfAbsent(player, k -> new SetCache());
        if (cache.storiesEpoch != membershipEpoch) {
            final java.util.Set<Material> materials = java.util.EnumSet.noneOf(Material.class);
            final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
                .getConfigurationSection(player + "." + StatType.STORY);
            if (section != null) {
                for (String story : section.getKeys(false)) {
                    if (section.getBoolean(story + ".UNLOCKED")) {
                        try {
                            materials.add(Material.valueOf(story));
                        } catch (IllegalArgumentException e) {
                            // 键名非材质（crafted/编辑数据）：跳过
                        }
                    }
                }
            }
            cache.uniqueStories = java.util.Collections.unmodifiableSet(materials);
            cache.storiesEpoch = membershipEpoch;
        }
        return cache.uniqueStories;
    }

    /** 已镀金材质集合的纪元缓存快照（同 {@link #getUnlockedSpellIdSet}） */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static java.util.Set<Material> getGildedSet(UUID player) {
        final SetCache cache = SET_CACHE.computeIfAbsent(player, k -> new SetCache());
        if (cache.gildedEpoch != membershipEpoch) {
            final java.util.Set<Material> materials = java.util.EnumSet.noneOf(Material.class);
            final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
                .getConfigurationSection(player + "." + StatType.STORY);
            if (section != null) {
                for (String story : section.getKeys(false)) {
                    if (section.getBoolean(story + ".GILDED")) {
                        try {
                            materials.add(Material.valueOf(story));
                        } catch (IllegalArgumentException e) {
                            // 键名非材质（crafted/编辑数据）：跳过
                        }
                    }
                }
            }
            cache.gilded = java.util.Collections.unmodifiableSet(materials);
            cache.gildedEpoch = membershipEpoch;
        }
        return cache.gilded;
    }

    @ParametersAreNonnullByDefault
    public static void addChronicle(Player player, BlockDefinition definition) {
        addChronicle(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static void addChronicle(UUID player, BlockDefinition definition) {
        // 单次路径构建（原实现 getChronicle 与 set 各构建一次）
        final String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".TIMES_CHRONICLED";
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();
        stats.set(path, stats.getInt(path) + 1);
        bumpStatsEpoch();
    }

    @ParametersAreNonnullByDefault
    public static int getChronicle(UUID player, BlockDefinition definition) {
        String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".TIMES_CHRONICLED";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getInt(path);
    }

    @ParametersAreNonnullByDefault
    public static int getChronicle(Player player, BlockDefinition definition) {
        return getChronicle(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static void addRealisation(Player player, BlockDefinition definition) {
        // 复制粘贴错误：原实现误调 addChronicle（现实转化计数被记入发掘计数）
        addRealisation(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static void addRealisation(UUID player, BlockDefinition definition) {
        // 单次路径构建（原实现 getRealisation 与 set 各构建一次）
        final String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".TIMES_REALISED";
        final org.bukkit.configuration.file.FileConfiguration stats =
            CrystamaeHistoria.getConfigManager().getPlayerStats();
        stats.set(path, stats.getInt(path) + 1);
        bumpStatsEpoch();
    }

    @ParametersAreNonnullByDefault
    public static int getRealisation(UUID player, BlockDefinition definition) {
        String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".TIMES_REALISED";
        return CrystamaeHistoria.getConfigManager().getPlayerStats().getInt(path);
    }

    @ParametersAreNonnullByDefault
    public static int getRealisation(Player player, BlockDefinition definition) {
        // 复制粘贴错误：原实现误调 getChronicle（故事集图鉴的"现实转化次数"显示的是发掘次数）
        return getRealisation(player.getUniqueId(), definition);
    }

    @ParametersAreNonnullByDefault
    public static BlockRank getBlockRank(UUID uuid, BlockDefinition definition) {
        final int chronicleAmount = Math.min(getChronicle(uuid, definition), 100);
        final int realisedAmount = Math.min(getRealisation(uuid, definition), 100);
        final int blockValue = (chronicleAmount + realisedAmount) / 2;
        return BlockRank.getByAmount(blockValue);
    }

    @ParametersAreNonnullByDefault
    public static StoryRank getStoryRank(UUID uuid) {
        int total = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().size();
        final int unlocked = getStoriesUnlocked(uuid);
        // blocks.yml 被清空时 total 为 0：除零得 NaN，按 0% 处理
        final double percent = total > 0 ? ((double) unlocked / total) * 100 : 0;
        return StoryRank.getByPercent(percent);
    }

    @ParametersAreNonnullByDefault
    public static int getStoriesUnlocked(UUID uuid) {
        // 纪元缓存：写方法递增纪元后失效；重复查询（图鉴翻页/液化池 rank 谓词）
        // 在无写入期间命中缓存，免 O(n) 键扫描
        final CountCache cache = COUNT_CACHE.computeIfAbsent(uuid, k -> new CountCache());
        if (cache.storiesEpoch == statsEpoch) {
            return cache.stories;
        }
        final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
            .getConfigurationSection(uuid + "." + StatType.STORY);
        int unlocked = 0;
        if (section != null) {
            // 相对路径读取：原实现逐键重建全路径（uuid.STORY.<key>.UNLOCKED）再从根解析，
            // O(n) 次全路径行走；子节相对读取只走最后一层，结果等价
            for (String story : section.getKeys(false)) {
                if (section.getBoolean(story + ".UNLOCKED")) unlocked++;
            }
        }
        cache.stories = unlocked;
        cache.storiesEpoch = statsEpoch;
        return unlocked;
    }

    @ParametersAreNonnullByDefault
    public static String getStoryRankString(UUID uuid) {
        int total = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().size();
        int unlocked = getStoriesUnlocked(uuid);
        StoryRank storyRank = StoryRank.getByPercent(((double) unlocked / total) * 100);
        return MessageFormat.format(
            "{0}故事等级: {1}{2}{0} ({3}/{4})",
            ThemeType.PASSIVE.getColor(),
            storyRank.getTheme().getColor(),
            storyRank.getTheme().getLoreLine(),
            unlocked,
            total
        );
    }

    @ParametersAreNonnullByDefault
    public static SpellRank getSpellRank(UUID uuid) {
        int total = SpellType.getEnabledSpells().length;
        int unlocked = getSpellsUnlocked(uuid);
        return SpellRank.getByPercent(((double) unlocked / total) * 100);
    }

    @ParametersAreNonnullByDefault
    public static int getSpellsUnlocked(UUID uuid) {
        // 纪元缓存（同 getStoriesUnlocked）
        final CountCache cache = COUNT_CACHE.computeIfAbsent(uuid, k -> new CountCache());
        if (cache.spellsEpoch == statsEpoch) {
            return cache.spells;
        }
        final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
            .getConfigurationSection(uuid + "." + StatType.SPELL);
        int unlocked = 0;
        if (section != null) {
            // 相对路径读取（原实现逐键重建全路径从根解析）
            for (String spell : section.getKeys(false)) {
                if (section.getBoolean(spell + ".UNLOCKED")) unlocked++;
            }
        }
        cache.spells = unlocked;
        cache.spellsEpoch = statsEpoch;
        return unlocked;
    }

    @ParametersAreNonnullByDefault
    public static String getSpellRankString(UUID uuid) {
        int total = SpellType.getEnabledSpells().length;
        int unlocked = getSpellsUnlocked(uuid);
        SpellRank spellRank = SpellRank.getByPercent(((double) unlocked / total) * 100);
        return MessageFormat.format(
            "{0}法术等级: {1}{2}{0} ({3}/{4})",
            ThemeType.PASSIVE.getColor(),
            spellRank.getTheme().getColor(),
            spellRank.getTheme().getLoreLine(),
            unlocked,
            total
        );
    }

    @ParametersAreNonnullByDefault
    public static GildingRank getGildingRank(UUID uuid) {
        int total = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().size();
        final int unlocked = getBlocksGilded(uuid);
        return GildingRank.getByPercent(((double) unlocked / total) * 100);
    }

    @ParametersAreNonnullByDefault
    public static int getBlocksGilded(UUID uuid) {
        // 纪元缓存（同 getStoriesUnlocked）
        final CountCache cache = COUNT_CACHE.computeIfAbsent(uuid, k -> new CountCache());
        if (cache.gildedEpoch == statsEpoch) {
            return cache.gilded;
        }
        final ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats()
            .getConfigurationSection(uuid + "." + StatType.STORY);
        int unlocked = 0;
        if (section != null) {
            // 相对路径读取（同 getStoriesUnlocked）
            for (String story : section.getKeys(false)) {
                if (section.getBoolean(story + ".GILDED")) unlocked++;
            }
        }
        cache.gilded = unlocked;
        cache.gildedEpoch = statsEpoch;
        return unlocked;
    }

    @ParametersAreNonnullByDefault
    public static String getGildingRankString(UUID uuid) {
        int total = CrystamaeHistoria.getStoriesManager().getBlockDefinitionMap().size();
        int unlocked = getBlocksGilded(uuid);
        GildingRank gildingRank = GildingRank.getByPercent(((double) unlocked / total) * 100);
        return MessageFormat.format(
            "{0}镀金等级: {1}{2}{0} ({3}/{4})",
            ThemeType.PASSIVE.getColor(),
            gildingRank.getTheme().getColor(),
            gildingRank.getTheme().getLoreLine(),
            unlocked,
            total
        );
    }

    enum StatType {
        SPELL,
        STORY
    }
}

