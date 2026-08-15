package io.github.sefiraat.crystamaehistoria.runnables.spells;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.logging.Level;

public class SpellTickRunnable extends BukkitRunnable {

    private final CastInformation castInformation;
    private int numberOfRuns;

    @ParametersAreNonnullByDefault
    public SpellTickRunnable(CastInformation castInformation, int numberOfRuns) {
        this.castInformation = castInformation;
        this.numberOfRuns = numberOfRuns;
    }

    @Override
    public void run() {
        // 施法者已下线时终止法术：召唤物/飞行等效果在玩家退出时同样会被清理，
        // 且大量 tick 回调直接链式引用施法者（getCasterAsPlayer().getLocation()），离线时会 NPE
        if (Bukkit.getPlayer(castInformation.getCaster()) == null) {
            this.cancel();
            return;
        }
        if (numberOfRuns <= 0) {
            try {
                castInformation.runAfterTicksEvent();
            } catch (Exception e) {
                CrystamaeHistoria.getInstance().getLogger()
                    .log(Level.WARNING, "法术收尾回调执行异常，该次施法已终止", e);
            }
            this.cancel();
        } else {
            try {
                castInformation.runTickEvent();
            } catch (Exception e) {
                // 断路器：tick 回调抛异常时终止该次施法，避免剩余 tick 内每周期重复抛异常刷爆日志
                CrystamaeHistoria.getInstance().getLogger()
                    .log(Level.WARNING, "法术 tick 回调执行异常，该次施法已终止", e);
                this.cancel();
                return;
            }
        }
        numberOfRuns--;
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        CrystamaeHistoria.getSpellMemory().getTickingCastables().remove(this);
    }

}
