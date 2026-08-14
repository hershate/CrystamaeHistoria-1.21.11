package io.github.sefiraat.crystamaehistoria.slimefun;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.DummyGuideOnly;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.DummyItemGroup;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.GildedCollectionFlexGroup;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.MainFlexGroup;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.SpellCollectionFlexGroup;
import io.github.sefiraat.crystamaehistoria.slimefun.itemgroups.StoryCollectionFlexGroup;
import io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.MagicPaintbrush;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.Skulls;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import lombok.experimental.UtilityClass;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@UtilityClass
public final class ItemGroups {

    public static final DummyItemGroup DUMMY_ITEM_GROUP = new DummyItemGroup(
        Keys.newKey("dummy"),
        CustomItemStack.create(
            new ItemStack(Material.FIRE_CHARGE),
            ThemeType.MAIN.getColor() + "魔法水晶编年史 - 占位符 - 不要使用这里的物品"
        )
    );
    public static final MainFlexGroup MAIN = new MainFlexGroup(
        Keys.newKey("main"),
        CustomItemStack.create(
            new ItemStack(Material.AMETHYST_CLUSTER),
            ThemeType.MAIN.getColor() + "魔法水晶编年史"
        )
    );
    public static final DummyItemGroup MECHANISMS = new DummyItemGroup(
        Keys.newKey("mechanisms"),
        CustomItemStack.create(
            new ItemStack(Material.DEEPSLATE_TILE_SLAB),
            ThemeType.MAIN.getColor() + "装置"
        )
    );
    public static final DummyItemGroup CRYSTALS = new DummyItemGroup(
        Keys.newKey("crystals"),
        CustomItemStack.create(
            new ItemStack(Material.AMETHYST_CLUSTER),
            ThemeType.MAIN.getColor() + "魔法水晶"
        )
    );
    public static final DummyItemGroup TOOLS = new DummyItemGroup(
        Keys.newKey("tools"),
        CustomItemStack.create(
            new ItemStack(Material.STICK),
            ThemeType.MAIN.getColor() + "法杖与工具"
        )
    );
    public static final DummyItemGroup ARTISTIC = new DummyItemGroup(
        Keys.newKey("art"),
        CustomItemStack.create(
            MagicPaintbrush.getTippedBrush(DyeColor.WHITE, true),
            ThemeType.MAIN.getColor() + "魔法与艺术"
        )
    );
    public static final DummyItemGroup GADGETS = new DummyItemGroup(
        Keys.newKey("gadgets"),
        CustomItemStack.create(
            new ItemStack(Material.REDSTONE_LAMP),
            ThemeType.MAIN.getColor() + "魔法与科技"
        )
    );
    public static final DummyItemGroup EXALTED = new DummyItemGroup(
        Keys.newKey("exalted"),
        CustomItemStack.create(
            new ItemStack(Material.BEACON),
            ThemeType.MAIN.getColor() + "尊贵物品"
        )
    );
    public static final DummyItemGroup MATERIALS = new DummyItemGroup(
        Keys.newKey("materials"),
        CustomItemStack.create(
            new ItemStack(Material.GOLD_INGOT),
            ThemeType.MAIN.getColor() + "原材料"
        )
    );
    public static final DummyItemGroup RUNES = new DummyItemGroup(
        Keys.newKey("runes"),
        CustomItemStack.create(
            new ItemStack(Material.ENCHANTING_TABLE),
            ThemeType.MAIN.getColor() + "神秘符文"
        )
    );
    public static final DummyItemGroup UNIQUES = new DummyItemGroup(
        Keys.newKey("uniques"),
        CustomItemStack.create(
            new ItemStack(Material.NETHER_STAR),
            ThemeType.MAIN.getColor() + "独特物品"
        )
    );
    public static final DummyItemGroup GUIDE = new DummyItemGroup(
        Keys.newKey("guide"),
        CustomItemStack.create(
            new ItemStack(Material.KNOWLEDGE_BOOK),
            ThemeType.MAIN.getColor() + "指南/教程"
        )
    );
    public static final StoryCollectionFlexGroup STORY_COLLECTION = new StoryCollectionFlexGroup(
        Keys.newKey("story_collection"),
        CustomItemStack.create(
            new ItemStack(Material.KNOWLEDGE_BOOK),
            ThemeType.MAIN.getColor() + "故事集"
        )
    );
    public static final SpellCollectionFlexGroup SPELL_COLLECTION = new SpellCollectionFlexGroup(
        Keys.newKey("spell_collection"),
        CustomItemStack.create(
            new ItemStack(Material.KNOWLEDGE_BOOK),
            ThemeType.MAIN.getColor() + "法术集"
        )
    );
    public static final GildedCollectionFlexGroup GILDING_COLLECTION = new GildedCollectionFlexGroup(
        Keys.newKey("gilding_collection"),
        CustomItemStack.create(
            new ItemStack(Material.KNOWLEDGE_BOOK),
            ThemeType.MAIN.getColor() + "镀金集"
        )
    );


    public static void setup() {
        final CrystamaeHistoria plugin = CrystamaeHistoria.getInstance();

        /*
        These items are for the misc guide which serves to inform players
        how the more abstract mechanics work where a true recipe doesn't
        suit or would be detrimental to the process.
         */

        // Chronicler
        SlimefunItem guideChronicler = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_CHRONICLER",
                new ItemStack(Material.DEEPSLATE_TILE_SLAB),
                ThemeType.GUIDE,
                "指南: 记录者",
                "你可以在记录者中放置一个原版物品",
                "记录者会\"发掘\"物品中含有的故事",
                "故事的类型取决于物品的类型",
                "而故事数量则是随机的，但与物品级别有关",
                "",
                "所有可发掘的物品可以在\"故事集\"分类中查看",
                "",
                "当记录者完成发掘后，记录者上方的悬浮物品会消失"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Realisation
        SlimefunItem guideRealisation = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_REALISATION",
                new ItemStack(Material.CHISELED_DEEPSLATE),
                ThemeType.GUIDE,
                "指南: 现实祭坛",
                "将一个有故事的物品放入现实祭坛中",
                "祭坛会开始从该物品中提取魔法能量",
                "这会在周围生成紫水晶(必须同一高度)",
                "",
                "魔法水晶簇会缓慢生长，直到魔法能量被完全提取出来",
                "这时你可以破坏魔法水晶簇来获取魔法水晶"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Liquefaction
        SlimefunItem guideLiquefaction = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_LIQUEFACTION",
                new ItemStack(Material.CAULDRON),
                ThemeType.GUIDE,
                "指南: 液化池",
                "你可以使用液化池做出一些很酷的东西",
                "",
                "任何扔进液化池中的魔法水晶会转为液态",
                "当你扔进其他物品时",
                "液化池会尝试使用该物品进行合成",
                "",
                "如果你丢入错误的物品，你会失去所有魔法水晶"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Stave Configurator
        SlimefunItem guideStave = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_STAVE_CONFIGURATOR",
                new ItemStack(Material.CAULDRON),
                ThemeType.GUIDE,
                "指南: 法杖配置器",
                "将法杖放置在输入栏",
                "把充能的法术板放入法术栏",
                "点击'保存法杖设置'来将法术绑定至法杖上",
                "点击'移除法术板'来将法术板从法杖中取出",
                "",
                "你可以在任何时间随意调整法杖上的法术"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Make a Spell
        SlimefunItem guideMakeSpell = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_MAKE_SPELL",
                new ItemStack(Material.PAPER),
                ThemeType.GUIDE,
                "指南: 制作法术",
                "要制作一个法术，你需要先制作一块基础魔法板。",
                "然后在液化池中，放入足够数量的三种不同类型的魔法水晶",
                "最后将魔法板投入液化池即可"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Recharge a Spell
        SlimefunItem guideChargeSpell = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_CHARGE_SPELL",
                new ItemStack(Material.PAPER),
                ThemeType.GUIDE,
                "指南: 充能法术",
                "要为法术重新充能，你需要先",
                "使用法杖配置器将法术板从法杖中取出",
                "然后在液化池中，投入制作该法术所使用的三种魔法水晶",
                "最后投入法术板，即可重新充能"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Nether Draining
        SlimefunItem guideNetherDraining = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_NETHER_DRAINING",
                Skulls.CRYSTAL_CLEAR.getPlayerHead(),
                ThemeType.GUIDE,
                "指南: 下界祛魔",
                "当神秘魔法水晶穿过下界传送门时",
                "其中包含的魔法将会丢失变成空白水晶",
                "可以重新注入魔法能量"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Prismatic Gilding
        SlimefunItem guideGilding = new SlimefunItem(
            ItemGroups.GUIDE,
            ThemeType.themedSlimefunItemStack(
                "CRY_GUIDE_GILDING",
                new ItemStack(Material.WARPED_FENCE),
                ThemeType.GUIDE,
                "指南: 镀金",
                "向棱镜镀金器投掷棱镜水晶",
                "可以将棱镜水晶转化为魔法形态",
                "此时，你可以手持已经完全发掘故事的方块",
                "右键点击棱镜镀金器即可将方块镀金",
                "需要的棱镜水晶数量与方块等级一致",
                "镀金的魔法水晶簇需要手动破坏"
            ),
            DummyGuideOnly.TYPE,
            new ItemStack[]{}
        );

        // Slimefun Registry
        ItemGroups.MAIN.register(plugin);

        guideChronicler.register(plugin);
        guideRealisation.register(plugin);
        guideLiquefaction.register(plugin);
        guideStave.register(plugin);
        guideMakeSpell.register(plugin);
        guideChargeSpell.register(plugin);
        guideNetherDraining.register(plugin);
        guideGilding.register(plugin);
    }
}
