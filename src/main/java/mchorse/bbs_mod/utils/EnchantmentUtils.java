package mchorse.bbs_mod.utils;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Since 1.21 enchantments live in a dynamic registry, so {@code EnchantmentHelper} asks for a
 * {@link RegistryEntry} instead of the enchantment itself — which means the level of a fixed
 * enchantment (the riptide the film records) has to be looked up against a world's registries
 * first. Everything here degrades to "not enchanted" instead of throwing, because a recorded
 * take can be played back long after a data pack removed the enchantment it was made with.
 */
public class EnchantmentUtils
{
    public static int getLevel(World world, RegistryKey<Enchantment> key, ItemStack stack)
    {
        if (world == null || stack == null || stack.isEmpty())
        {
            return 0;
        }

        DynamicRegistryManager registries = world.getRegistryManager();
        Optional<Registry<Enchantment>> registry = registries.getOptional(RegistryKeys.ENCHANTMENT);

        if (registry.isEmpty())
        {
            return 0;
        }

        RegistryEntry<Enchantment> entry = registry.get().getEntry(key).orElse(null);

        return entry == null ? 0 : EnchantmentHelper.getLevel(entry, stack);
    }
}
