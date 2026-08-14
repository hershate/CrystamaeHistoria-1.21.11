package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataType;

/**
 * 本地实现的附加 {@link PersistentDataType} 常量集合。
 * <p>
 * 原先依赖第三方库 MorePersistentDataTypes (com.jeff_media)，
 * 为使插件仅依赖 paper-api 与 Slimefun，这里提供等价的原生实现。
 * 编码格式与原库保持一致，以兼容历史数据：
 * <ul>
 *     <li>BOOLEAN: byte (0/1)</li>
 *     <li>DOUBLE_ARRAY: byte[]（DataOutputStream 写出长度与元素）</li>
 *     <li>INTEGER_ARRAY: 直接使用 Bukkit 原生 int[] 类型</li>
 *     <li>LOCATION: byte[]（BukkitObjectOutputStream 序列化）</li>
 * </ul>
 */
public interface DataType {

    PersistentDataType<Byte, Boolean> BOOLEAN = new BooleanDataType();

    PersistentDataType<byte[], double[]> DOUBLE_ARRAY = new DoubleArrayDataType();

    PersistentDataType<int[], int[]> INTEGER_ARRAY = PersistentDataType.INTEGER_ARRAY;

    PersistentDataType<byte[], Location> LOCATION = new LocationDataType();

    PersistentDataType<String, String> STRING = PersistentDataType.STRING;

    PersistentDataType<Integer, Integer> INTEGER = PersistentDataType.INTEGER;

    PersistentDataType<Long, Long> LONG = PersistentDataType.LONG;
}
