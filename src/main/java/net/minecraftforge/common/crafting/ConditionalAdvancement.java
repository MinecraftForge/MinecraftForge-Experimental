/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.crafting.conditions.ConditionCodec;
import net.minecraftforge.common.crafting.conditions.ICondition;

/**
 * A `ConditionalAdvancement` is a single advancement file that contains multiple advancements, each having a condition.
 * When loaded it will return the first advancement that the conditions pass.
 *
 * This allows for multiple variants of an advancement to share the same name in the registry. Which allows dependents
 * to reference it without having to care about the conditions themselves.
 *
 * This is most likely useful when you have variants of a recipe based on what mods/resources are installed but want
 * to maintain the same 'entry' in the advancement book.
 */
public record ConditionalAdvancement(ICondition condition, Optional<Advancement> child) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static final Identifier DOESNT_MATTER = Identifier.fromNamespaceAndPath("doesnt", "matter");

        private List<ConditionalAdvancement> advancements = new ArrayList<>();
        private ICondition condition;

        public Builder condition(ICondition value) {
            this.condition = value;
            return this;
        }

        public Builder advancement(Consumer<Consumer<Advancement.Builder>> callable) {
            callable.accept(this::advancement);
            return this;
        }

        public Builder advancement(Advancement.Builder builder) {
            return advancement(builder.build(DOESNT_MATTER).value());
        }

        public Builder advancement(AdvancementHolder holder) {
            return advancement(holder.value());
        }

        private Builder advancement(Advancement value) {
            if (condition == null)
                throw new IllegalStateException("Can not add a advancement with no conditions.");

            if (value == null)
                throw new IllegalStateException("Can not add a null advancement");

            this.advancements.add(new ConditionalAdvancement(this.condition, Optional.of(value)));
            this.condition = null;

            return this;
        }

        public AdvancementHolder build(final Identifier id) {
            if (this.advancements.isEmpty())
                throw new IllegalStateException("Can not build an empty ConditionalAdvancement");

            var list = new ArrayList<ConditionalAdvancement>();
            Advancement root = null;
            for (var child : advancements) {
                if (root == null) {
                    var adv = child.child.get();
                    root = new Advancement(
                        adv.parent(),
                        adv.display(),
                        adv.rewards(),
                        adv.criteria(),
                        adv.requirements(),
                        adv.sendsTelemetryEvent(),
                        adv.name(),
                        Optional.of(list)
                    );
                    list.add(new ConditionalAdvancement(child.condition, Optional.empty()));
                } else {
                    list.add(child);
                }
            }

            return new AdvancementHolder(id, root);
        }
    }



    private static final String KEY = "forge:children";
    public static final Codec<Advancement> wrapCodec(MapCodec<Advancement> vanilla) {
        var vanillaCodec = vanilla.codec();
        var childCodec = RecordCodecBuilder.<ConditionalAdvancement>create(i -> i.group(
            ICondition.SAFE_CODEC.fieldOf(ICondition.DEFAULT_FIELD).forGetter(ConditionalAdvancement::condition),
            vanillaCodec.optionalFieldOf("child").forGetter(ConditionalAdvancement::child)
        ).apply(i, ConditionalAdvancement::new)).listOf();

        return Codec.of(new MapEncoder.Implementation<Advancement>() {
            @Override
            public <T> RecordBuilder<T> encode(Advancement input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                var root = vanilla.encode(input, ops, prefix);
                if (!input.forgeConditions().isPresent())
                    return root;

                var children = input.forgeConditions().get();
                if (children.isEmpty())
                    return root;

                if (children.size() != 1)
                    root.add(KEY, childCodec.encodeStart(ops, children));

                var outerCondition = ConditionalRecipe.aggregate(null, children, ConditionalAdvancement::condition);
                if (outerCondition != null)
                    root.add(ICondition.DEFAULT_FIELD, ICondition.CODEC.encodeStart(ops, outerCondition));

                return root;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.concat(vanilla.keys(ops), List.of(ops.createString(KEY)).stream());
            }
        }, new MapDecoder.Implementation<Advancement>() {
            @Override
            public <T> DataResult<Advancement> decode(DynamicOps<T> ops, MapLike<T> input) {
                var root = vanilla.decode(ops, input);
                var children = input.get(KEY);
                if (children == null)
                    return root;

                var context = ConditionCodec.getContext(ops);
                return ops.getStream(children).flatMap(stream -> {
                    var count = new AtomicInteger();
                    var ret = stream.map(entry -> accept(context, ops, count, entry, root))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);

                    if (ret != null)
                        return ret;

                    return DataResult.error(() -> "No advancement passed conditions, if this is the case, you should have an outer condition.");
                });
            }

            private <T> DataResult<Advancement> accept(ICondition.IContext context, DynamicOps<T> ops, AtomicInteger count, T entry, DataResult<Advancement> root) {
                count.getAndIncrement();
                var map = ops.getMap(entry).result().orElse(null);
                if (map == null)
                    return DataResult.error(() -> "Entry " + count.get() + " was not MapLike " + entry.getClass());

                if (map.get(ICondition.DEFAULT_FIELD) != null) {
                    var parsed = ICondition.SAFE_CODEC.parse(ops, (T)map.get(ICondition.DEFAULT_FIELD));
                    if (parsed.result().isPresent()) {
                        var condition = parsed.result().get();
                        if (!condition.test(context, ops))
                            return null;
                    }
                }

                var child = map.get("child");
                if (child != null)
                    return vanillaCodec.parse(ops, (T)child);
                return root;
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.concat(vanilla.keys(ops), List.of(ops.createString(KEY)).stream());
            }
        }).codec();
    }

}
