/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting.ingredients;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;

public interface IIngredientSerializer<T extends Ingredient> {
    MapCodec<T> codec();
    StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

    record Simple<Type extends Ingredient>(MapCodec<Type> codec, StreamCodec<RegistryFriendlyByteBuf, Type> streamCodec) implements IIngredientSerializer<Type> { }
    public static <T extends Ingredient> Simple<T> simple(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        return new Simple<>(codec, streamCodec);
    }
}
