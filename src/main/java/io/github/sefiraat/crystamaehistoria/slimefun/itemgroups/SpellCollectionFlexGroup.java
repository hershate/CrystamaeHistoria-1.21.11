package io.github.sefiraat.crystamaehistoria.slimefun.itemgroups;

import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCore;
import io.github.sefiraat.crystamaehistoria.player.PlayerStatistics;
import io.github.sefiraat.crystamaehistoria.player.SpellRank;
import io.github.sefiraat.crystamaehistoria.slimefun.ItemGroups;
import io.github.sefiraat.crystamaehistoria.slimefun.Materials;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.theme.GuiElements;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.items.groups.FlexItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import io.github.sefiraat.crystamaehistoria.utils.NameUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @noinspection deprecation
 */
public class SpellCollectionFlexGroup extends FlexItemGroup {

    private static final int PAGE_SIZE = 36;

    private static final int GUIDE_BACK = 1;
    private static final int GUIDE_STATS = 7;

    private static final int PAGE_PREVIOUS = 46;
    private static final int PAGE_NEXT = 52;

    private static final int[] HEADER = new int[]{
        0, 1, 2, 3, 4, 5, 6, 7, 8
    };
    private static final int[] FOOTER = new int[]{
        45, 46, 47, 48, 49, 50, 51, 52, 53
    };

    private static final int SPELL = 19;
    private static final int[] RECIPE = new int[]{
        21, 22, 23
    };
    private static final int MECHANISM = 25;
    private static final int CRYSTA_COST = 37;
    private static final int CAST_TYPE = 38;
    private static final int VALUES = 39;
    private static final int RANGE = 40;
    private static final int KNOCK_BACK = 41;
    private static final int PROJECTILE_INFO = 42;
    private static final int EFFECTS = 43;

    public SpellCollectionFlexGroup(NamespacedKey key, ItemStack item) {
        super(key, item);
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean isVisible(Player player, PlayerProfile playerProfile, SlimefunGuideMode guideMode) {
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void open(Player p, PlayerProfile profile, SlimefunGuideMode mode) {
        final ChestMenu chestMenu = new ChestMenu(ThemeType.MAIN.getColor() + "魔法水晶编年史 - 法术集");

        for (int slot : HEADER) {
            chestMenu.addItem(slot, ChestMenuUtils.getBackground(), (player1, i1, itemStack, clickAction) -> false);
        }

        for (int slot : FOOTER) {
            chestMenu.addItem(slot, ChestMenuUtils.getBackground(), (player1, i1, itemStack, clickAction) -> false);
        }

        chestMenu.setEmptySlotsClickable(false);
        setupPage(p, profile, mode, chestMenu, 1);
        chestMenu.open(p);
    }

    @ParametersAreNonnullByDefault
    private void setupPage(Player player, PlayerProfile profile, SlimefunGuideMode mode, ChestMenu menu, int page) {
        // 启用集已在 setupEnabledSpells 启动时按 id 排序，翻页直接用数组视图
        final List<SpellType> spellTypes = Arrays.asList(SpellType.getEnabledSpells());
        final int numberOfBlocks = spellTypes.size();
        final int totalPages = (int) Math.ceil(numberOfBlocks / (double) PAGE_SIZE);
        final int start = (page - 1) * PAGE_SIZE;
        final int end = Math.min(start + PAGE_SIZE, spellTypes.size());

        final List<SpellType> spellTypeSubList = spellTypes.subList(start, end);

        reapplyFooter(player, profile, mode, menu, page, totalPages);

        // Sound
        menu.addMenuOpeningHandler((p) -> p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F));

        // Back
        menu.replaceExistingItem(
            GUIDE_BACK,
            ChestMenuUtils.getBackButton(
                player,
                "",
                ChatColor.GRAY + Slimefun.getLocalization().getMessage(player, "guide.back.guide")
            )
        );
        menu.addMenuClickHandler(GUIDE_BACK, (player1, slot, itemStack, clickAction) -> {
            SlimefunGuide.openItemGroup(profile, ItemGroups.MAIN, mode, 1);
            return false;
        });

        // Stats
        menu.replaceExistingItem(GUIDE_STATS, getStatsStack(player));
        menu.addMenuClickHandler(GUIDE_STATS, (player1, slot, itemStack, clickAction) -> false);

        // 页级单次解析玩家法术统计子节，36 槽相对路径读取（原每槽从根走全路径）
        final ConfigurationSection spellStatSection = PlayerStatistics.getSpellStatSection(player.getUniqueId());

        for (int i = 0; i < 36; i++) {
            final int slot = i + 9;

            if (i + 1 <= spellTypeSubList.size()) {
                final SpellType spellType = spellTypeSubList.get(i);
                final boolean researched = PlayerStatistics.hasUnlockedSpell(player.getUniqueId(), spellType, spellStatSection);

                if (mode == SlimefunGuideMode.CHEAT_MODE || researched) {
                    menu.replaceExistingItem(slot, spellType.getSpell().getThemedStack().item());
                    menu.addMenuClickHandler(slot, (player1, i1, itemStack1, clickAction) -> {
                        displayDefinition(player1, profile, mode, menu, page, spellType);
                        return false;
                    });
                } else {
                    menu.replaceExistingItem(slot, GuiElements.getSpellNotUnlockedIcon(spellType.getSpell().getName()));
                    menu.addMenuClickHandler(slot, (player1, i1, itemStack1, clickAction) -> false);
                }
            } else {
                menu.replaceExistingItem(slot, null);
                menu.addMenuClickHandler(slot, (player1, i1, itemStack1, clickAction) -> false);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void displayDefinition(Player p, PlayerProfile profile, SlimefunGuideMode mode, ChestMenu menu, int returnPage, SpellType spellType) {
        final Spell spell = spellType.getSpell();
        // Sound
        menu.addMenuOpeningHandler((player) -> player.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0F, 1.0F));

        // Back
        menu.replaceExistingItem(
            GUIDE_BACK,
            ChestMenuUtils.getBackButton(
                p,
                "",
                ChatColor.GRAY + Slimefun.getLocalization().getMessage(p, "guide.back.guide")
            )
        );
        menu.addMenuClickHandler(GUIDE_BACK, (player1, slot, itemStack, clickAction) -> {
            setupPage(player1, profile, mode, menu, returnPage);
            return false;
        });

        clearDisplay(menu);

        menu.replaceExistingItem(SPELL, spellType.getSpell().getThemedStack().item());
        menu.addMenuClickHandler(SPELL, ((player, i, itemStack, clickAction) -> false));

        for (int i = 0; i < RECIPE.length; i++) {
            int slot = RECIPE[i];
            StoryType storyType = spellType.getSpell().getRecipe().getInput(i);
            ItemStack stack = Materials.getDummyCrystalMap().get(storyType).getItem();
            menu.replaceExistingItem(slot, stack);
            menu.addMenuClickHandler(slot, ((player, slot2, itemStack, clickAction) -> false));
        }

        menu.replaceExistingItem(MECHANISM, MECHANISM_STACK);
        menu.addMenuClickHandler(MECHANISM, ((player, i, itemStack, clickAction) -> false));

        // 详情 7 堆为不可变 SpellCore 的纯函数，按法术记忆化，clone 保持每格独立实例
        final ItemStack[] detail = DETAIL_STACKS.computeIfAbsent(
            spellType,
            st -> buildDetailStacks(st.getSpell())
        );
        menu.replaceExistingItem(CRYSTA_COST, detail[0].clone());
        menu.addMenuClickHandler(CRYSTA_COST, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(VALUES, detail[1].clone());
        menu.addMenuClickHandler(VALUES, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(CAST_TYPE, detail[2].clone());
        menu.addMenuClickHandler(CAST_TYPE, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(RANGE, detail[3].clone());
        menu.addMenuClickHandler(RANGE, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(KNOCK_BACK, detail[4].clone());
        menu.addMenuClickHandler(KNOCK_BACK, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(PROJECTILE_INFO, detail[5].clone());
        menu.addMenuClickHandler(PROJECTILE_INFO, ((player, i, itemStack, clickAction) -> false));

        menu.replaceExistingItem(EFFECTS, detail[6].clone());
        menu.addMenuClickHandler(EFFECTS, ((player, i, itemStack, clickAction) -> false));

    }

    /** 详情堆缓存：[基本信息, 法术属性, 施法类型, 范围, 击退, 弹射物, 药水效果] */
    private static final java.util.EnumMap<SpellType, ItemStack[]> DETAIL_STACKS = new java.util.EnumMap<>(SpellType.class);

    /** 机制说明堆与法术无关，全常量，启动后共享单实例（展示只读） */
    private static final ItemStack MECHANISM_STACK = buildMechanismStack();

    @Nonnull
    private ItemStack[] buildDetailStacks(Spell spell) {
        return new ItemStack[]{
            getBasicStack(spell),
            getValuesStack(spell),
            getCastTypeStack(spell),
            getRangeStack(spell),
            getKnockBackStack(spell),
            getProjectileStack(spell),
            getEffectsStack(spell)
        };
    }

    @Nonnull
    private static ItemStack buildMechanismStack() {
        final List<String> lore = Arrays.stream(
                new String[]{
                    "在液化池中投入魔法水晶",
                    "将其转化为液化魔法水晶",
                    "",
                    "数量最多的3种液化魔法水晶决定法术的类型",
                    "",
                    "投入一个魔法板来制作法术"
                }
            ).map(s -> ThemeType.PASSIVE.getColor() + s)
            .collect(Collectors.toList());

        return CustomItemStack.create(
            Material.CAULDRON,
            ThemeType.MAIN.getColor() + "液化池",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private void clearDisplay(ChestMenu menu) {
        for (int i = 0; i < 45; i++) {
            final int slot = i + 9;
            menu.replaceExistingItem(slot, null);
            menu.addMenuClickHandler(slot, (player1, i1, itemStack1, clickAction) -> false);
        }
    }

    @ParametersAreNonnullByDefault
    private void reapplyFooter(Player p, PlayerProfile profile, SlimefunGuideMode mode, ChestMenu menu, int page, int totalPages) {
        for (int slot : FOOTER) {
            menu.replaceExistingItem(slot, ChestMenuUtils.getBackground());
            menu.addMenuClickHandler(slot, ((player, i, itemStack, clickAction) -> false));
        }

        menu.replaceExistingItem(PAGE_PREVIOUS, ChestMenuUtils.getPreviousButton(p, page, totalPages));
        menu.addMenuClickHandler(PAGE_PREVIOUS, (player1, slot, itemStack, clickAction) -> {
            final int previousPage = page - 1;
            if (previousPage >= 1) {
                setupPage(player1, profile, mode, menu, previousPage);
            }
            return false;
        });

        menu.replaceExistingItem(PAGE_NEXT, ChestMenuUtils.getNextButton(p, page, totalPages));
        menu.addMenuClickHandler(PAGE_NEXT, (player1, slot, itemStack, clickAction) -> {
            final int nextPage = page + 1;
            if (nextPage <= totalPages) {
                setupPage(player1, profile, mode, menu, nextPage);
            }
            return false;
        });
    }

    @ParametersAreNonnullByDefault
    private ItemStack getBasicStack(Spell spell) {
        final SpellCore spellCore = spell.getSpellCore();
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();

        final String crysta = MessageFormat.format("{0}每次施法消耗充能: {1}{2}", color, passive, spellCore.getCrystaCost());
        final String crystaMulti = MessageFormat.format("{0}施法消耗{1}随着法杖等级提升而增加", color, spellCore.isCrystaMultiplied() ? "会" : "不会");
        final String cooldown = MessageFormat.format("{0}冷却时间(秒): {1}{2}", color, passive, spell.getSpellCore().getCooldownSeconds());
        final String cooldownDivided = MessageFormat.format("{0}冷却时间{1}随着法杖等级提升而减少", color, spellCore.isCooldownDivided() ? "会" : "不会");


        return CustomItemStack.create(
            Material.GLOW_BERRIES,
            ThemeType.MAIN.getColor() + "基本信息",
            crysta,
            crystaMulti,
            cooldown,
            cooldownDivided
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getValuesStack(Spell spell) {
        final SpellCore spellCore = spell.getSpellCore();
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final List<String> lore = new ArrayList<>();

        final String damageMessage = MessageFormat.format("{0}伤害: {1}{2}", color, passive, spellCore.getDamageAmount());
        final String damageMulti = MessageFormat.format("{0}伤害{1}随着法杖等级提升而增加", color, spellCore.isDamageMultiplied() ? "会" : "不会");

        final String healMessage = MessageFormat.format("{0}治疗量: {1}{2}", color, passive, spellCore.getHealAmount());
        final String healMulti = MessageFormat.format("{0}治疗量{1}随着法杖等级提升而增加", color, spellCore.isHealMultiplied() ? "会" : "不会");

        if (spellCore.isDamagingSpell()) {
            lore.add(damageMessage);
            lore.add(damageMulti);
        } else {
            lore.add(passive + "该法术不会造成伤害.");
        }

        if (spellCore.isHealingSpell()) {
            lore.add(healMessage);
            lore.add(healMulti);
        } else {
            lore.add(passive + "该法术不会进行治疗.");
        }

        return CustomItemStack.create(
            Material.MAP,
            ThemeType.MAIN.getColor() + "法术属性",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getCastTypeStack(Spell spell) {
        final SpellCore spellCore = spell.getSpellCore();
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final ChatColor notice = ThemeType.NOTICE.getColor();
        final List<String> lore = new ArrayList<>();

        final int ticks = spellCore.getNumberOfTicks();

        final String instantCast = MessageFormat.format("{0}瞬间: {1}施法后立即生效", color, passive);
        final String damagingSpell = MessageFormat.format("{0}伤害: {1}会造成伤害、附带负面效果", color, passive);
        final String healingSpell = MessageFormat.format("{0}治疗: {1}会进行治疗、附带正面效果", color, passive);
        final String effectingSpell = MessageFormat.format("{0}特效: {1}会附带药水效果", color, passive);
        final String tickingSpell1 = MessageFormat.format("{0}多重: {1}该法术会释放 {2}{3}{4} 次", color, passive, notice, ticks, passive);
        final String tickingSpell2 = MessageFormat.format("{0}施法次数{1}随着法杖等级提升而增加", passive, spellCore.isNumberOfTicksMultiplied() ? "会" : "不会");

        final String projectileSpell1 = MessageFormat.format("{0}弹射物: {1}该法术会发射弹射物", color, passive);
        final String projectileSpell2 = MessageFormat.format("{0}命中后会附带后续效果", passive);

        if (spellCore.isInstantCast()) {
            lore.add(instantCast);
        }
        if (spellCore.isDamagingSpell()) {
            lore.add(damagingSpell);
        }
        if (spellCore.isHealingSpell()) {
            lore.add(healingSpell);
        }
        if (spellCore.isEffectingSpell()) {
            lore.add(effectingSpell);
        }
        if (spellCore.isTickingSpell()) {
            lore.add(tickingSpell1);
            lore.add(tickingSpell2);
        }
        if (spellCore.isProjectileSpell()) {
            lore.add(projectileSpell1);
            lore.add(projectileSpell2);
        }
        return CustomItemStack.create(
            Material.NAME_TAG,
            ThemeType.MAIN.getColor() + "施法类型",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getRangeStack(Spell spell) {
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final List<String> lore = new ArrayList<>();

        final String message = MessageFormat.format("{0}范围: {1}{2}", color, passive, spell.getSpellCore().getRange());
        final String multiMessage = MessageFormat.format("{0}范围{1}随着法杖等级提升而增加", passive, spell.getSpellCore().isRangeMultiplied() ? "会" : "不会");
        final String noRange = passive + "不会受到范围的影响";

        if (spell.getSpellCore().getRange() > 0) {
            lore.add(message);
            lore.add(multiMessage);
        } else {
            lore.add(noRange);
        }

        return CustomItemStack.create(
            Material.TARGET,
            ThemeType.MAIN.getColor() + "范围",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getKnockBackStack(Spell spell) {
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final List<String> lore = new ArrayList<>();

        final String message = MessageFormat.format("{0}击退: {1}{2}", color, passive, spell.getSpellCore().getKnockbackAmount());
        final String multiMessage = MessageFormat.format("{0}击退等级{1}随着法杖等级提升而增加", passive, spell.getSpellCore().isKnockbackMultiplied() ? "会" : "不会");
        final String noKnockback = passive + "没有击退效果";

        if (spell.getSpellCore().getKnockbackAmount() > 0) {
            lore.add(message);
            lore.add(multiMessage);
        } else {
            lore.add(noKnockback);
        }

        return CustomItemStack.create(
            Material.SLIME_BLOCK,
            ThemeType.MAIN.getColor() + "击退",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getProjectileStack(Spell spell) {
        final SpellCore spellCore = spell.getSpellCore();
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();

        final String aoeMessage = MessageFormat.format("{0}弹射物溅射范围: {1}{2}", color, passive, spellCore.getProjectileAoeRange());
        final String aoeMultiMessage = MessageFormat.format("{0}溅射范围{1}随着法杖等级提升而增加", passive, spellCore.isProjectileAoeMultiplied() ? "会" : "不会");
        final String knockbackMessage = MessageFormat.format("{0}弹射物击退: {1}{2}", color, passive, spellCore.getProjectileKnockbackAmount());
        final String knockbackMultiMessage = MessageFormat.format("{0}击退等级{1}随着法杖等级提升而增加", passive, spellCore.isProjectileKnockbackMultiplied() ? "会" : "不会");

        final List<String> lore = new ArrayList<>();

        if (spellCore.isProjectileSpell()) {
            lore.add(aoeMessage);
            lore.add(aoeMultiMessage);
            lore.add("");
            lore.add(knockbackMessage);
            lore.add(knockbackMultiMessage);
        } else {
            lore.add(passive + "不是弹射物类型的法术");
        }

        return CustomItemStack.create(
            Material.FIRE_CHARGE,
            ThemeType.MAIN.getColor() + "弹射物信息",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getEffectsStack(Spell spell) {
        final SpellCore spellCore = spell.getSpellCore();
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final List<String> lore = new ArrayList<>();

        if (spellCore.isEffectingSpell()) {
            final String effectAmplification = MessageFormat.format(
                "{0}药水效果等级{1}随着法杖等级提升而增加",
                passive,
                spellCore.isAmplificationMultiplied() ? "会" : "不会"
            );
            final String effectDuration = MessageFormat.format(
                "{0}药水效果持续时间{1}随着法杖等级提升而增加",
                passive,
                spellCore.isEffectDurationMultiplied() ? "会" : "不会"
            );

            if (spellCore.getNegativeEffectPairMap().size() > 0) {
                lore.add(color + "负面效果:");
                spellCore.getNegativeEffectPairMap().forEach(
                    (type, pair) -> {
                        final String negativeEffectMessage = MessageFormat.format(
                            "{0}{1}: {2}等级 ({3}) - 持续时间 ({4})",
                            color,
                            NameUtils.getPotionEffectTypeName(type),
                            passive,
                            pair.getFirstValue(),
                            pair.getSecondValue()
                        );
                        lore.add(negativeEffectMessage);
                    }
                );
            }

            if (spellCore.getPositiveEffectPairMap().size() > 0) {
                lore.add(color + "正面效果:");
                spellCore.getPositiveEffectPairMap().forEach(
                    (type, pair) -> {
                        final String positiveEffectMessage = MessageFormat.format(
                            "{0}{1}: {2}等级 ({3}) - 持续时间 ({4})",
                            color,
                            NameUtils.getPotionEffectTypeName(type),
                            passive,
                            pair.getFirstValue(),
                            pair.getSecondValue()
                        );
                        lore.add(positiveEffectMessage);
                    }
                );
            }

            lore.add("");
            lore.add(effectAmplification);
            lore.add(effectDuration);
        } else {
            lore.add(passive + "没有药水效果");
        }

        return CustomItemStack.create(
            Material.BREWING_STAND,
            ThemeType.MAIN.getColor() + "药水效果",
            lore
        );
    }

    @ParametersAreNonnullByDefault
    private ItemStack getStatsStack(Player player) {
        final ChatColor color = ThemeType.CLICK_INFO.getColor();
        final ChatColor passive = ThemeType.PASSIVE.getColor();
        final List<String> lore = new ArrayList<>();
        final SpellRank spellRank = PlayerStatistics.getSpellRank(player.getUniqueId());

        lore.add(MessageFormat.format("{0}已解锁法术: {1}{2}", color, passive, PlayerStatistics.getSpellsUnlocked(player.getUniqueId())));
        lore.add(MessageFormat.format("{0}等级: {1}{2}", color, spellRank.getTheme().getColor(), spellRank.getTheme().getLoreLine()));

        return CustomItemStack.create(
            Material.TARGET,
            ThemeType.MAIN.getColor() + "法术统计",
            lore
        );
    }
}
