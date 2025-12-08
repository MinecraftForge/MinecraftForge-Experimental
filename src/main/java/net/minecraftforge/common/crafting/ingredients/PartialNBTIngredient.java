/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Ingredient that matches the given items, performing a partial NBT match. Use {@link StrictNBTIngredient} if you want exact match on NBT */
public class PartialNBTIngredient extends AbstractIngredient {
    public static Builder builder() {
        return new Builder();
    }

    private final CompoundTag nbt;
    private final NbtPredicate predicate;

    private PartialNBTIngredient(HolderSet<Item> items, CompoundTag nbt) {
        super(items);

        if (items.size() == 0)
            throw new IllegalArgumentException("Cannot create a PartialNBTIngredient with no items");

        this.nbt = nbt;
        this.predicate = new NbtPredicate(nbt);
    }

    /** Creates a new ingredient matching any item from the list, containing the given NBT */
    @SuppressWarnings("deprecation")
    public static PartialNBTIngredient of(CompoundTag nbt, ItemLike... items) {
        return of(
            HolderSet.direct(
                Arrays.stream(items)
                    .map(ItemLike::asItem)
                    .map(Item::builtInRegistryHolder)
                    .toList()
            ),
            nbt);
    }

    /** Creates a new ingredient matching the given item, containing the given NBT  */
    @SuppressWarnings("deprecation")
    public static PartialNBTIngredient of(ItemLike item, CompoundTag nbt) {
        return of(HolderSet.direct(item.asItem().builtInRegistryHolder()), nbt);
    }

    /** Creates a new ingredient matching the given item, containing the given NBT  */
    public static PartialNBTIngredient of(HolderSet<Item> items, CompoundTag nbt) {
        return new PartialNBTIngredient(items, nbt);
    }

    @Override
    public boolean test(@Nullable ItemStack input) {
        if (input == null)
            return false;
        var nbt = input.get(DataComponents.CUSTOM_DATA);
        return nbt != null && input.is(this.values) && predicate.matches(nbt.copyTag());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> serializer() {
        return SERIALIZER;
    }

    public static final MapCodec<PartialNBTIngredient> CODEC = RecordCodecBuilder.mapCodec(builder ->
        builder.group(
            Ingredient.NON_AIR_HOLDER_SET_CODEC.fieldOf("items").forGetter(i -> i.values),
            TagParser.FLATTENED_CODEC.fieldOf("nbt").forGetter(i -> i.nbt)
        ).apply(builder, PartialNBTIngredient::new)
    );

    public static final IIngredientSerializer<PartialNBTIngredient> SERIALIZER = new IIngredientSerializer<>() {
        private final StreamCodec<RegistryFriendlyByteBuf, HolderSet<Item>> HOLDER_SET = ByteBufCodecs.holderSet(Registries.ITEM);

        @Override
        public MapCodec<? extends PartialNBTIngredient> codec() {
            return CODEC;
        }

        @Override
        public PartialNBTIngredient read(RegistryFriendlyByteBuf buffer) {
            var items = HOLDER_SET.decode(buffer);
            var nbt = buffer.readNbt();
            return new PartialNBTIngredient(items, Objects.requireNonNull(nbt));
        }

        @Override
        public void write(RegistryFriendlyByteBuf buffer, PartialNBTIngredient value) {
            HOLDER_SET.encode(buffer, value.values);
            buffer.writeNbt(value.nbt);
        }
    };

    public static class Builder {
        private final List<ItemLike> items = new ArrayList<>();
        private CompoundTag nbt;

        public Builder nbt(CompoundTag value) {
            if (this.nbt != null)
                throw new IllegalStateException("NBT Tag already set");
            this.nbt = value;
            return this;
        }

        public Builder item(ItemLike item) {
            this.items.add(item);
            return this;
        }

        public Builder items(ItemLike... values) {
            for (var item : values)
                this.items.add(item);
            return this;
        }

        public PartialNBTIngredient build() {
            if (nbt == null)
                throw new IllegalStateException("NBT Data not set");
            if (items.isEmpty())
                throw new IllegalStateException("No items added");

            return PartialNBTIngredient.of(nbt, items.stream().toArray(ItemLike[]::new));
        }
    }
}
