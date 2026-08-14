package io.github.sefiraat.crystamaehistoria.utils.theme;

import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.Getter;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public enum ThemeType {
    WARNING(ChatColor.YELLOW, "警告"),
    ERROR(ChatColor.RED, "错误"),
    NOTICE(ChatColor.WHITE, "通知"),
    PASSIVE(ChatColor.GRAY, ""),
    SUCCESS(ChatColor.GREEN, "成功"),
    MAIN(ChatColor.of("#21588f"), "魔法水晶编年史"),
    CLICK_INFO(ChatColor.of("#e4ed32"), "单击此处"),
    RESEARCH(ChatColor.of("#a60e03"), "研究"),
    CRAFTING(ChatColor.of("#dbcea9"), "合成材料"),
    CRYSTAL(ChatColor.of("#dbcea9"), "魔法水晶"),
    MACHINE(ChatColor.of("#3295a8"), "机器"),
    MECHANISM(ChatColor.of("#3295a8"), "装置"),
    GADGET(ChatColor.of("#8732a8"), "道具"),
    EXALTED(ChatColor.of("#8732a8"), "尊贵物品"),
    GUIDE(ChatColor.of("#444444"), "指南"),
    CHEST(ChatColor.of("#b89b1c"), "箱子"),
    DROP(ChatColor.of("#bf307f"), "稀有掉落物"),
    BASE(ChatColor.of("#9e9e9e"), "基础资源"),
    MOLTEN_METAL(ChatColor.of("#21588f"), "熔融金属"),
    LIQUID(ChatColor.of("#65dbb4"), "液体"),
    CAST(ChatColor.of("#ffe138"), "模具"),
    PART(ChatColor.of("#42c8f5"), "部件"),
    TOOL(ChatColor.of("#c2fc03"), "工具"),
    STAVE(ChatColor.of("#c2fc03"), "法杖"),
    ARMOUR(ChatColor.of("#c2fc03"), "防具"),
    INFO(ChatColor.of("#21588f"), "信息"),
    MOD(ChatColor.of("#bf307f"), "模组"),
    PROP(ChatColor.of("#bf307f"), "特性"),
    SPELL(ChatColor.of("#bf307f"), "法术"),
    RUNE(ChatColor.of("#32a852"), "符文"),
    MULTIBLOCK(ChatColor.of("#ba12af"), "多方块结构"),
    RARITY_COMMON(ChatColor.of("#dbdbdb"), "普通"),
    RARITY_UNCOMMON(ChatColor.of("#97d16b"), "罕见"),
    RARITY_RARE(ChatColor.of("#d1db5c"), "稀有"),
    RARITY_EPIC(ChatColor.of("#b355d9"), "史诗"),
    RARITY_MYTHICAL(ChatColor.of("#c42336"), "神秘"),
    RARITY_UNIQUE(ChatColor.of("#b35f12"), "独特"),
    TYPE_ELEMENTAL(ChatColor.of("#ba0000"), "元素"),
    TYPE_MECHANICAL(ChatColor.of("#ba5d00"), "机械"),
    TYPE_ALCHEMICAL(ChatColor.of("#e5e81a"), "炼金"),
    TYPE_HISTORICAL(ChatColor.of("#24e81a"), "历史"),
    TYPE_HUMAN(ChatColor.of("#201ae8"), "人类"),
    TYPE_ANIMAL(ChatColor.of("#701ae8"), "动物"),
    TYPE_CELESTIAL(ChatColor.of("#ffffff"), "天体"),
    TYPE_VOID(ChatColor.of("#222222"), "虚空"),
    TYPE_PHILOSOPHICAL(ChatColor.of("#4d4aa8"), "哲学"),
    RANK_SPELL_APPRENTICE(ChatColor.of("#cdbfff"), "学徒"),
    RANK_SPELL_MAGE(ChatColor.of("#b5a1ff"), "法师"),
    RANK_SPELL_WIZARD(ChatColor.of("#9d82ff"), "巫师"),
    RANK_SPELL_CONJURER(ChatColor.of("#8969ff"), "咒术师"),
    RANK_SPELL_SORCERER(ChatColor.of("#6f47ff"), "术士"),
    RANK_SPELL_MAGI(ChatColor.of("#5729ff"), "魔法师"),
    RANK_SPELL_MASTER_MAGI(ChatColor.of("#3d08ff"), "大师级魔法师"),
    RANK_SPELL_GRANDMASTER_MAGI(ChatColor.of("#6b08ff"), "宗师级魔法师"),
    RANK_STORY_PUPIL(ChatColor.of("#eeffa8"), "小学生"),
    RANK_STORY_STUDENT(ChatColor.of("#e7ff82"), "学生"),
    RANK_STORY_RESEARCHER(ChatColor.of("#e0ff5e"), "研究者"),
    RANK_STORY_READER(ChatColor.of("#d8ff33"), "读者"),
    RANK_STORY_LECTURER(ChatColor.of("#ceff00"), "讲师"),
    RANK_STORY_PROFESSOR(ChatColor.of("#99ff00"), "教授"),
    RANK_STORY_ADJUNCT_PROFESSOR(ChatColor.of("#6aff00"), "兼任教授"),
    RANK_STORY_EMERITUS_PROFESSOR(ChatColor.of("#33ff00"), "荣誉教授"),
    RANK_BLOCK_UNKNOWN(ChatColor.of("#a8ffb1"), "未知"),
    RANK_BLOCK_HEARD_OF(ChatColor.of("#87ff94"), "略有耳闻"),
    RANK_BLOCK_KNOWN(ChatColor.of("#66ff77"), "已知"),
    RANK_BLOCK_DETAILED(ChatColor.of("#4dff60"), "详细"),
    RANK_BLOCK_RESEARCHED(ChatColor.of("#29ff40"), "研究过"),
    RANK_BLOCK_EXPERT_OF(ChatColor.of("#0fff29"), "专家"),
    RANK_BLOCK_MASTER_OF(ChatColor.of("#00db18"), "大师"),
    RANK_BLOCK_SME(ChatColor.of("#00820e"), "历史见证者"),
    RANK_GILDING_NOVICE(ChatColor.of("#a8ffb1"), "新手"),
    RANK_GILDING_MEMBER(ChatColor.of("#87ff94"), "员工"),
    RANK_GILDING_SECRETARY(ChatColor.of("#66ff77"), "秘书"),
    RANK_GILDING_OFFICER(ChatColor.of("#4dff60"), "主管"),
    RANK_GILDING_EXECUTIVE(ChatColor.of("#29ff40"), "执行官"),
    RANK_GILDING_CHIEF(ChatColor.of("#0fff29"), "首席执行官"),
    RANK_GILDING_MANAGER(ChatColor.of("#00db18"), "经理"),
    RANK_GILDING_OWNER(ChatColor.of("#00820e"), "老板");

    /**
     * List of names to be given to ArmourStands, invisible but mods and Minimaps can see them :)
     */
    @Nonnull
    private static final List<String> EGG_NAMES = Arrays.asList(
        "TheBusyBiscuit",
        "Alessio",
        "Walshy",
        "Jeff",
        "Seggan",
        "BOOMER_1",
        "svr333",
        "variananora",
        "ProfElements",
        "Riley",
        "FluffyBear",
        "GallowsDove",
        "Apeiros",
        "Martin",
        "Bunnky",
        "ReasonFoundDecoy",
        "Oah",
        "Azak",
        "andrewandy",
        "EpicPlayer10",
        "GentlemanCheesy",
        "ybw0014",
        "Ashian",
        "R.I.P",
        "OOOOMAGAAA",
        "TerslenK",
        "FN_FAL",
        "supertechxter"
    );

    @Getter
    private static final ThemeType[] cachedValues = values();
    private final ChatColor color;
    private final String loreLine;

    ThemeType(ChatColor color, String loreLine) {
        this.color = color;
        this.loreLine = loreLine;

    }

    /**
     * Gets a SlimefunItemStack with a pre-populated lore and name with themed colors.
     *
     * @param id        The ID for the new {@link SlimefunItemStack}
     * @param itemStack The vanilla {@link ItemStack} used to base the {@link SlimefunItemStack} on
     * @param themeType The {@link ThemeType} {@link ChatColor} to apply to the {@link SlimefunItemStack} name
     * @param name      The name to apply to the {@link SlimefunItemStack}
     * @param lore      The lore lines for the {@link SlimefunItemStack}. Lore is book-ended with empty strings.
     * @return Returns the new {@link SlimefunItemStack}
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static SlimefunItemStack themedSlimefunItemStack(String id,
                                                            ItemStack itemStack,
                                                            ThemeType themeType,
                                                            String name,
                                                            String... lore
    ) {
        ChatColor passiveColor = ThemeType.PASSIVE.getColor();
        List<String> finalLore = new ArrayList<>();
        finalLore.add("");
        for (String s : lore) {
            finalLore.add(passiveColor + s);
        }
        finalLore.add("");
        finalLore.add(applyThemeToString(ThemeType.CLICK_INFO, themeType.getLoreLine()));
        return new SlimefunItemStack(
            id,
            itemStack,
            ThemeType.applyThemeToString(themeType, name),
            finalLore.toArray(new String[finalLore.size() - 1])
        );
    }

    /**
     * Applies the theme color to a given string
     *
     * @param themeType The {@link ThemeType} to apply the color from
     * @param string    The string to apply the color to
     * @return Returns the string provides preceded by the color
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static String applyThemeToString(ThemeType themeType, String string) {
        return themeType.getColor() + string;
    }

    /**
     * Gets an ItemStack with a pre-populated lore and name with themed colors.
     *
     * @param material  The {@link Material} used to base the {@link ItemStack} on
     * @param themeType The {@link ThemeType} {@link ChatColor} to apply to the {@link ItemStack} name
     * @param name      The name to apply to the {@link ItemStack}
     * @param lore      The lore lines for the {@link ItemStack}. Lore is book-ended with empty strings.
     * @return Returns the new {@link ItemStack}
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public static ItemStack themedItemStack(Material material, ThemeType themeType, String name, String... lore) {
        ChatColor passiveColor = ThemeType.PASSIVE.getColor();
        List<String> finalLore = new ArrayList<>();
        finalLore.add("");
        for (String s : lore) {
            finalLore.add(passiveColor + s);
        }
        finalLore.add("");
        finalLore.add(applyThemeToString(ThemeType.CLICK_INFO, themeType.getLoreLine()));
        return CustomItemStack.create(
            material,
            ThemeType.applyThemeToString(themeType, name),
            finalLore.toArray(new String[finalLore.size() - 1])
        );
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static ThemeType getByRarity(StoryRarity storyRarity) {
        switch (storyRarity) {
            case COMMON:
                return RARITY_COMMON;
            case UNCOMMON:
                return RARITY_UNCOMMON;
            case RARE:
                return RARITY_RARE;
            case EPIC:
                return RARITY_EPIC;
            case MYTHICAL:
                return RARITY_MYTHICAL;
            case UNIQUE:
                return RARITY_UNIQUE;
            default:
                throw new IllegalStateException("Unexpected value: " + storyRarity);
        }
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public static ThemeType getByType(StoryType storyType) {
        switch (storyType) {
            case ELEMENTAL:
                return TYPE_ELEMENTAL;
            case MECHANICAL:
                return TYPE_MECHANICAL;
            case ALCHEMICAL:
                return TYPE_ALCHEMICAL;
            case HISTORICAL:
                return TYPE_HISTORICAL;
            case HUMAN:
                return TYPE_HUMAN;
            case ANIMAL:
                return TYPE_ANIMAL;
            case CELESTIAL:
                return TYPE_CELESTIAL;
            case VOID:
                return TYPE_VOID;
            case PHILOSOPHICAL:
                return TYPE_PHILOSOPHICAL;
            default:
                throw new IllegalStateException("Unexpected value: " + storyType);
        }
    }

    @Nonnull
    public static String getRandomEggName() {
        int rnd = ThreadLocalRandom.current().nextInt(0, EGG_NAMES.size());
        return EGG_NAMES.get(rnd);
    }

    @Nonnull
    public static List<String> getEggNames() {
        return EGG_NAMES;
    }

    @Nonnull
    public Particle.DustOptions getDustOptions(float size) {
        return new Particle.DustOptions(
            Color.fromRGB(
                color.getColor().getRed(),
                color.getColor().getGreen(),
                color.getColor().getBlue()
            ),
            size
        );
    }

    @Nonnull
    public TextColor getComponentColor() {
        return TextColor.color(this.getColor().getColor().getRGB());
    }

}
