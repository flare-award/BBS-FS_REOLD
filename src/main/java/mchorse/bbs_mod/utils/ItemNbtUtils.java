package mchorse.bbs_mod.utils;

import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Item NBT moved onto data components in 1.20.5: the stack's free-form tag is now the
 * {@code minecraft:custom_data} component and the block entity payload a {@code BlockItem} applies
 * is the {@code minecraft:block_entity_data} one. Components are also immutable — reading one,
 * editing the compound and writing it back is the only way to change them — so every helper here
 * does that round trip in one call.
 */
public class ItemNbtUtils
{
    /** The stack's custom data compound (a copy), or an empty one when it carries none. */
    public static NbtCompound getCustomData(ItemStack stack)
    {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);

        return component == null ? new NbtCompound() : component.getNbt().copy();
    }

    public static void setCustomData(ItemStack stack, NbtCompound nbt)
    {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /** Writes one entry into the custom data compound, preserving the rest. */
    public static void putCustomData(ItemStack stack, String key, NbtElement value)
    {
        NbtCompound nbt = getCustomData(stack);

        nbt.put(key, value);

        setCustomData(stack, nbt);
    }

    /** The block entity payload a {@code BlockItem} writes into the placed block (a copy). */
    public static NbtCompound getBlockEntityData(ItemStack stack)
    {
        NbtComponent component = stack.get(DataComponentTypes.BLOCK_ENTITY_DATA);

        return component == null ? new NbtCompound() : component.getNbt().copy();
    }

    public static void setBlockEntityData(ItemStack stack, NbtCompound nbt)
    {
        stack.set(DataComponentTypes.BLOCK_ENTITY_DATA, NbtComponent.of(nbt));
    }

    /** Writes one entry into the block entity payload, preserving the rest. */
    public static void putBlockEntityData(ItemStack stack, String key, NbtElement value)
    {
        NbtCompound nbt = getBlockEntityData(stack);

        nbt.put(key, value);

        setBlockEntityData(stack, nbt);
    }

    /**
     * The stack's components in the bracket syntax {@code /give} takes, or an empty string when it
     * is a plain item. Only what differs from the item's defaults is listed, so the command stays
     * short.
     */
    public static String toGiveComponents(ItemStack stack)
    {
        ComponentChanges changes = stack.getComponentChanges();

        if (changes.isEmpty())
        {
            return "";
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");

        for (Map.Entry<ComponentType<?>, Optional<?>> entry : changes.entrySet())
        {
            Identifier id = Registries.DATA_COMPONENT_TYPE.getId(entry.getKey());

            if (id == null || entry.getValue().isEmpty())
            {
                continue;
            }

            joiner.add(id + "=" + encode(entry.getKey(), entry.getValue().get()));
        }

        String components = joiner.toString();

        return "[]".equals(components) ? "" : components;
    }

    @SuppressWarnings("unchecked")
    private static <T> String encode(ComponentType<T> type, Object value)
    {
        return type.getCodecOrThrow()
            .encodeStart(NbtOps.INSTANCE, (T) value)
            .result()
            .map(NbtElement::toString)
            .orElse(String.valueOf(value));
    }
}
