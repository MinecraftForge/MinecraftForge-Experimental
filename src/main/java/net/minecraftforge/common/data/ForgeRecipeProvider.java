/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import com.google.gson.JsonElement;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.Advancement.Builder;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.unsafe.UnsafeFieldAccess;
import net.minecraftforge.unsafe.UnsafeHacks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ForgeRecipeProvider extends VanillaRecipeProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final HolderGetter<Item> items;
    private final Map<Item, TagKey<Item>> replacements = HashMap.newHashMap(12);
    private final Set<ResourceKey<Recipe<?>>> excludes = HashSet.newHashSet(16);
    private final UnsafeFieldAccess<ShapelessRecipe, List<Ingredient>> INGREDIENTS = UnsafeHacks.findField(ShapelessRecipe.class, "ingredients");
    private final UnsafeFieldAccess<ShapedRecipe, ShapedRecipePattern> PATTERN = UnsafeHacks.findField(ShapedRecipe.class, "pattern");
    private final UnsafeFieldAccess<Ingredient, HolderSet<Item>> VALUES = UnsafeHacks.findField(Ingredient.class, "values");

    private ForgeRecipeProvider(final BootstrapContext<Recipe<?>> recipeOutput, final BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
        this.output = new Wrapped(this.output);
        this.items = recipeOutput.lookup(Registries.ITEM);
    }

    private void exclude(ItemLike item) {
        exclude(BuiltInRegistries.ITEM.getKey(item.asItem()).toString());
    }

    private void exclude(String name) {
        excludes.add(ResourceKey.create(Registries.RECIPE, Identifier.parse(name)));
    }

    private void replace(ItemLike item, TagKey<Item> tag) {
        replacements.put(item.asItem(), tag);
    }

    @Override
    protected void buildRecipes() {
        replace(Items.STICK, Tags.Items.RODS_WOODEN);
        replace(Items.GOLD_INGOT, Tags.Items.INGOTS_GOLD);
        replace(Items.IRON_INGOT, Tags.Items.INGOTS_IRON);
        replace(Items.NETHERITE_INGOT, Tags.Items.INGOTS_NETHERITE);
        replace(Items.COPPER_INGOT, Tags.Items.INGOTS_COPPER);
        replace(Items.AMETHYST_SHARD, Tags.Items.GEMS_AMETHYST);
        replace(Items.DIAMOND, Tags.Items.GEMS_DIAMOND);
        replace(Items.EMERALD, Tags.Items.GEMS_EMERALD);
        replace(Items.CHEST, Tags.Items.CHESTS_WOODEN);
        replace(Blocks.COBBLESTONE, Tags.Items.COBBLESTONES_NORMAL);
        replace(Blocks.COBBLED_DEEPSLATE, Tags.Items.COBBLESTONES_DEEPSLATE);

        replace(Items.STRING, Tags.Items.STRINGS);
        exclude(getConversionRecipeName(Blocks.WOOL.pick(DyeColor.WHITE), Items.STRING));

        exclude(Blocks.GOLD_BLOCK);
        exclude(Items.GOLD_NUGGET);
        exclude(Blocks.IRON_BLOCK);
        exclude(Items.IRON_NUGGET);
        exclude(Blocks.DIAMOND_BLOCK);
        exclude(Blocks.EMERALD_BLOCK);
        exclude(Blocks.NETHERITE_BLOCK);
        Blocks.COPPER_BLOCK.forEach(this::exclude);
        exclude(Blocks.AMETHYST_BLOCK);

        exclude(Blocks.COBBLESTONE_STAIRS);
        exclude(Blocks.COBBLESTONE_SLAB);
        exclude(Blocks.COBBLESTONE_WALL);
        exclude(Blocks.COBBLED_DEEPSLATE_STAIRS);
        exclude(Blocks.COBBLED_DEEPSLATE_SLAB);
        exclude(Blocks.COBBLED_DEEPSLATE_WALL);

        super.buildRecipes();
    }

    @Nullable
    private Recipe<?> enhance(ResourceKey<Recipe<?>> id, Recipe<?> vanilla) {
        if (vanilla instanceof ShapelessRecipe shapeless)
            return enhance(id, shapeless);
        if (vanilla instanceof ShapedRecipe shaped)
            return enhance(id, shaped);
        return null;
    }

    @Nullable
    private Recipe<?> enhance(ResourceKey<Recipe<?>> id, ShapelessRecipe vanilla) {
        List<Ingredient> ingredients = INGREDIENTS.get(vanilla);
        boolean modified = false;
        for (int x = 0; x < ingredients.size(); x++) {
            Ingredient ing = enhance(id, ingredients.get(x));
            if (ing != null) {
                ingredients.set(x, ing);
                modified = true;
            }
        }
        return modified ? vanilla : null;
    }

    @Nullable
    private Recipe<?> enhance(ResourceKey<Recipe<?>> id, ShapedRecipe vanilla) {
        ShapedRecipePattern pattern = PATTERN.get(vanilla);
        var data = pattern.data().orElseThrow(() -> new IllegalStateException("Weird shaped recipe, data is missing? " + id + " " + vanilla));
        Map<Character, Ingredient> ingredients = data.key();
        boolean modified = false;
        for (Character x : ingredients.keySet()) {
            Ingredient ing = enhance(id, ingredients.get(x));
            if (ing != null) {
                ingredients.put(x, ing);
                modified = true;
            }
        }
        return modified ? vanilla : null;
    }

    @Nullable
    private Ingredient enhance(ResourceKey<Recipe<?>> name, Ingredient vanilla) {
        if (excludes.contains(name))
            return null;

        HolderSet<Item> vanillaItems = VALUES.get(vanilla);
        var unwraped = vanillaItems.unwrap();

        if (unwraped.left().isPresent()) // Already a tag
            return null;

        Ingredient ret = null;
        var items = new ArrayList<Holder<Item>>();
        for (var entry : unwraped.right().get()) {
            var item = entry.get();
            var replacement = replacements.get(item);
            if (replacement != null) {
                if (ret != null) {
                    LOGGER.warn("Failed to enahnce {} ingredient has multiple input items", name);
                    return null;
                }
                ret = Ingredient.of(this.items.getOrThrow(replacement));
            } else
                items.add(entry);
        }

        if (ret != null && !items.isEmpty()) {
            LOGGER.warn("Failed to enahnce {} ingredient has multiple input items", name);
            return null;
        }

        return ret;
    }

    private class Wrapped implements RecipeOutput {
        private final RecipeOutput wrapped;

        private Wrapped(RecipeOutput wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, AdvancementHolder advancement) {
            var modified = ForgeRecipeProvider.this.enhance(id, recipe);
            if (modified != null)
                wrapped.accept(id, modified, null);
        }

        @Override
        public Builder advancement() {
            return wrapped.advancement();
        }

        @Override
        public void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, Identifier advancementId, JsonElement advancement) {
            var modified = ForgeRecipeProvider.this.enhance(id, recipe);
            if (modified != null)
                wrapped.accept(id, modified, null);
        }

        @Override
        public Provider registry() {
            return wrapped.registry();
        }

        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
            return wrapped.lookup(key);
        }

        @Override
        public <S> Stream<Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
            return wrapped.listContextElements(key);
        }
    }
}
