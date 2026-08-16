package io.github.sefiraat.crystamaehistoria.utils.mobgoals;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentUUIDDataType;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractGoal<T extends Mob> implements Goal<T> {

    /**
     * 目标类型集常量：所有召唤物目标均为 TARGET；原实现每次调用分配新 EnumSet
     * （注册与目标选择器重评估时反复调用）。内容恒定，共享只读。
     */
    private static final EnumSet<GoalType> GOAL_TYPES = EnumSet.of(GoalType.TARGET);

    @Getter
    protected final UUID owner;
    @Getter
    protected T self;
    protected GoalKey<T> goalKey;

    protected AbstractGoal(UUID owningPlayer) {
        super();
        this.owner = owningPlayer;
    }

    public void setSelf(T self) {
        this.self = self;
        Class<T> clazz = (Class<T>) self.getClass();
        this.goalKey = GoalKey.of(clazz, Keys.newKey("mob_goal"));
    }

    @Override
    public final boolean shouldActivate() {
        return true;
    }

    @Override
    public final boolean shouldStayActive() {
        return true;
    }

    @Override
    public final void stop() {
        self.getPathfinder().stopPathfinding();
        self.setTarget(null);
    }

    @Override
    public void tick() {
        final Player player = removeOffline();
        // 目标读取缓存：本 tick 内多次判定共用一次实体记忆读取
        final LivingEntity currentTarget = self.getTarget();

        if (player == null || (currentTarget != null && currentTarget.equals(player))) {
            self.setTarget(null);
            return;
        }

        if (!getTickCondition() || (currentTarget != null && !currentTarget.isDead())) {
            return;
        }

        if (getTargetsEnemies()) {
            final List<LivingEntity> entities = new ArrayList<>(
                player.getWorld().getNearbyEntitiesByType(
                    getTargetClass(),
                    player.getLocation(),
                    10,
                    10,
                    10,
                    entity -> {
                        final UUID testOwner = DataTypeMethods.getCustom(entity, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE);
                        if (testOwner == null) {
                            return true;
                        } else {
                            return !testOwner.equals(owner);
                        }
                    }
                )
            );

            if (!entities.isEmpty()) {
                LivingEntity random = entities.get(ThreadLocalRandom.current().nextInt(entities.size()));
                self.setTarget(random);
                self.attack(random);
                return;
            }
        }

        // 跨世界 distance 会抛 IllegalArgumentException（主人过传送门后 AI 每 tick 崩）：
        // 不同世界时跳过跟随逻辑（召唤物由 SpellMemory 过期清理兜底）
        // 距离以平方比较（免开方），阈值同平方
        if (getFollowsPlayer()
            && self.getLocation().getWorld() == player.getWorld()
        ) {
            final double stayNear = getStayNearDistance();
            if (self.getLocation().distanceSquared(player.getLocation()) > stayNear * stayNear) {
                final Location location = player.getLocation().clone().add(
                    ThreadLocalRandom.current().nextDouble(-1.5, 1.5),
                    0,
                    ThreadLocalRandom.current().nextDouble(-1.5, 1.5)
                );
                self.getPathfinder().moveTo(location);
            }
        }

        customActions(player);

    }

    @Nullable
    public Player removeOffline() {
        // 单次在线查询：getPlayer(uuid) 为 null ⇔ getOfflinePlayer(uuid).isOnline() 为 false
        // （同一 UUID 语义等价，免去 OfflinePlayer 档案对象与两次额外查找）
        final Player player = Bukkit.getPlayer(owner);

        if (player == null) {
            self.remove();
            return null;
        }
        return player;
    }

    public boolean getTickCondition() {
        return true;
    }

    public boolean getTargetsEnemies() {
        return true;
    }

    public Class<? extends LivingEntity> getTargetClass() {
        return Monster.class;
    }

    public boolean getFollowsPlayer() {
        return true;
    }

    public double getStayNearDistance() {
        return 5D;
    }

    public void customActions(Player player) {

    }

    @Override
    @Nonnull
    public final GoalKey<T> getKey() {
        return goalKey;
    }

    @Override
    @Nonnull
    public final EnumSet<GoalType> getTypes() {
        return GOAL_TYPES;
    }

}
