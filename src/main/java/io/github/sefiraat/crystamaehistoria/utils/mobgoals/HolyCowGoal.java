package io.github.sefiraat.crystamaehistoria.utils.mobgoals;

import io.github.sefiraat.crystamaehistoria.utils.Keys;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.crystamaehistoria.utils.datatypes.PersistentUUIDDataType;
import org.bukkit.Location;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class HolyCowGoal extends AbstractGoal<Cow> {

    public HolyCowGoal(UUID owningPlayer) {
        super(owningPlayer);
    }

    @Override
    public void tick() {
        final Player player = removeOffline();
        // 目标读取缓存：本 tick 内多次判定共用一次实体记忆读取
        final LivingEntity currentTarget = self.getTarget();

        if (player == null) {
            return;
        }

        if (currentTarget != null && currentTarget.equals(player)) {
            self.setTarget(null);
            return;
        }

        final List<LivingEntity> entitiesAroundCow = new ArrayList<>(
            player.getWorld().getNearbyEntitiesByType(
                Monster.class,
                self.getLocation(),
                1,
                1,
                1,
                entity -> {
                    final UUID testOwner = DataTypeMethods.getCustom(entity, Keys.PDC_IS_SPAWN_OWNER, PersistentUUIDDataType.TYPE);
                    if (testOwner == null) {
                        return true;
                    } else {
                        return !testOwner.equals(getOwner());
                    }
                }
            )
        );

        if (!entitiesAroundCow.isEmpty()) {
            Entity entity = getSelf();
            entity.getLocation().getWorld().createExplosion(player, entity.getLocation(), 10, false, false);
            getSelf().remove();
            return;
        }

        if (currentTarget != null && !currentTarget.isDead()) {
            return;
        }

        if (getTargetsEnemies()) {
            final List<LivingEntity> entities = new ArrayList<>(
                player.getWorld().getNearbyEntitiesByType(
                    Monster.class,
                    player.getLocation(),
                    15,
                    15,
                    15,
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
                int random = ThreadLocalRandom.current().nextInt(entities.size());
                self.getPathfinder().moveTo(entities.get(random).getLocation());
                return;
            }
        }

        // 跨世界 distance 会抛 IllegalArgumentException：不同世界时跳过跟随（同 AbstractGoal）
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
    }
}
