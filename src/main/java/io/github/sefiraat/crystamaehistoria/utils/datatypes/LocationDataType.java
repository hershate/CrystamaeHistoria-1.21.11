package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectStreamClass;

/**
 * byte[] 与 {@link Location} 之间的 {@link PersistentDataType}。
 * 编码与原 MorePersistentDataTypes 库的 LOCATION 一致：
 * 通过 {@link BukkitObjectOutputStream} 对 Location（ConfigurationSerializable）做对象序列化，
 * 可完整保留世界、坐标与朝向信息。
 * <p>
 * 安全说明：该类型用于物品 PDC（回忆水晶格），而物品 NBT 可被改造客户端注入任意字节。
 * 反序列化因此必须经过类白名单过滤，阻断任意类实例化的反序列化攻击链：
 * Location 以 ConfigurationSerializable Map 形式序列化，合法对象图仅包含
 * java.util（HashMap）与 java.lang（String/Double 等）下的类。
 */
public class LocationDataType implements PersistentDataType<byte[], Location> {

    @Nonnull
    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Nonnull
    @Override
    public Class<Location> getComplexType() {
        return Location.class;
    }

    @Nonnull
    @Override
    public byte[] toPrimitive(@Nonnull Location complex, @Nonnull PersistentDataAdapterContext context) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream bukkitOutputStream = new BukkitObjectOutputStream(outputStream)
        ) {
            bukkitOutputStream.writeObject(complex);
            bukkitOutputStream.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("无法序列化 Location", e);
        }
    }

    @Nonnull
    @Override
    public Location fromPrimitive(@Nonnull byte[] primitive, @Nonnull PersistentDataAdapterContext context) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(primitive);
             FilteredObjectInputStream bukkitInputStream = new FilteredObjectInputStream(inputStream)
        ) {
            final Object object = bukkitInputStream.readObject();
            if (!(object instanceof Location)) {
                throw new IllegalStateException("Location 数据格式不正确");
            }
            return (Location) object;
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new IllegalStateException("无法反序列化 Location（数据损坏或被篡改）", e);
        }
    }

    /**
     * 仅允许 JDK 基础类通过反序列化解析，拒绝一切白名单外的类描述符，
     * 使注入的恶意序列化字节无法实例化任何潜在攻击类。
     */
    private static final class FilteredObjectInputStream extends BukkitObjectInputStream {

        private FilteredObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            final String name = desc.getName();
            if (name.startsWith("java.lang.")
                || name.startsWith("java.util.")
                || name.startsWith("[")
            ) {
                return super.resolveClass(desc);
            }
            throw new InvalidClassException("拒绝反序列化白名单外的类", name);
        }
    }
}
