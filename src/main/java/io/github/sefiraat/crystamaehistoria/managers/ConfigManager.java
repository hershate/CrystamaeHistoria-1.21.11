package io.github.sefiraat.crystamaehistoria.managers;

import com.google.common.base.Charsets;
import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.LiquefactionBasinCache;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Getter
public class ConfigManager {

    private final FileConfiguration blocks;
    private final FileConfiguration stories;
    private final FileConfiguration playerStats;
    private final FileConfiguration blockColors;
    private final FileConfiguration spells;

    public ConfigManager() {
        this.blocks = getConfig("blocks.yml", true);
        this.stories = getConfig("generic-stories.yml", true);
        this.playerStats = getConfig("player_stats.yml", false);
        this.blockColors = getConfig("block_colors.yml", false);
        this.spells = getConfig("spells.yml", false);
    }

    @Nonnull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private FileConfiguration getConfig(@Nonnull String fileName, boolean updateWithDefaults) {
        final CrystamaeHistoria plugin = CrystamaeHistoria.getInstance();
        final File file = new File(plugin.getDataFolder(), fileName);

        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        try {
            // 原实现在此处再 configuration.load(file) 一次——同一 tick 内文件不可能
            // 变化，纯属重复解析整个文件，已去除（5 个配置文件的启动解析量减半）
            if (updateWithDefaults) {
                updateConfig(configuration, file, fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configuration;
    }

    @ParametersAreNonnullByDefault
    private void updateConfig(FileConfiguration config, File file, String fileName) throws IOException {
        final InputStream inputStream = CrystamaeHistoria.getInstance().getResource(fileName);
        // jar 内资源缺失（打包异常）时跳过默认值合并，不能 NPE 中断插件启用
        if (inputStream == null) {
            return;
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
        final YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
        // 稳态启动（文件已含全部默认键）免落盘：copyDefaults 只补缺失键、不覆写
        // 既有值，故"无缺失键"时写盘产物与现存文件逐字节等价，纯属重复 IO
        boolean missingDefault = false;
        for (String key : defaults.getKeys(true)) {
            if (!config.contains(key)) {
                missingDefault = true;
                break;
            }
        }
        config.addDefaults(defaults);
        config.options().copyDefaults(true);
        if (missingDefault) {
            config.save(file);
        }
    }

    @ParametersAreNonnullByDefault
    public boolean spellEnabled(Spell spell) {
        // 性能：每次施法都会走到这里。spells.yml 仅在 loadConfig()（启动期）写入，
        // 同一值那时已同步进 Spell.enabled 字段——直接读字段，省去 YAML 路径分割与查表
        return spell.isEnabled();
    }

    public void loadConfig() {
        // Spells
        boolean newSpellKeys = false;
        for (SpellType spellType : SpellType.getCachedValues()) {
            Spell spell = spellType.getSpell();
            if (!spells.contains(spell.getId())) {
                // 原实现每补一个缺失键就整文件落盘一次（首次启动至多 69 次全量
                // 序列化+写盘）；改为全部补齐后一次落盘，终态一致
                spells.set(spell.getId(), true);
                newSpellKeys = true;
            }
            boolean enabled = spells.getBoolean(spell.getId());
            spell.setEnabled(enabled);
            if (enabled) {
                LiquefactionBasinCache.addSpellRecipe(spellType, spell.getRecipe());
            }
        }
        if (newSpellKeys) {
            try {
                final File file = new File(CrystamaeHistoria.getInstance().getDataFolder(), "spells.yml");
                spells.save(file);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void saveAll() {
        saveAll(false);
    }

    /**
     * 周期落盘（10 分钟）带脏判定跳过：统计纪元（PlayerStatistics，全部
     * 6 个写点递增）与上次保存时的水位线比较——无写入则跳过
     * player_stats.yml 的全量 YAML 序列化与磁盘写（稳态零落盘）；
     * config.yml 运行期无写入方，首个周期后同样跳过。
     * 关服路径 force=true 无条件落盘（最终冲刷）。
     */
    public void saveAll(boolean force) {
        final int currentEpoch = io.github.sefiraat.crystamaehistoria.player.PlayerStatistics.getStatsEpoch();
        final boolean statsDirty = force || currentEpoch != savedStatsEpoch;
        final boolean configDirty = force || !configSavedOnce;
        if (!statsDirty && !configDirty) {
            return;
        }
        CrystamaeHistoria.getInstance().getLogger().info("正在保存魔法水晶编年史的数据");
        final File configFile = new File(CrystamaeHistoria.getInstance().getDataFolder(), "config.yml");
        try {
            if (configDirty) {
                CrystamaeHistoria.getInstance().getConfig().save(configFile);
                configSavedOnce = true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (statsDirty) {
            saveResearches();
            savedStatsEpoch = currentEpoch;
        }
    }

    /** 上次 player_stats 落盘时的统计纪元（-1 保证首个周期必落盘） */
    private int savedStatsEpoch = -1;

    /** config.yml 是否已落盘过（运行期无写入方，一次即稳态） */
    private boolean configSavedOnce;

    private void saveResearches() {
        File file = new File(CrystamaeHistoria.getInstance().getDataFolder(), "player_stats.yml");
        try {
            playerStats.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
