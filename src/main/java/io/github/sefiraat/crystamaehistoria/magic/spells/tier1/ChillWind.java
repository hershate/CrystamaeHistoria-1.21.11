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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public class ChillWind extends Spell {

    public ChillWind() {
        SpellCoreBuilder spellCoreBuilder = new SpellCoreBuilder(120, true, 7, false, 25, true)
            .makeTickingSpell(this::cast, 20, true, 5, false)
            .makeEffectingSpell(false, false)
            .addNegativeEffect(PotionEffectType.SLOWNESS, 4, 1)
            .addNegativeEffect(PotionEffectType.MINING_FATIGUE, 4, 1);
        setSpellCore(spellCoreBuilder.build());
    }

    @ParametersAreNonnullByDefault
    public void cast(CastInformation castInformation) {
        final Location location = castInformation.getCasterAsPlayer().getLocation().clone().add(0, 1, 0);
        final double range = getRange(castInformation);
        final double effectRange = range * 0.75;
        final int density = 20;
        // Particles（单次克隆的可复用坐标：原实现每点一次 clone().add，~840 点/次施法）
        final Location point = location.clone();
        final double baseX = location.getX();
        final double baseY = location.getY();
        final double baseZ = location.getZ();
        for (double height = 0; height <= Math.PI; height += Math.PI / density) {
            final double r = range * Math.sin(height);
            final double y = range * Math.cos(height);
            for (double a = 0; a < Math.PI * 2; a += Math.PI / density) {
                final double x = Math.cos(a) * r;
                final double z = Math.sin(a) * r;
                point.setX(baseX + x);
                point.setY(baseY + y);
                point.setZ(baseZ + z);
                ParticleUtils.displayParticleEffect(point, Particle.END_ROD, 0.1, 1);
            }
        }
        // Effects
        for (Entity entity : location.getWorld().getNearbyEntities(location, effectRange, effectRange, effectRange)) {
            if (entity instanceof LivingEntity
                && !entity.getUniqueId().equals(castInformation.getCaster())
            ) {
                LivingEntity livingEntity = (LivingEntity) entity;
                livingEntity.setFreezeTicks(Math.min(livingEntity.getMaxFreezeTicks(), livingEntity.getFreezeTicks() + 20));
                if (livingEntity.getFreezeTicks() == livingEntity.getMaxFreezeTicks()) {
                    applyNegativeEffects(livingEntity, castInformation);
                }
            }
        }
    }

    @Nonnull
    @Override
    public RecipeSpell getRecipe() {
        return new RecipeSpell(
            1,
            StoryType.ELEMENTAL,
            StoryType.ALCHEMICAL,
            StoryType.VOID
        );
    }

    @Nonnull
    @Override
    public String getName() {
        return "冰冻领域";
    }

    @Nonnull
    @Override
    public String[] getLore() {
        return new String[]{
            "施法者在周围展开冰冻领域",
            "可以冻结附近的生物"
        };
    }

    @Nonnull
    @Override
    public String getId() {
        return "CHILL_WIND";
    }

    @Nonnull
    @Override
    public Material getMaterial() {
        return Material.BLUE_ICE;
    }
}
