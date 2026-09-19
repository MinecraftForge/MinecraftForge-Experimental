/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.crafting.conditions.ConditionCodec;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ICondition.IContext;
import net.minecraftforge.common.crafting.conditions.OrCondition;
import net.minecraftforge.common.crafting.conditions.TrueCondition;
import org.jetbrains.annotations.Nullable;

/**
 * So, A 'ConditionalRecipe' differs from all normal recipes in the fact that in addition to the conditions
 * disabling the entire recipe, it has sub-recipes that themselves have conditions.
 *
 * And when being deserialized it returns the first entry that passes the conditional check.
 * This basically means that you can have multiple variants all use the same recipe name, and
 * only one will ever be loaded.
 *
 * This also means that you can wrap ALL recipes in a Conditional even those that don't explicitly
 * have support for them in their data gen.
 */
public class ConditionalRecipe implements Recipe<RecipeInput> {
    public static Builder builder(RecipeOutput output) {
        return new Builder(output);
    }

    private final @Nullable ICondition mainCondition;
    private final List<InnerRecipe> children;
    private final Recipe<RecipeInput> first;

    @SuppressWarnings("unchecked")
    private ConditionalRecipe(@Nullable ICondition mainCondition, List<InnerRecipe> children) {
        this.mainCondition = mainCondition;
        this.children = children;
        this.first = (Recipe<RecipeInput>)children.getFirst().recipe();
    }

    public static class Builder {
        private final List<InnerRecipe> recipes = new ArrayList<>();
        private final List<InnerAdvancement> advancements = new ArrayList<>();
        private final Bouncer bouncer;

        @Nullable private ICondition condition;
        @Nullable private ICondition mainCondition;
        @Nullable private Identifier advancementId;
        @Nullable private RecipeCategory category = null;

        private Builder(RecipeOutput output) {
            this.bouncer = new Bouncer(output, this);
        }

        public Builder category(RecipeCategory category) {
            this.category = category;
            return this;
        }

        public Builder mainCondition(ICondition value) {
            if (this.mainCondition != null)
                throw new IllegalStateException("Attempted to overrride the main condition, only one is allowed to be set");
            this.mainCondition = value;
            return this;
        }

        public Builder condition(ICondition value) {
            if (this.condition != null)
                throw new IllegalStateException("Attempted to override a previous set condition before adding a recipe");
            this.condition = value;
            return this;
        }

        public Builder recipe(Consumer<RecipeOutput> callable) {
            callable.accept(bouncer);
            return this;
        }

        public Builder recipe(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable AdvancementHolder advancement) {
            if (condition == null)
                throw new IllegalStateException("Can not add a recipe with no conditions.");
            recipes.add(new InnerRecipe(this.condition, recipe));
            if (advancement != null)
                advancements.add(new InnerAdvancement(this.condition, advancement, null));
            condition = null;
            return this;
        }

        public Builder advancement(Identifier id) {
            this.advancementId = id;
            return this;
        }

        public void save(RecipeOutput out, String namespace, String path) {
            save(out, Identifier.fromNamespaceAndPath(namespace, path));
        }

        public void save(RecipeOutput out, Identifier id) {
            if (condition != null)
                throw new IllegalStateException("Invalid ConditionalRecipe builder, Orphaned conditions");

            if (recipes.isEmpty())
                throw new IllegalStateException("Invalid ConditionalRecipe builder, No recipes");

            AdvancementHolder advancement = null;
            if (!advancements.isEmpty()) {
                var adv = ConditionalAdvancement.builder();
                for (var data : advancements) {
                    adv.condition(data.condition());
                    adv.advancement(data.advancement());
                }
                if (advancementId == null) {
                    if (this.category == null)
                        advancementId = id.withPrefix("recipes/");
                    else
                        advancementId = id.withPrefix("recipes/" + category.getFolderName() + '/');
                }
                advancement = adv.build(advancementId);
            }

            out.accept(ResourceKey.create(Registries.RECIPE, id), new ConditionalRecipe(mainCondition, recipes), advancement);
        }

        private record Bouncer(RecipeOutput wrapped, Builder builder) implements RecipeOutput {
            @Override
            public void accept(ResourceKey<Recipe<?>> id, Recipe<?> value, @Nullable AdvancementHolder advancement) {
                builder.recipe(id, value, advancement);
            }

            @Override
            public Advancement.Builder advancement() {
                return wrapped.advancement();
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
                return wrapped.lookup(key);
            }

            @SuppressWarnings("deprecation")
            @Override
            public <S> Stream<Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
                return wrapped.listContextElements(key);
            }

            @Override
            public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
                return wrapped.registryLookup(registry);
            }
        }
    }

    private record InnerRecipe(ICondition condition, Recipe<?> recipe) {}
    private record InnerAdvancement(ICondition condition, AdvancementHolder advancement, JsonObject json) {}

    // In order to not error during data loading when all elements are filtered out, we need to add an outer condition so we can read it from the top level
    static <T> @Nullable ICondition aggregate(@Nullable ICondition main, List<T> conditionals, Function<T, ICondition> getter) {
        if (main != null)
            return main;
        if (conditionals.isEmpty())
            return null;
        if (conditionals.size() == 1)
            return getter.apply(conditionals.getFirst());

        var list = new ArrayList<ICondition>(conditionals.size());
        for (var entry : conditionals) {
            var condition = getter.apply(entry);
            if (condition == null || condition == TrueCondition.INSTANCE)
                return null;
            list.add(condition);
        }
        return new OrCondition(list);
    }

    private static final MapCodec<Recipe<?>> CODEC = Codec.of(new MapEncoder.Implementation<>() {
        @Override
        public <T> RecordBuilder<T> encode(Recipe<?> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            if (!(input instanceof ConditionalRecipe))
                new IllegalStateException("ConditionalRecipe.CODEC can only be used for ConditionRecipes, how did you get here?");

            var wrapper = (ConditionalRecipe)input;

            var outerCondition = aggregate(wrapper.mainCondition, wrapper.children, InnerRecipe::condition);
            if (outerCondition != null)
                prefix.add(ICondition.DEFAULT_FIELD, ICondition.CODEC.encodeStart(ops, outerCondition));

            var recipes = ops.listBuilder();
            for (var recipe : wrapper.children) {
                var map = ops.mapBuilder();
                if (wrapper.mainCondition != null || wrapper.children.size() != 1)
                    map.add(ICondition.DEFAULT_FIELD, recipe.condition(), ICondition.CODEC);
                map.add("recipe", recipe.recipe(), Recipe.DIRECT_CODEC);
                recipes.add(map.build(ops.emptyMap()));
            }
            prefix.add("recipes", recipes.build(ops.emptyList()));
            return prefix;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(ICondition.DEFAULT_FIELD), ops.createString("recipes"));
        }
    }, new MapDecoder.Implementation<Recipe<?>>() {
        @Override
        public <T> DataResult<Recipe<?>> decode(DynamicOps<T> ops, MapLike<T> input) {
            var context = ConditionCodec.getContext(ops);
            return ops.getStream(input.get("recipes")).flatMap(stream -> {
                var count = new Holder<Integer>();
                count.value = -1;
                var ret = stream.map(entry -> accept(context, ops, count, entry))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

                if (ret != null)
                    return ret;

                return DataResult.error(() -> "No recipe passed conditions, if this is the case, you should have an outer condition.");
            });
        }

        private <T> DataResult<Recipe<?>> accept(IContext context, DynamicOps<T> ops, Holder<Integer> count, T entry) {
            count.value = count.value + 1;
            var map = ops.getMap(entry).result().orElse(null);
            if (map == null)
                return DataResult.error(() -> "Entry " + count.value + " was not MapLike " + entry.getClass());

            if (map.get(ICondition.DEFAULT_FIELD) != null) {
                var parsed = ICondition.SAFE_CODEC.parse(ops, (T)map.get(ICondition.DEFAULT_FIELD));
                if (parsed.result().isPresent()) {
                    var condition = parsed.result().get();
                    if (!condition.test(context, ops))
                        return null;
                }
            }

            var recipe = map.get("recipe");
            if (recipe == null)
                return DataResult.error(() -> "Missing `recipe` entry " + count.value);

            var ret = Recipe.DIRECT_CODEC.parse(ops, (T)recipe);
            return ret;
        }

        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.of(ops.createString(ICondition.DEFAULT_FIELD), ops.createString("recipes"));
        }
    });

    private static final StreamCodec<RegistryFriendlyByteBuf, ConditionalRecipe> STREAM_CODEC = StreamCodec.of(
        (_, _) -> new UnsupportedOperationException("ConditionaRecipe.SERIALIZER does not support encoding to network"),
        _ -> { throw new UnsupportedOperationException("ConditionaRecipe.SERIALIZER does not support encoding to network"); }
    );

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static MapCodec<ConditionalRecipe> typedCodec() {
        return (MapCodec<ConditionalRecipe>)(MapCodec)CODEC;
    }
    public static final RecipeSerializer<ConditionalRecipe> SERIALZIER = new RecipeSerializer<>(typedCodec(), STREAM_CODEC);

    private static final class Holder<T> {
        private T value;
    }

    @Override
    public RecipeSerializer<ConditionalRecipe> getSerializer() {
        return SERIALZIER;
    }

    // This should never happen, as we're just doing this during data gen.
    // But we need to have a full Recipe object in order for datagen to work.
    @Override
    public boolean matches(RecipeInput input, Level level) {
        return this.first.matches(input, level);
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return this.first.assemble((RecipeInput)input);
    }

    @Override
    public boolean showNotification() {
        return this.first.showNotification();
    }

    @Override
    public String group() {
        return this.first.group();
    }

    @SuppressWarnings("unchecked")
    @Override
    public RecipeType<Recipe<RecipeInput>> getType() {
        return (RecipeType<Recipe<RecipeInput>>)this.first.getType();
    }

    @Override
    public PlacementInfo placementInfo() {
        return this.first.placementInfo();
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return this.first.recipeBookCategory();
    }
}
