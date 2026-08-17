package io.github.sefiraat.crystamaehistoria.slimefun.items.exhalted;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import org.bukkit.Location;
import org.bukkit.entity.Animals;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class ExaltedFertilityPharo extends ExaltedItem {

    private final int range;

    public ExaltedFertilityPharo(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int range) {
        super(itemGroup, item, recipeType, recipe);
        this.range = range;
    }

    @Override
    public void onExalt(ExaltedItem slimefunItem, Location location) {
        // getNearbyEntitiesByType 返回 Collection：原 stream().toList() 为
        // 每 tick 二次复制 + List 转型；直接迭代取第 rnd 个元素零复制
        // （该路径由展示架 afterTick 周期驱动），均匀随机语义不变
        final Collection<Animals> animals = location.getNearbyEntitiesByType(Animals.class, this.range, this.range);
        if (!animals.isEmpty()) {
            final int rnd = ThreadLocalRandom.current().nextInt(animals.size());
            int index = 0;
            for (Animals animal : animals) {
                if (index++ == rnd) {
                    animal.setLoveModeTicks(100);
                    break;
                }
            }
        }
    }
}
