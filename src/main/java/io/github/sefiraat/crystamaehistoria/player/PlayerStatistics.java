package io.github.sefiraat.crystamaehistoria.player;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.stories.BlockDefinition;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.UUID;

public class PlayerStatistics {

    @ParametersAreNonnullByDefault
    public static void unlockSpell(Player player, SpellType spellType) {
        unlockSpell(player.getUniqueId(), spellType);
    }

    @ParametersAreNonnullByDefault
    public static void unlockSpell(UUID player, SpellType spellType) {
        String path = player + "." + StatType.SPELL + "." + spellType.getId() + ".UNLOCKED";
        CrystamaeHistoria.getConfigManager().getPlayerStats().set(path, true);
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

    @ParametersAreNonnullByDefault
    public static void unlockStoryGilded(UUID player, BlockDefinition definition) {
        String path = player + "." + StatType.STORY + "." + definition.getMaterial() + ".GILDED";
        CrystamaeHistoria.getConfigManager().getPlayerStats().set(path, true);
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
        String path = uuid + "." + StatType.STORY;
        ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats().getConfigurationSection(path);
        if (section == null) {
            return 0;
        }
        int unlocked = 0;
        for (String story : section.getKeys(false)) {
            String storyPath = uuid + "." + StatType.STORY + "." + story + ".UNLOCKED";
            if (CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(storyPath)) unlocked++;
        }
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
        String path = uuid + "." + StatType.SPELL;
        ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats().getConfigurationSection(path);
        if (section == null) {
            return 0;
        }
        int unlocked = 0;
        for (String spell : section.getKeys(false)) {
            String storyPath = uuid + "." + StatType.SPELL + "." + spell + ".UNLOCKED";
            if (CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(storyPath)) unlocked++;
        }
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
        String path = uuid + "." + StatType.STORY;
        ConfigurationSection section = CrystamaeHistoria.getConfigManager().getPlayerStats().getConfigurationSection(path);
        if (section == null) {
            return 0;
        }
        int unlocked = 0;
        for (String story : section.getKeys(false)) {
            String storyPath = uuid + "." + StatType.STORY + "." + story + ".GILDED";
            if (CrystamaeHistoria.getConfigManager().getPlayerStats().getBoolean(storyPath)) unlocked++;
        }
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

