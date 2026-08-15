package io.github.sefiraat.crystamaehistoria.magic.spells.tier1;

import io.github.sefiraat.crystamaehistoria.magic.CastInformation;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.Spell;
import io.github.sefiraat.crystamaehistoria.magic.spells.core.SpellCoreBuilder;
import io.github.sefiraat.crystamaehistoria.slimefun.items.mechanisms.liquefactionbasin.RecipeSpell;
import io.github.sefiraat.crystamaehistoria.stories.definition.StoryType;
import io.github.sefiraat.crystamaehistoria.utils.ParticleUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Slime;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class GrowUp extends Spell {

    public GrowUp() {
        SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(360, false, 5, true, 50, true)
            .makeInstantSpell(this::cast);
        setSpellCore(spellCoreBuilder.build());
    }

    @ParametersAreNonnullByDefault
    public void cast(CastInformation castInformation) {
        Location casterLocation = castInformation.getCastLocation();
        double range = getRange(castInformation);
        for (Entity entity : casterLocation.getWorld().getNearbyEntities(casterLocation, range, range, range)) {
            if (entity instanceof Ageable) {
                Ageable ageable = (Ageable) entity;
                if (!ageable.isAdult()) {
                    ageable.setAdult();
                    ParticleUtils.displayParticleEffect(entity, Particle.SCRAPE, 1, 3);
                }
            } else if (entity instanceof Slime) {
                Slime slime = (Slime) entity;
                // 钳制尺寸上限：原版 API 对越界 size 抛 IllegalArgumentException
                slime.setSize(Math.min(slime.getSize() + 1, 255));
            } else if (entity instanceof Phantom) {
                Phantom phantom = (Phantom) entity;
                phantom.setSize(Math.min(phantom.getSize() + 1, 64));
            }
        }
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.MECHANICAL,
            StoryType.ANIMAL,
            StoryType.VOID
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "生长";
    }

    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "让附近的生物生长",
            "不再是幼年期了!"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "GROW_UP";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.SLIME_BLOCK;
    }
}
