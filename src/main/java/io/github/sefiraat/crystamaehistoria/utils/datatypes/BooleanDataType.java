package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;

/**
 * byte 与 Boolean 之间的 {@link PersistentDataType}。
 * 编码与原 MorePersistentDataTypes 库一致（1 表示 true，0 表示 false）。
 */
public class BooleanDataType implements PersistentDataType<Byte, Boolean> {

    @Nonnull
    @Override
    public Class<Byte> getPrimitiveType() {
        return Byte.class;
    }

    @Nonnull
    @Override
    public Class<Boolean> getComplexType() {
        return Boolean.class;
    }

    @Nonnull
    @Override
    public Byte toPrimitive(@Nonnull Boolean complex, @Nonnull PersistentDataAdapterContext context) {
        return complex ? (byte) 1 : (byte) 0;
    }

    @Nonnull
    @Override
    public Boolean fromPrimitive(@Nonnull Byte primitive, @Nonnull PersistentDataAdapterContext context) {
        return primitive == 1;
    }
}
