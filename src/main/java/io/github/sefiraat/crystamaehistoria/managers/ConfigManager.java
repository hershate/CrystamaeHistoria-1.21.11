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
        config.addDefaults(defaults);
        config.options().copyDefaults(true);
        config.save(file);
    }

    @ParametersAreNonnullByDefault
    public boolean spellEnabled(Spell spell) {
        // 性能：每次施法都会走到这里。spells.yml 仅在 loadConfig()（启动期）写入，
        // 同一值那时已同步进 Spell.enabled 字段——直接读字段，省去 YAML 路径分割与查表
        return spell.isEnabled();
    }

    public void loadConfig() {
        // Spells
        for (SpellType spellType : SpellType.getCachedValues()) {
            Spell spell = spellType.getSpell();
            if (!spells.contains(spell.getId())) {
                try {
                    final File file = new File(CrystamaeHistoria.getInstance().getDataFolder(), "spells.yml");
                    spells.set(spell.getId(), true);
                    spells.save(file);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
            boolean enabled = spells.getBoolean(spell.getId());
            spell.setEnabled(enabled);
            if (enabled) {
                LiquefactionBasinCache.addSpellRecipe(spellType, spell.getRecipe());
            }
        }
    }

    public void saveAll() {
        CrystamaeHistoria.getInstance().getLogger().info("正在保存魔法水晶编年史的数据");
        final File configFile = new File(CrystamaeHistoria.getInstance().getDataFolder(), "config.yml");
        try {
            CrystamaeHistoria.getInstance().getConfig().save(configFile);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        saveResearches();
    }

    private void saveResearches() {
        File file = new File(CrystamaeHistoria.getInstance().getDataFolder(), "player_stats.yml");
        try {
            playerStats.save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
