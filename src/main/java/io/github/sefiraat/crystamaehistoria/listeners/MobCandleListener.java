package io.github.sefiraat.crystamaehistoria.listeners;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.BoundingBox;

import java.util.Map;

public class MobCandleListener implements Listener {

    @EventHandler
    public void onInteract(CreatureSpawnEvent e) {
        final Entity entity = e.getEntity();
        if (entity instanceof Monster
            && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER
            && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG
            && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.BUILD_WITHER
        ) {
            // CreatureSpawnEvent 为世界级高频事件：常态（无禁刷区）零迭代器分配
            final Map<BoundingBox, Long> areas = CrystamaeHistoria.getSpellMemory().getNoSpawningAreas();
            if (areas.isEmpty()) {
                return;
            }
            for (BoundingBox boundingBox : areas.keySet()) {
                if (boundingBox.contains(e.getLocation().toVector())) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }
}