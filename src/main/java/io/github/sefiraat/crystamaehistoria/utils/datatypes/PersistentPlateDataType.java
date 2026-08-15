package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.magic.SpellType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A {@link PersistentDataType} for {@link InstancePlate}s which uses an
 * {@link Integer} array for storage purposes.
 * Creatively thieved from {@see <a href="https://github.com/baked-libs/dough/blob/main/dough-data/src/main/java/io/github/bakedlibs/dough/data/persistent/PersistentUUIDDataType.java">PersistentUUIDDataType}
 *
 * @author Sfiguz7
 * @author Walshy
 */

public class PersistentPlateDataType implements PersistentDataType<PersistentDataContainer, InstancePlate> {

    public static final PersistentDataType<PersistentDataContainer, InstancePlate> TYPE = new PersistentPlateDataType();

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    @Nonnull
    public Class<InstancePlate> getComplexType() {
        return InstancePlate.class;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public PersistentDataContainer toPrimitive(InstancePlate complex, PersistentDataAdapterContext context) {
        PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(Keys.PLATE_TIER, PersistentDataType.INTEGER, complex.getTier());
        container.set(Keys.PLATE_SPELL, PersistentDataType.STRING, complex.getStoredSpell().getId());
        container.set(Keys.PLATE_CHARGES, PersistentDataType.INTEGER, complex.getCrysta());
        container.set(Keys.PLATE_COOLDOWN, PersistentDataType.LONG, complex.getCooldown());
        return container;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public InstancePlate fromPrimitive(PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        // PDC 内容不可信（/sf cheat 或改造客户端可构造缺键/非法值的法术板）：
        // 原实现在缺键时拆箱 NPE、非法法术名时裸抛 IllegalArgumentException，
        // 现统一转为带上下文的失败关闭异常
        final Integer tier = primitive.get(Keys.PLATE_TIER, PersistentDataType.INTEGER);
        final String spellId = primitive.get(Keys.PLATE_SPELL, PersistentDataType.STRING);
        final Integer charges = primitive.get(Keys.PLATE_CHARGES, PersistentDataType.INTEGER);
        final Long cooldown = primitive.get(Keys.PLATE_COOLDOWN, PersistentDataType.LONG);
        if (tier == null || spellId == null || charges == null || cooldown == null) {
            throw new IllegalStateException("充能法术板数据损坏：PDC 键缺失");
        }
        final SpellType spellType;
        try {
            spellType = SpellType.valueOf(spellId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("充能法术板数据损坏：未知法术 " + spellId, e);
        }
        InstancePlate instancePlate = new InstancePlate(tier, spellType, charges);
        instancePlate.setCooldown(cooldown);
        return instancePlate;
    }
}
