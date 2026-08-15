package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataType;
import io.github.sefiraat.crystamaehistoria.slimefun.items.tools.satchel.SatchelInstance;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryRarity;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;

/**
 * A {@link PersistentDataType} for {@link SatchelInstance}
 * Creatively thieved from {@see <a href="https://github.com/baked-libs/dough/blob/main/dough-data/src/main/java/io/github/bakedlibs/dough/data/persistent/PersistentUUIDDataType.java">PersistentUUIDDataType}
 *
 * @author Sfiguz7
 * @author Walshy
 */

public class PersistentSatchelInstanceType implements PersistentDataType<PersistentDataContainer, SatchelInstance> {

    public static final PersistentDataType<PersistentDataContainer, SatchelInstance> TYPE = new PersistentSatchelInstanceType();

    public static final NamespacedKey SATCHEL_ID = Keys.newKey("satchel_id");
    public static final NamespacedKey SATCHEL_TIER = Keys.newKey("satchel_tier");
    public static final NamespacedKey SATCHEL_LAST_USER = Keys.newKey("satchel_last_user");

    public static final NamespacedKey UNIQUE = Keys.newKey("amount_unique");
    public static final NamespacedKey COMMON = Keys.newKey("amount_common");
    public static final NamespacedKey UNCOMMON = Keys.newKey("amount_uncommon");
    public static final NamespacedKey RARE = Keys.newKey("amount_rare");
    public static final NamespacedKey EPIC = Keys.newKey("amount_epic");
    public static final NamespacedKey MYTHICAL = Keys.newKey("amount_mythical");

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    @Nonnull
    public Class<SatchelInstance> getComplexType() {
        return SatchelInstance.class;
    }

    @Override
    @Nonnull
    public PersistentDataContainer toPrimitive(@Nonnull SatchelInstance complex, @Nonnull PersistentDataAdapterContext context) {
        final PersistentDataContainer container = context.newPersistentDataContainer();

        container.set(SATCHEL_ID, DataType.LONG, complex.getId());
        container.set(SATCHEL_TIER, DataType.INTEGER, complex.getTier());
        container.set(SATCHEL_LAST_USER, DataType.STRING, complex.getLastUser());

        container.set(UNIQUE, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.UNIQUE));
        container.set(COMMON, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.COMMON));
        container.set(UNCOMMON, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.UNCOMMON));
        container.set(RARE, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.RARE));
        container.set(EPIC, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.EPIC));
        container.set(MYTHICAL, DataType.INTEGER_ARRAY, complex.getAmounts().get(StoryRarity.MYTHICAL));
        return container;
    }

    @Override
    @Nonnull
    public SatchelInstance fromPrimitive(@Nonnull PersistentDataContainer primitive, @Nonnull PersistentDataAdapterContext context) {
        // PDC 内容不可信（改造客户端可构造缺键收纳袋）：原实现缺键时拆箱 NPE，
        // 且 null 用户名会在下次保存时以 null 写 STRING 键直接抛异常。缺失字段一律取保守默认值
        final Long storedId = primitive.get(SATCHEL_ID, DataType.LONG);
        final Integer storedTier = primitive.get(SATCHEL_TIER, DataType.INTEGER);
        final String name = primitive.get(SATCHEL_LAST_USER, DataType.STRING);
        final SatchelInstance instance = new SatchelInstance(
            storedId != null ? storedId : 0L,
            storedTier != null && storedTier >= 1 ? storedTier : 1
        );

        instance.setLastUser(name != null ? name : "未知");

        instance.setAmounts(StoryRarity.UNIQUE, primitive.get(UNIQUE, DataType.INTEGER_ARRAY));
        instance.setAmounts(StoryRarity.COMMON, primitive.get(COMMON, DataType.INTEGER_ARRAY));
        instance.setAmounts(StoryRarity.UNCOMMON, primitive.get(UNCOMMON, DataType.INTEGER_ARRAY));
        instance.setAmounts(StoryRarity.RARE, primitive.get(RARE, DataType.INTEGER_ARRAY));
        instance.setAmounts(StoryRarity.EPIC, primitive.get(EPIC, DataType.INTEGER_ARRAY));
        instance.setAmounts(StoryRarity.MYTHICAL, primitive.get(MYTHICAL, DataType.INTEGER_ARRAY));

        return instance;
    }
}
