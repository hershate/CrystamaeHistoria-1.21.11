package io.github.sefiraat.crystamaehistoria.utils;

import com.destroystokyo.paper.entity.ai.MobGoals;
import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicFallingBlock;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicProjectile;
import io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects.MagicSummon;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentUUIDDataType;
import io.github.sefiraat.crystamaehistoria.utils.mobgoals.AbstractGoal;
import io.github.sefiraat.crystamaehistoria.utils.theme.ThemeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.collections.Pair;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.UUID;
import java.util.function.Consumer;

@UtilityClass
public class SpellUtils {

    /**
     * 全部可被召唤为法术召唤物的实体类型（供世界级高频事件的类型门控——
     * 非候选类型的实体死亡/方块变化事件可零成本跳过 PDC 读取）。
     * 预置集合覆盖现存 12 个召唤法术的全部类型，保证崩溃重启后仅存 PDC
     * 标记的残留召唤物仍被门控覆盖；{@code summonTemporaryMob} 在运行期
     * 自动登记新类型，新增召唤法术无需手动维护。
     */
    private static final EnumSet<EntityType> SUMMONABLE_MOB_TYPES = EnumSet.of(
        EntityType.VEX,
        EntityType.GOAT,
        EntityType.GIANT,
        EntityType.BLAZE,
        EntityType.COW,
        EntityType.SILVERFISH,
        EntityType.ENDERMITE,
        EntityType.BAT,
        EntityType.RAVAGER,
        EntityType.ZOMBIE,
        EntityType.PHANTOM,
        EntityType.IRON_GOLEM
    );

    /** 实体类型是否可能是法术召唤物（门控检查，非候选即排除）。 */
    public static boolean isSummonableMobType(EntityType entityType) {
        return SUMMONABLE_MOB_TYPES.contains(entityType);
    }

    @ParametersAreNonnullByDefault
    public static <T extends Mob> MagicSummon summonTemporaryMob(
        EntityType entityType,
        UUID caster,
        Location location,
        @Nullable AbstractGoal<T> goal
    ) {
        return summonTemporaryMob(entityType, caster, location, goal, 30);
    }

    @ParametersAreNonnullByDefault
    public static <T extends Mob> MagicSummon summonTemporaryMob(
        EntityType entityType,
        UUID caster,
        Location location,
        @Nullable AbstractGoal<T> goal,
        int timeInSeconds
    ) {
        return summonTemporaryMob(entityType, caster, location, goal, timeInSeconds * 1000L, null);
    }

    @ParametersAreNonnullByDefault
    private static <T extends Mob> MagicSummon summonTemporaryMob(
        EntityType entityType,
        UUID caster,
        Location location,
        @Nullable AbstractGoal<T> goal,
        long duration,
        @Nullable Consumer<MagicSummon> tickConsumer
    ) {
        // 生成前预配置（consumer 重载）：PDC 标记与名称随生成包广播；
        // AI 目标（goal API）保守留在生成后
        final Class<? extends org.bukkit.entity.Entity> mobClass = entityType.getEntityClass();
        final T mob;
        if (mobClass != null) {
            mob = (T) location.getWorld().spawn(location, mobClass, spawned -> {
                DataTypeMethods.setCustom(spawned, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE, caster);
                spawned.setCustomName(ThemeType.getRandomEggName());
            });
        } else {
            mob = (T) location.getWorld().spawnEntity(location, entityType);
            DataTypeMethods.setCustom(mob, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE, caster);
            mob.setCustomName(ThemeType.getRandomEggName());
        }
        final MagicSummon magicSummon = new MagicSummon(mob.getUniqueId(), caster);
        final MobGoals mobGoals = Bukkit.getMobGoals();
        // 运行期自动登记类型，保证未来新增召唤类型无需手动维护门控白名单
        SUMMONABLE_MOB_TYPES.add(entityType);

        if (tickConsumer != null) {
            magicSummon.setTickConsumer(tickConsumer);
        }

        CrystamaeHistoria.getSummonedEntityMap().put(magicSummon, System.currentTimeMillis() + duration);

        if (goal == null) {
            mobGoals.removeAllGoals(mob);
        } else {
            goal.setSelf(mob);
            mobGoals.addGoal(mob, 1, goal);
        }

        return magicSummon;
    }

    @ParametersAreNonnullByDefault
    public static <T extends Mob> MagicSummon summonTemporaryMob(
        EntityType entityType,
        UUID caster,
        Location location,
        @Nullable AbstractGoal<T> goal,
        Consumer<MagicSummon> tickConsumer
    ) {
        return summonTemporaryMob(entityType, caster, location, goal, 120, tickConsumer);
    }

    @ParametersAreNonnullByDefault
    public static <T extends Mob> MagicSummon summonTemporaryMob(
        EntityType entityType,
        UUID caster,
        Location location,
        @Nullable AbstractGoal<T> goal,
        int timeInSeconds,
        Consumer<MagicSummon> tickConsumer
    ) {
        return summonTemporaryMob(entityType, caster, location, goal, timeInSeconds * 1000L, tickConsumer);
    }

    @ParametersAreNonnullByDefault
    public static MagicProjectile summonMagicProjectile(
        CastInformation castInformation,
        EntityType entityType,
        Location location
    ) {
        return summonMagicProjectile(castInformation, entityType, location, 5);
    }

    @ParametersAreNonnullByDefault
    public static MagicProjectile summonMagicProjectile(
        CastInformation castInformation,
        EntityType entityType,
        Location location,
        int timeInSeconds
    ) {
        return summonMagicProjectile(castInformation, entityType, location, timeInSeconds * 1000L, null);
    }

    @ParametersAreNonnullByDefault
    private static MagicProjectile summonMagicProjectile(
        CastInformation castInformation,
        EntityType entityType,
        Location location,
        long duration,
        @Nullable Consumer<MagicProjectile> tickConsumer
    ) {
        // 生成前预配置（consumer 重载）：配置随生成包一次性广播，原
        // spawn 后逐个 setter 会各发一次实体元数据同步包
        final Class<? extends org.bukkit.entity.Entity> entityClass = entityType.getEntityClass();
        final Projectile projectile;
        if (entityClass != null) {
            projectile = (Projectile) location.getWorld().spawn(location, entityClass, spawned -> {
                final Projectile proj = (Projectile) spawned;
                proj.setShooter(Bukkit.getPlayer(castInformation.getCaster()));
                proj.setBounce(false);
                if (proj instanceof Fireball) {
                    Fireball fireball = (Fireball) proj;
                    fireball.setIsIncendiary(false);
                    fireball.setYield(0f);
                }
            });
        } else {
            // 未知实体类（无 Class 映射的类型）回退原路径
            projectile = (Projectile) location.getWorld().spawnEntity(location, entityType);
            projectile.setShooter(Bukkit.getPlayer(castInformation.getCaster()));
            projectile.setBounce(false);
            if (projectile instanceof Fireball) {
                Fireball fireball = (Fireball) projectile;
                fireball.setIsIncendiary(false);
                fireball.setYield(0f);
            }
        }
        final MagicProjectile magicProjectile = new MagicProjectile(projectile);

        if (tickConsumer != null) {
            magicProjectile.setConsumer(tickConsumer);
        }
        CrystamaeHistoria.getSpellMemory().registerProjectile(
            magicProjectile, new Pair<>(castInformation, System.currentTimeMillis() + duration));
        return magicProjectile;
    }

    @ParametersAreNonnullByDefault
    public static MagicProjectile summonMagicProjectile(
        CastInformation castInformation,
        EntityType entityType,
        Location location,
        Consumer<MagicProjectile> tickConsumer
    ) {
        return summonMagicProjectile(castInformation, entityType, location, 30, tickConsumer);
    }

    @ParametersAreNonnullByDefault
    public static MagicProjectile summonMagicProjectile(
        CastInformation castInformation,
        EntityType entityType,
        Location location,
        int timeInSeconds,
        Consumer<MagicProjectile> tickConsumer
    ) {
        return summonMagicProjectile(castInformation, entityType, location, timeInSeconds * 1000L, tickConsumer);
    }

    @ParametersAreNonnullByDefault
    public static MagicFallingBlock summonMagicFallingBlock(
        CastInformation castInformation,
        Location location,
        Material material
    ) {
        return summonMagicFallingBlock(castInformation, location, material, 30);
    }

    @ParametersAreNonnullByDefault
    public static MagicFallingBlock summonMagicFallingBlock(
        CastInformation castInformation,
        Location location,
        Material material,
        int timeInSeconds
    ) {
        return summonMagicFallingBlock(castInformation, location, material, timeInSeconds * 1000L);
    }

    @ParametersAreNonnullByDefault
    private static MagicFallingBlock summonMagicFallingBlock(
        CastInformation castInformation,
        Location location,
        Material material,
        long duration
    ) {
        return summonMagicFallingBlock(castInformation, location, material.createBlockData(), duration);
    }

    /** BlockData 重载：调用方缓存 BlockData 实例（Material.createBlockData 每次新建 ~百 ns），可跨多次生成复用（不可变配置） */
    @ParametersAreNonnullByDefault
    public static MagicFallingBlock summonMagicFallingBlock(
        CastInformation castInformation,
        Location location,
        org.bukkit.block.data.BlockData blockData,
        long duration
    ) {
        final FallingBlock fallingBlock = location.getWorld().spawnFallingBlock(location, blockData);
        final MagicFallingBlock magicFallingBlock = new MagicFallingBlock(fallingBlock);

        CrystamaeHistoria.getSpellMemory().registerFallingBlock(
            magicFallingBlock, new Pair<>(castInformation, System.currentTimeMillis() + duration));
        return magicFallingBlock;
    }
}
