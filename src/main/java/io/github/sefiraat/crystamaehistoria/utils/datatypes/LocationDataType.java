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

/**
 * byte[] 与 {@link Location} 之间的 {@link PersistentDataType}。
 * 编码与原 MorePersistentDataTypes 库的 LOCATION 一致：
 * 通过 {@link BukkitObjectOutputStream} 对 Location（ConfigurationSerializable）做对象序列化，
 * 可完整保留世界、坐标与朝向信息。
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
             BukkitObjectInputStream bukkitInputStream = new BukkitObjectInputStream(inputStream)
        ) {
            return (Location) bukkitInputStream.readObject();
        } catch (IOException e) {
            throw new IllegalStateException("无法反序列化 Location", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("反序列化 Location 时找不到对应类", e);
        }
    }
}
