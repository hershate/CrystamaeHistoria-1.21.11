package io.github.sefiraat.crystamaehistoria.runnables.spells;

import io.github.sefiraat.crystamaehistoria.utils.ArmourStandUtils;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.ParametersAreNonnullByDefault;

public class FloatingHeadAnimation extends BukkitRunnable {
    public static final long SPEED = 1;

    private final ArmorStand armorStand;

    @ParametersAreNonnullByDefault
    public FloatingHeadAnimation(ArmorStand armorStand) {
        this.armorStand = armorStand;
    }

    @Override
    public void run() {
        // 上游遗留死分支已移除：panelAnimationStep 忽略方向参数（头部持续旋转为
        // 唯一可见效果），展示架从不垂直位移，Y 越界比较恒 false——原每 tick
        // getLocation() 分配与分支翻转均为纯开销
        ArmourStandUtils.panelAnimationStep(armorStand, true);
    }
}
