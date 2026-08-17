package io.github.sefiraat.crystamaehistoria.utils;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import lombok.Data;
import org.bukkit.NamespacedKey;

import javax.annotation.Nonnull;

@Data
public final class Keys {

    // BlockStorage
    public static final String BS_CP_WORKING_ON = "BS_CP_WORKING_ON";
    public static final String BS_CP_ACTIVE_PLAYER = "BS_CP_ACTIVE_PLAYER";
    public static final String BS_CP_STORIED = "BS_CP_STORIED";
    public static final String BS_CP_STORIES = "BS_CP_STORIES";

    // PDC Json
    public static final String JS_S_AVAILABLE_STORIES = "JS_S_AS";
    public static final String JS_S_TIER = "JS_S_T";

    // Misc
    public static final String PANEL_STAND_PREFIX = "CH_PANEL_";

    // Recipe Types
    public static final NamespacedKey GUIDE_ONLY = newKey("guide");
    public static final NamespacedKey GUIDE_MAKE_SPELL = newKey("guide_make_spell");
    public static final NamespacedKey GUIDE_RECHARGE_SPELL = newKey("guide_recharge_spell");
    public static final NamespacedKey GUIDE_STAVE_CONFIGURATOR = newKey("guide_stave");
    public static final NamespacedKey GUIDE_LIQUEFACTION = newKey("guide_liquefaction");
    public static final NamespacedKey GUIDE_REALISATION = newKey("guide_realisation");

    public static final NamespacedKey REALISATION_ALTAR_RECIPE_TYPE = newKey("r_d_c");
    public static final NamespacedKey REALISATION_ALTAR_RECIPE_SIGIL = newKey("r_d_s");
    public static final NamespacedKey LIQUEFACTION_CRAFTING_RECIPE_TYPE = newKey("l_d_c");
    public static final NamespacedKey LIQUEFACTION_SPELL_RECIPE_TYPE = newKey("l_d_s");
    public static final NamespacedKey NETHER_DRAINING_RECIPE_TYPE = newKey("nether_draining");

    // PDC
    // Items
    public static final NamespacedKey PDC_IS_STORIED = newKey("is_s");
    public static final NamespacedKey PDC_POTENTIAL_STORIES = newKey("s_pot");
    /** 故事上限扁平 int 键（v2）：原 JSON 编码（JS_S_AS/JS_S_T）中 tier 只写不读，
     * 消费值仅 available count 一个数字；读取 int 优先、JSON 回退（旧存档兼容） */
    public static final NamespacedKey PDC_STORY_LIMIT = newKey("s_lim_i");
    public static final NamespacedKey PDC_CURRENT_NUMBER_OF_STORIES = newKey("s_cur_n");
    public static final NamespacedKey PDC_STORIES = newKey("s_list");
    /** 故事列表 v2 瘦编码（单容器两键：合并 id 串 + 稀有度 int[]）；读取 v2 优先，回退 v1 */
    public static final NamespacedKey PDC_STORIES_V2 = newKey("s_list2");
    public static final NamespacedKey PDC_PLATE_STORAGE = newKey("plt");
    public static final NamespacedKey PDC_STAVE_STORAGE = newKey("stv");

    // 法术对抗标记（Prism/AntiPrism）与回忆水晶格位置——回调内静态引用，
    // 替代原每调用 newKey 构造（惯例与 PDC_* 族一致）
    public static final NamespacedKey PDC_PRISM = newKey("PRISM");
    public static final NamespacedKey PDC_ANTIPRISM = newKey("ANTIPRISM");
    public static final NamespacedKey PDC_RECALL_LOCATION = newKey("location");
    public static final NamespacedKey PDC_SATCHEL_STORAGE = newKey("satchel");
    public static final NamespacedKey PDC_ON_COOLDOWN = newKey("cooldown");
    public static final NamespacedKey PDC_PAINT_TYPE = newKey("paint_type");
    public static final NamespacedKey PDC_IS_GILDED = newKey("gilded");

    // Type - Story
    public static final NamespacedKey STORY_ID = newKey("s_id");
    public static final NamespacedKey STORY_RARITY = newKey("s_r");
    public static final NamespacedKey STORY_TYPE = newKey("s_t");
    public static final NamespacedKey STORY_IS_GILDED = newKey("s_g");
    // Type - Story v2（瘦编码容器内两键）
    public static final NamespacedKey STORY_IDS_JOINED = newKey("s_ids");
    public static final NamespacedKey STORY_RARITIES = newKey("s_rars");
    // Type - Story chunk v2（区块晶簇状态容器内五键：ids 连接串/稀有度数组/位置长数组/共享世界/镀金打包串）
    public static final NamespacedKey CHUNK_STORY_IDS = newKey("c_ids");
    public static final NamespacedKey CHUNK_STORY_RARITIES = newKey("c_rars");
    public static final NamespacedKey CHUNK_STORY_POSITIONS = newKey("c_pos");
    public static final NamespacedKey CHUNK_STORY_WORLD = newKey("c_world");
    public static final NamespacedKey CHUNK_STORY_GILDED = newKey("c_gild");

    // Type - Plate
    public static final NamespacedKey PLATE_TIER = newKey("p_t");
    // Type - Stave v2（扁平容器内五键：槽位/法术连接串 + tier/crysta 数组 + cooldown 长数组）
    public static final NamespacedKey STAVE_SLOTS_JOINED = newKey("stv_slots");
    public static final NamespacedKey STAVE_SPELLS_JOINED = newKey("stv_spells");
    public static final NamespacedKey STAVE_TIERS = newKey("stv_t");
    public static final NamespacedKey STAVE_CRYSTAS = newKey("stv_c");
    public static final NamespacedKey STAVE_COOLDOWNS = newKey("stv_cd");
    public static final NamespacedKey PLATE_SPELL = newKey("p_s");
    public static final NamespacedKey PLATE_CHARGES = newKey("p_c");
    public static final NamespacedKey PLATE_COOLDOWN = newKey("p_cd");

    // Type - Plate
    public static final NamespacedKey STAVE_SLOT = newKey("sv_s");
    public static final NamespacedKey STAVE_PLATE = newKey("sv_p");

    // Entities
    public static final NamespacedKey PDC_IS_DISPLAY_STAND = newKey("a_dpy");
    // 原 invul 键（Protectorate 无敌标记）已迁移为 SpellMemory 会话内注册表：
    // 旧实体 NBT 中的残留键无人读取，属惰性数据，无害保留在既有实体上
    public static final NamespacedKey PDC_IS_WEATHER_WITHER = newKey("weather");
    public static final NamespacedKey PDC_IS_SPAWN_OWNER = newKey("owner");
    public static final NamespacedKey PDC_IS_DISPLAY_ITEM = newKey("di");

    // Chunk Storage
    public static final NamespacedKey RESOLUTION_CRYSTAL_MAP = newKey("c_r_c");
    public static final NamespacedKey RESOLUTION_RARITY_MAP = newKey("c_r_r");
    public static final NamespacedKey RESOLUTION_STORY_MAP = newKey("c_r_s");
    public static final NamespacedKey RESOLUTION_STORY_LOCATION = newKey("c_r_l");
    public static final NamespacedKey RESOLUTION_STORY_WORLD = newKey("c_r_w");

    private Keys() {
        throw new IllegalStateException("Utility Class");
    }

    @Nonnull
    public static NamespacedKey newKey(@Nonnull String value) {
        return new NamespacedKey(CrystamaeHistoria.getInstance(), value);
    }
}
