package io.github.sefiraat.crystamaehistoria.magic.spells.tier1;

import io.github.sefiraat.crystamaehistoria.CrystamaeHistoria;
import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCoreBuilder;
import io.github.sefiraat.crystamaehistoria.runnables.spells.TunnelBoreRunnable;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * 隧道钻探：在施法者上方生成钻探实体，周期清理行进路径上半径内的方块。
 * （原类注释"Removed due to issues"已过时——本法术在 SpellType 中注册且可达。）
 */
public class TunnelBore extends Spell {

    public TunnelBore() {
        SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(50, true, 1, true, 30, true)
            .makeInstantSpell(this::cast);
        setSpellCore(spellCoreBuilder.build());
    }

    @ParametersAreNonnullByDefault
    public void cast(CastInformation castInformation) {
        final UUID caster = castInformation.getCaster();
        final Location location = castInformation.getCastLocation();
        final Vector direction = location.getDirection().clone();
        final int range = (int) getRange(castInformation);
        direction.setY(0);
        final Location spawnLocation = location.clone().add(0, range, 0);
        final Endermite bore = (Endermite) spawnLocation.getWorld().spawnEntity(
            spawnLocation,
            EntityType.ENDERMITE,
            CreatureSpawnEvent.SpawnReason.COMMAND,
            entity -> {
                Endermite mite = (Endermite) entity;
                mite.setGravity(false);
                mite.setInvulnerable(true);
                mite.setInvisible(true);
                mite.setVelocity(location.getDirection().multiply(2));
            }
        );
        TunnelBoreRunnable runnable = new TunnelBoreRunnable(bore, range, caster, range * 20);
        runnable.runTaskTimer(CrystamaeHistoria.getInstance(), 0, 1);
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.ELEMENTAL,
            StoryType.ALCHEMICAL,
            StoryType.ANIMAL
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "隧道钻机";
    }
    
    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "使用强大的魔法",
            "朝你面对的方向开凿出一条隧道",
            "但是不会掉落任何物品"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "TUNNEL_BORE";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.GOAT_SPAWN_EGG;
    }

}
