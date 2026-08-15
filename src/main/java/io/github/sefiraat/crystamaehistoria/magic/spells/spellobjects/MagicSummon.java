package io.github.sefiraat.crystamaehistoria.magic.spells.spellobjects;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;
import java.util.function.Consumer;

public class MagicSummon {

    @Getter
    public final UUID mobUUID;
    @Getter
    public final UUID ownerUUID;
    @Getter
    @Setter
    public Consumer<MagicSummon> tickConsumer;

    @ParametersAreNonnullByDefault
    public MagicSummon(UUID mobUUID, UUID ownerUUID) {
        this.mobUUID = mobUUID;
        this.ownerUUID = ownerUUID;
    }

    @Nullable
    public Mob getMob() {
        return (Mob) Bukkit.getEntity(mobUUID);
    }

    @Nullable
    public Player getPlayer() {
        return (Player) Bukkit.getEntity(ownerUUID);
    }

    public void kill() {
        CrystamaeHistoria.getSummonedEntityMap().remove(this);
        Mob mob = (Mob) Bukkit.getEntity(mobUUID);
        if (mob != null) {
            mob.remove();
        }
    }

    public void run() {
        if (tickConsumer != null) {
            try {
                tickConsumer.accept(this);
            } catch (Exception e) {
                // 断路器：tick 消费者异常时停用该消费者，避免剩余生命周期内每秒重复抛异常
                CrystamaeHistoria.getInstance().getLogger()
                    .log(java.util.logging.Level.WARNING, "召唤物 tick 回调执行异常，已停用该回调", e);
                tickConsumer = null;
            }
        }
    }
}

