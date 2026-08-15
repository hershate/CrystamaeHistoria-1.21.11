package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * byte[] 与 double[] 之间的 {@link PersistentDataType}。
 * 编码与原 MorePersistentDataTypes 库一致：
 * DataOutputStream 先写入数组长度，再逐个写入 double 值。
 */
public class DoubleArrayDataType implements PersistentDataType<byte[], double[]> {

    @Nonnull
    @Override
    public Class<byte[]> getPrimitiveType() {
        return byte[].class;
    }

    @Nonnull
    @Override
    public Class<double[]> getComplexType() {
        return double[].class;
    }

    @Nonnull
    @Override
    public byte[] toPrimitive(@Nonnull double[] complex, @Nonnull PersistentDataAdapterContext context) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(complex.length);
            for (double number : complex) {
                dos.writeDouble(number);
            }
            dos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("无法序列化 double 数组", e);
        }
    }

    @Nonnull
    @Override
    public double[] fromPrimitive(@Nonnull byte[] primitive, @Nonnull PersistentDataAdapterContext context) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(primitive); DataInputStream dis = new DataInputStream(bis)) {
            final int length = dis.readInt();
            // 长度字段来自持久化数据（不可信）：负值会 NegativeArraySizeException，
            // 超大值会 OOM；必须与实际字节数一致才接受
            if (length < 0 || length > (primitive.length - Integer.BYTES) / Double.BYTES) {
                throw new IllegalStateException("double 数组数据损坏：声明长度 " + length
                    + " 与实际数据量不符（字节数 " + primitive.length + "）");
            }
            double[] doubles = new double[length];
            for (int i = 0; i < doubles.length; i++) {
                doubles[i] = dis.readDouble();
            }
            return doubles;
        } catch (IOException e) {
            throw new IllegalStateException("无法反序列化 double 数组（数据损坏或被截断）", e);
        }
    }
}
