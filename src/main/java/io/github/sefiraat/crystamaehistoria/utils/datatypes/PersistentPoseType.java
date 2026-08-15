package io.github.sefiraat.crystamaehistoria.utils.datatypes;

import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataType;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.InstancePlate;
import io.github.sefiraat.crystamaehistoria.slimefun.items.artistic.PoseCloner;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.EulerAngle;

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

public class PersistentPoseType implements PersistentDataType<PersistentDataContainer, PoseCloner.StoredPose> {

    public static final NamespacedKey HEAD = Keys.newKey("head");
    public static final NamespacedKey BODY = Keys.newKey("body");
    public static final NamespacedKey LEFT_ARM = Keys.newKey("left_arm");
    public static final NamespacedKey RIGHT_ARM = Keys.newKey("right_arm");
    public static final NamespacedKey LEFT_LEG = Keys.newKey("left_leg");
    public static final NamespacedKey RIGHT_LEG = Keys.newKey("right_leg");
    public static final NamespacedKey SMALL = Keys.newKey("small");
    public static final NamespacedKey VISIBLE = Keys.newKey("visible");
    public static final NamespacedKey PLATE = Keys.newKey("plate");
    public static final NamespacedKey ARMS = Keys.newKey("arms");
    public static final NamespacedKey GRAVITY = Keys.newKey("gravity");

    public static final PersistentDataType<PersistentDataContainer, PoseCloner.StoredPose> TYPE = new PersistentPoseType();

    @Override
    @Nonnull
    public Class<PersistentDataContainer> getPrimitiveType() {
        return PersistentDataContainer.class;
    }

    @Override
    @Nonnull
    public Class<PoseCloner.StoredPose> getComplexType() {
        return PoseCloner.StoredPose.class;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public PersistentDataContainer toPrimitive(PoseCloner.StoredPose complex, PersistentDataAdapterContext context) {
        PersistentDataContainer container = context.newPersistentDataContainer();
        container.set(HEAD, DataType.DOUBLE_ARRAY, new double[]{
            complex.getHead().getX(),
            complex.getHead().getY(),
            complex.getHead().getZ()}
        );
        container.set(BODY, DataType.DOUBLE_ARRAY, new double[]{
            complex.getBody().getX(),
            complex.getBody().getY(),
            complex.getBody().getZ()}
        );
        container.set(LEFT_ARM, DataType.DOUBLE_ARRAY, new double[]{
            complex.getLeftArm().getX(),
            complex.getLeftArm().getY(),
            complex.getLeftArm().getZ()}
        );
        container.set(RIGHT_ARM, DataType.DOUBLE_ARRAY, new double[]{
            complex.getRightArm().getX(),
            complex.getRightArm().getY(),
            complex.getRightArm().getZ()}
        );
        container.set(LEFT_LEG, DataType.DOUBLE_ARRAY, new double[]{
            complex.getLeftLeg().getX(),
            complex.getLeftLeg().getY(),
            complex.getLeftLeg().getZ()}
        );
        container.set(RIGHT_LEG, DataType.DOUBLE_ARRAY, new double[]{
            complex.getRightLeg().getX(),
            complex.getRightLeg().getY(),
            complex.getRightLeg().getZ()}
        );
        container.set(SMALL, DataType.BOOLEAN, complex.isSmall());
        container.set(VISIBLE, DataType.BOOLEAN, complex.isVisible());
        container.set(PLATE, DataType.BOOLEAN, complex.isPlateVisible());
        container.set(ARMS, DataType.BOOLEAN, complex.isArmsVisible());
        container.set(GRAVITY, DataType.BOOLEAN, complex.isHasGravity());
        return container;
    }

    @Override
    @Nonnull
    @ParametersAreNonnullByDefault
    public PoseCloner.StoredPose fromPrimitive(PersistentDataContainer primitive, PersistentDataAdapterContext context) {
        // 姿态数据来自持久化 PDC（物品可携带，不可信）：逐字段校验，损坏时抛出带上下文的异常而非 NPE/AIOOBE
        double[] head = requireAngleArray(primitive, HEAD, "head");
        double[] body = requireAngleArray(primitive, BODY, "body");
        double[] leftArm = requireAngleArray(primitive, LEFT_ARM, "left_arm");
        double[] rightArm = requireAngleArray(primitive, RIGHT_ARM, "right_arm");
        double[] leftLeg = requireAngleArray(primitive, LEFT_LEG, "left_leg");
        double[] rightLeg = requireAngleArray(primitive, RIGHT_LEG, "right_leg");
        boolean isSmall = requireBoolean(primitive, SMALL, "small");
        boolean isVisible = requireBoolean(primitive, VISIBLE, "visible");
        boolean plateVisible = requireBoolean(primitive, PLATE, "plate");
        boolean armsVisible = requireBoolean(primitive, ARMS, "arms");
        boolean hasGravity = requireBoolean(primitive, GRAVITY, "gravity");
        return new PoseCloner.StoredPose(
            new EulerAngle(head[0], head[1], head[2]),
            new EulerAngle(body[0], body[1], body[2]),
            new EulerAngle(leftArm[0], leftArm[1], leftArm[2]),
            new EulerAngle(rightArm[0], rightArm[1], rightArm[2]),
            new EulerAngle(leftLeg[0], leftLeg[1], leftLeg[2]),
            new EulerAngle(rightLeg[0], rightLeg[1], rightLeg[2]),
            isSmall,
            isVisible,
            plateVisible,
            armsVisible,
            hasGravity
        );
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    private static double[] requireAngleArray(PersistentDataContainer primitive, NamespacedKey key, String name) {
        final double[] values = primitive.get(key, DataType.DOUBLE_ARRAY);
        if (values == null || values.length < 3) {
            throw new IllegalStateException("姿态数据损坏：缺少 " + name + " 角度分量");
        }
        return values;
    }

    @ParametersAreNonnullByDefault
    private static boolean requireBoolean(PersistentDataContainer primitive, NamespacedKey key, String name) {
        final Boolean value = primitive.get(key, DataType.BOOLEAN);
        if (value == null) {
            throw new IllegalStateException("姿态数据损坏：缺少 " + name + " 标志");
        }
        return value;
    }
}
