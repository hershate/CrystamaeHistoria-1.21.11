package io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Consumer;

public class MagicProjectile {

    @Getter
    private final UUID projectileUUID;
    @Getter
    @Setter
    private Consumer<MagicProjectile> consumer;

    @ParametersAreNonnullByDefault
    public MagicProjectile(Projectile projectile) {
        this.projectileUUID = projectile.getUniqueId();
    }

    /**
     * @param targetLocation The target location
     * @param speed          The speed to push the projectile
     */
    @ParametersAreNonnullByDefault
    public void setVelocity(Location targetLocation, double speed) {
        Projectile projectile = (Projectile) Bukkit.getEntity(projectileUUID);
        Vector velocity = targetLocation.toVector().subtract(projectile.getLocation().toVector()).normalize();
        setVelocity(velocity, speed);
    }

    /**
     * @param vector The vector to use
     * @param speed  The speed to push the projectile
     */
    @ParametersAreNonnullByDefault
    public void setVelocity(Vector vector, double speed) {
        Projectile projectile = (Projectile) Bukkit.getEntity(projectileUUID);
        // Fireball projectiles want to move wrongly :)
        if (projectile instanceof Fireball) {
            ((Fireball) projectile).setDirection(vector);
        }
        projectile.setVelocity(vector.multiply(speed));
    }

    @Nonnull
    public Location getLocation() {
        Projectile projectile = (Projectile) Bukkit.getEntity(projectileUUID);
        return projectile.getLocation();
    }

    @Nullable
    public Projectile getProjectile() {
        return (Projectile) Bukkit.getEntity(projectileUUID);
    }

    @ParametersAreNonnullByDefault
    public boolean matches(Projectile projectile) {
        return this.projectileUUID.equals(projectile.getUniqueId());
    }

    public void disableGravity() {
        Projectile projectile = (Projectile) Bukkit.getEntity(projectileUUID);
        if (projectile != null) {
            projectile.setGravity(false);
        }
    }

    public void kill() {
        CrystamaeHistoria.getSpellMemory().unregisterProjectile(this);
        Projectile projectile = (Projectile) Bukkit.getEntity(projectileUUID);
        if (projectile != null) {
            projectile.remove();
        }
    }

    public void run() {
        if (consumer != null) {
            try {
                consumer.accept(this);
            } catch (Exception e) {
                // 断路器：tick 消费者异常时禁用该消费者，避免剩余生命周期内每秒重复抛异常
                CrystamaeHistoria.getInstance().getLogger()
                    .log(java.util.logging.Level.WARNING, "弹射物 tick 回调执行异常，已停用该回调", e);
                consumer = null;
            }
        }
    }
}
