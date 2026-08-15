package io.github.sefiraat.crystamaehistoria.magic;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Consumer;

public class CastInformation {

    @Getter
    private final UUID caster;
    @Getter
    private final int staveLevel;
    @Getter
    private final Location castLocation;
    @Getter
    @Setter
    private SpellType spellType;
    @Getter
    @Setter
    private Location damageLocation;
    @Getter
    @Setter
    private LivingEntity mainTarget;
    @Getter
    @Setter
    private Block hitBlock;
    /**
     * 施法瞬间的视线 raycast 结果（50 格）。懒计算 + 一次冻结：
     * 构造时不 raycast（施法前置校验失败的交互不需要），
     * 首次读取或 freezeTargetsOnCast() 时以单次 rayTraceBlocks(50) 解析，
     * 之后值恒定——tick 型法术在后续 tick 读取到的仍是施法瞬间语义。
     */
    private RayTraceResult targetRayTraceOnCast;
    private boolean targetsResolved;
    @Getter
    @Setter
    private Location projectileLocation;
    @Getter
    @Setter
    private int currentTick = 1;
    @Setter
    private Consumer<CastInformation> beforeProjectileHitEvent;
    @Setter
    private Consumer<CastInformation> projectileHitEvent;
    @Setter
    private Consumer<CastInformation> afterProjectileHitEvent;
    @Setter
    private Consumer<CastInformation> projectileHitBlockEvent;
    @Setter
    private Consumer<CastInformation> tickEvent;
    @Setter
    private Consumer<CastInformation> afterTicksEvent;

    @ParametersAreNonnullByDefault
    public CastInformation(Player caster, int staveLevel) {
        this.caster = caster.getUniqueId();
        this.staveLevel = staveLevel;
        this.castLocation = caster.getLocation().clone();
    }

    public Block getTargetedBlockOnCast() {
        resolveTargetsOnCast();
        return targetRayTraceOnCast == null ? null : targetRayTraceOnCast.getHitBlock();
    }

    public BlockFace getTargetedBlockFaceOnCast() {
        resolveTargetsOnCast();
        return targetRayTraceOnCast == null ? null : targetRayTraceOnCast.getHitBlockFace();
    }

    /**
     * 强制立即解析并冻结视线目标（等价于旧实现构造器中的 raycast）。
     * 施法前置校验全部通过后、执行法术前调用，保证后续任意回调
     * （含跨 tick 的 tickEvent）读到的都是施法瞬间值。
     */
    public void freezeTargetsOnCast() {
        resolveTargetsOnCast();
    }

    private void resolveTargetsOnCast() {
        if (targetsResolved) {
            return;
        }
        targetsResolved = true;
        final Player player = Bukkit.getPlayer(caster);
        if (player != null) {
            // 单次 rayTraceBlocks 同时给出命中方块与命中面，
            // 语义与旧 getTargetBlockExact(50)/getTargetBlockFace(50)
            // （内部各自 rayTraceBlocks 一次）完全一致，成本减半
            targetRayTraceOnCast = player.rayTraceBlocks(50);
        }
    }

    public Player getCasterAsPlayer() {
        return Bukkit.getPlayer(this.caster);
    }

    public void runPreAffectEvent() {
        if (beforeProjectileHitEvent != null) {
            beforeProjectileHitEvent.accept(this);
        }
    }

    public void runAffectEvent() {
        if (projectileHitEvent != null) {
            projectileHitEvent.accept(this);
        }
    }

    public void runPostAffectEvent() {
        if (afterProjectileHitEvent != null) {
            afterProjectileHitEvent.accept(this);
        }
    }

    public void runProjectileHitBlockEvent() {
        if (projectileHitBlockEvent != null) {
            projectileHitBlockEvent.accept(this);
        }
    }

    public void runTickEvent() {
        if (tickEvent != null) {
            tickEvent.accept(this);
        }
        this.currentTick++;
    }

    public void runAfterTicksEvent() {
        if (afterTicksEvent != null) {
            afterTicksEvent.accept(this);
        }
    }

}
