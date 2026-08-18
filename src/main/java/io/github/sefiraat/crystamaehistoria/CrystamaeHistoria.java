package io.github.sefiraat.crystamaehistoria;

import com.google.common.base.Preconditions;
import io.github.sefiraat.crystamaehistoria.commands.GetRanks;
import io.github.sefiraat.crystamaehistoria.commands.HistoriaCommand;
import io.github.sefiraat.crystamaehistoria.commands.OpenSpellCompendium;
import io.github.sefiraat.crystamaehistoria.commands.OpenStoryCompendium;
import io.github.sefiraat.crystamaehistoria.commands.TestSpell;
import io.github.sefiraat.crystamaehistoria.commands.TestWand;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicFallingBlock;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicProjectile;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicSummon;
import io.github.sefiraat.crystamaehistoria.managers.ConfigManager;
import io.github.sefiraat.crystamaehistoria.managers.ListenerManager;
import io.github.sefiraat.crystamaehistoria.managers.RunnableManager;
import io.github.sefiraat.crystamaehistoria.managers.StoriesManager;
import io.github.sefiraat.crystamaehistoria.managers.SupportedPluginManager;
import io.github.sefiraat.crystamaehistoria.slimefun.ArtisticItems;
import io.github.sefiraat.crystamaehistoria.slimefun.Exalted;
import io.github.sefiraat.crystamaehistoria.slimefun.Gadgets;
import io.github.sefiraat.crystamaehistoria.slimefun.ItemGroups;
import io.github.sefiraat.crystamaehistoria.slimefun.Materials;
import io.github.sefiraat.crystamaehistoria.slimefun.Mechanisms;
import io.github.sefiraat.crystamaehistoria.slimefun.NetheoPlants;
import io.github.sefiraat.crystamaehistoria.slimefun.Runes;
import io.github.sefiraat.crystamaehistoria.slimefun.Tools;
import io.github.sefiraat.crystamaehistoria.slimefun.Uniques;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.chroniclerpanel.ChroniclerPanel;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.chroniclerpanel.ChroniclerPanelCache;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.crafting.EphemeralWorkBench;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.libraries.paperlib.PaperLib;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CrystamaeHistoria extends JavaPlugin implements SlimefunAddon {

    private static CrystamaeHistoria instance;

    private ConfigManager configManager;
    private StoriesManager storiesManager;
    private ListenerManager listenerManager;
    private RunnableManager runnableManager;
    private SpellMemory spellMemory;
    private SupportedPluginManager supportedPluginManager;
    private HistoriaCommand historiaCommand;

    public static CrystamaeHistoria getInstance() {
        return instance;
    }

    @Nonnull
    public static CrystamaeHistoria instance() {
        return Objects.requireNonNull(instance, "CrystamaeHistoria 还没有启用");
    }

    public static ConfigManager getConfigManager() {
        return instance.configManager;
    }

    public static StoriesManager getStoriesManager() {
        return instance.storiesManager;
    }

    public static ListenerManager getListenerManager() {
        return instance.listenerManager;
    }

    public static RunnableManager getRunnableManager() {
        return instance.runnableManager;
    }

    public static SpellMemory getSpellMemory() {
        return instance.spellMemory;
    }

    public static SupportedPluginManager getSupportedPluginManager() {
        return instance.supportedPluginManager;
    }

    public static PluginManager getPluginManager() {
        return instance.getServer().getPluginManager();
    }

    @Nonnull
    public static Map<MagicProjectile, Pair<CastInformation, Long>> getProjectileMap() {
        return instance.spellMemory.getProjectileMap();
    }

    @Nonnull
    public static Map<MagicFallingBlock, Pair<CastInformation, Long>> getFallingBlockMap() {
        return instance.spellMemory.getFallingBlockMap();
    }

    @Nonnull
    public static Map<UUID, Pair<CastInformation, Long>> getStrikeMap() {
        return instance.spellMemory.getStrikeMap();
    }

    @Nonnull
    public static Map<MagicSummon, Long> getSummonedEntityMap() {
        return instance.spellMemory.getSummonedEntities();
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static CastInformation getProjectileCastInfo(MagicProjectile magicProjectile) {
        CastInformation castInformation = getProjectileMap().get(magicProjectile).getFirstValue();
        Preconditions.checkNotNull(
            castInformation,
            "Cast information is null, magical projectile spawned incorrectly."
        );
        return castInformation;
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static CastInformation getFallingBlockCastInfo(MagicFallingBlock magicFallingBlock) {
        CastInformation castInformation = getFallingBlockMap().get(magicFallingBlock).getFirstValue();
        Preconditions.checkNotNull(
            castInformation,
            "Cast information is null, magical falling block spawned incorrectly."
        );
        return castInformation;
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static CastInformation getStrikeCastInfo(UUID lightningStrike) {
        CastInformation castInformation = getStrikeMap().get(lightningStrike).getFirstValue();
        Preconditions.checkNotNull(
            castInformation,
            "Cast information is null, magical projectile spawned incorrectly."
        );
        return castInformation;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("########################################");
        getLogger().info("     CrystamaeHistoria  魔法水晶编年史    ");
        getLogger().info("########################################");

        if (PaperLib.isSpigot() && !PaperLib.isPaper()) {
            getLogger().warning("==========================================");
            getLogger().warning("魔法水晶编年史使用了部分 Paper 的特性");
            getLogger().warning("需要 Paper 以及衍生服务端才能运行");
            getLogger().warning("");
            getLogger().warning("Spigot 服务端无法运行魔法水晶编年史");
            getLogger().warning("请切换至 Paper 以及衍生服务端");
            getLogger().warning("==========================================");

            instance = null;
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        // 启动阶段计时（冷路径画像，单次汇总日志）
        final long t0 = System.nanoTime();
        this.configManager = new ConfigManager();
        final long tConfig = System.nanoTime();
        this.storiesManager = new StoriesManager();
        final long tStories = System.nanoTime();
        this.listenerManager = new ListenerManager();
        this.runnableManager = new RunnableManager();
        this.spellMemory = new SpellMemory();
        this.supportedPluginManager = new SupportedPluginManager();
        final long tManagers = System.nanoTime();

        configManager.loadConfig();

        SpellType.setupEnabledSpells();
        final long tLoad = System.nanoTime();

        setupSlimefun();
        final long tItems = System.nanoTime();

        setupCommands();
        final long tDone = System.nanoTime();

        getLogger().info(() -> String.format(
            "启动阶段耗时: 配置加载 %.1fms | 故事域构建 %.1fms | 管理器 %.1fms | "
                + "法术配置 %.1fms | 物品注册 %.1fms | 命令 %.2fms | 合计 %.1fms",
            (tConfig - t0) / 1e6, (tStories - tConfig) / 1e6, (tManagers - tStories) / 1e6,
            (tLoad - tManagers) / 1e6, (tItems - tLoad) / 1e6, (tDone - tItems) / 1e6,
            (tDone - t0) / 1e6));
    }

    private void setupCommands() {
        final PluginCommand command = getCommand("crystamaehistoria");
        if (command != null) {
            this.historiaCommand = new HistoriaCommand(command);
            this.historiaCommand.addSub(new TestSpell());
            this.historiaCommand.addSub(new TestWand());
            this.historiaCommand.addSub(new OpenSpellCompendium());
            this.historiaCommand.addSub(new OpenStoryCompendium());
            this.historiaCommand.addSub(new GetRanks());
        }
    }

    @Override
    public void onDisable() {
        if (instance != null) {
            for (ChroniclerPanelCache cache : ChroniclerPanel.getCaches().values()) {
                cache.shutdown();
            }

            spellMemory.clearAll();
            // 关服最终冲刷：无条件落盘
            configManager.saveAll(true);
            instance = null;
        }
    }

    private void setupSlimefun() {
        ItemGroups.setup();
        Materials.setup();
        Mechanisms.setup();
        Tools.setup();
        Gadgets.setup();
        ArtisticItems.setup();
        Exalted.setup();
        Uniques.setup();
        Runes.setup();
        if (supportedPluginManager.isNetheopoiesis()) {
            try {
                NetheoPlants.setup();
            } catch (NoClassDefFoundError e) {
                getLogger().severe("你必须更新下界乌托邦才能让魔法水晶编年史添加相关功能.");
            }
        }
        // 全部物品注册完成后再收集增强合成台配方：类加载时点快照会漏掉
        // 后续注册段（Gadgets/Artistic/Exalted/Uniques/Runes/Netheo）的配方
        EphemeralWorkBench.setupRecipes();
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nullable
    @Override
    public String getBugTrackerURL() {
        return "https://github.com/Sefiraat/CrystamaeHistoria/issues";
    }
}
