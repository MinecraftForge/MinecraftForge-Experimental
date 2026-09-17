/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.SingleRegistryBootstrap;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.predicates.CompositeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.InvertedLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.loot.CanToolPerformAction;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

/**
 * Currently used only for replacing shears item to shears_dig tool action
 */
public final class ForgeLootTableProvider extends LootTableProvider {
    private static final String POOLS = "pools"; // LootTable.Builder.pools
    private static final String ENTRIES = "entries"; // LootPool.entries
    private static final String CONDITIONS = "conditions"; // LootPool.conditions
    private static final String CHILDREN = "children"; // CompositeEntryBase.children
    private static final String ENTRY_CONDITION = "conditions"; // LootPoolEntryContainer.conditions
    private static final String TERMS = "terms"; // CompositeLootItemCondition.terms

    public static SingleRegistryBootstrap<LootTable> create() {
        var vanilla = (LootTableProvider)VanillaLootTableProvider.create();

        var providers = new ArrayList<SubProviderEntry>(vanilla.getTables().size());
        for (var provider : vanilla.getTables())
            providers.add(new SubProviderEntry(context -> provider.bootstrap().create(new Wrapper(context)), provider.paramSet()));

        return new ForgeLootTableProvider(vanilla.getRequired(), providers);
    }

    private ForgeLootTableProvider(final Set<ResourceKey<LootTable>> requiredTables, final List<LootTableProvider.SubProviderEntry> subProviders) {
        super(requiredTables, subProviders);
    }

    private record Wrapper(LootTableSubProvider.Context wrapped) implements LootTableSubProvider.Context {
        @Override
        public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> key) {
            return wrapped.lookup(key);
        }

        @Override
        public <S> Stream<Reference<S>> listContextElements(ResourceKey<? extends Registry<? extends S>> key) {
            return wrapped.listContextElements(key);
        }

        @Override
        public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
            return wrapped.registryLookup(registry);
        }

        @Override
        public Reference<LootTable> accept(ResourceKey<LootTable> key, Builder value) {
            boolean modified = findAndReplaceInLootTableBuilder(value, Items.SHEARS, ToolActions.SHEARS_DIG);
            if (modified)
                return wrapped.accept(key, value);
            return wrapped.lookup(Registries.LOOT_TABLE).getOrThrow(key); // Return the vanilla reference if we didn't change anything
        }

    }

    private static boolean findAndReplaceInLootTableBuilder(LootTable.Builder builder, Item from, ToolAction toolAction) {
        ImmutableList.Builder<LootPool> lootPools = ObfuscationReflectionHelper.getPrivateValue(LootTable.Builder.class, builder, POOLS);
        boolean found = false;

        if (lootPools == null) {
            throw new IllegalStateException(LootTable.Builder.class.getName() + " is missing field " + POOLS);
        }

        for (LootPool lootPool : lootPools.build()) {
            if (findAndReplaceInLootPool(lootPool, from, toolAction)) {
                found = true;
            }
        }

        return found;
    }

    private static boolean findAndReplaceInLootPool(LootPool lootPool, Item from, ToolAction toolAction) {
        List<LootPoolEntryContainer> lootEntries = ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, ENTRIES);
        List<LootItemCondition> lootConditions = ObfuscationReflectionHelper.getPrivateValue(LootPool.class, lootPool, CONDITIONS);
        boolean found = false;

        if (lootEntries == null)
            throw new IllegalStateException(LootPool.class.getName() + " is missing field " + ENTRIES);

        for (LootPoolEntryContainer lootEntry : lootEntries) {
            if (findAndReplaceInLootEntry(lootEntry, from, toolAction))
                found = true;

            if (lootEntry instanceof CompositeEntryBase) {
                if (findAndReplaceInParentedLootEntry((CompositeEntryBase) lootEntry, from, toolAction))
                    found = true;
            }
        }

        if (lootConditions == null)
            throw new IllegalStateException(LootPool.class.getName() + " is missing field " + CONDITIONS);
        else {
            lootConditions = new ArrayList<>(lootConditions);
            ObfuscationReflectionHelper.setPrivateValue(LootPool.class,  lootPool, lootConditions, CONDITIONS);
        }

        for (int i = 0; i < lootConditions.size(); i++) {
            LootItemCondition lootCondition = lootConditions.get(i);
            if (lootCondition instanceof MatchTool matchTool && checkMatchTool(matchTool, from)) {
                lootConditions.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
                found = true;
            } else if (lootCondition instanceof InvertedLootItemCondition inverted) {
                LootItemCondition invLootCondition = inverted.term().get();

                if (invLootCondition instanceof MatchTool matchTool && checkMatchTool(matchTool, from)) {
                    lootConditions.set(i, InvertedLootItemCondition.invert(CanToolPerformAction.canToolPerformAction(toolAction)).build());
                    found = true;
                } else if (invLootCondition instanceof CompositeLootItemCondition compositeLootItemCondition && findAndReplaceInComposite(compositeLootItemCondition, from, toolAction)) {
                    found = true;
                }
            } else if (lootCondition instanceof CompositeLootItemCondition compositeLootItemCondition && findAndReplaceInComposite(compositeLootItemCondition, from, toolAction)) {
                found = true;
            }
        }

        return found;
    }

    private static boolean findAndReplaceInParentedLootEntry(CompositeEntryBase entry, Item from, ToolAction toolAction) {
        List<LootPoolEntryContainer> lootEntries = ObfuscationReflectionHelper.getPrivateValue(CompositeEntryBase.class, entry, CHILDREN);
        boolean found = false;

        if (lootEntries == null) {
            throw new IllegalStateException(CompositeEntryBase.class.getName() + " is missing field " + CHILDREN);
        }

        for (LootPoolEntryContainer lootEntry : lootEntries) {
            if (findAndReplaceInLootEntry(lootEntry, from, toolAction)) {
                found = true;
            }
        }

        return found;
    }

    private static boolean findAndReplaceInLootEntry(LootPoolEntryContainer entry, Item from, ToolAction toolAction) {
        List<LootItemCondition> lootConditions = ObfuscationReflectionHelper.getPrivateValue(LootPoolEntryContainer.class, entry, ENTRY_CONDITION);
        boolean found = false;

        if (lootConditions == null)
            throw new IllegalStateException(LootPoolEntryContainer.class.getName() + " is missing field f_7963" + "6_");
        else {
            lootConditions = new ArrayList<>(lootConditions);
            ObfuscationReflectionHelper.setPrivateValue(LootPoolEntryContainer.class, entry, lootConditions, ENTRY_CONDITION);
        }

        for (int i = 0; i < lootConditions.size(); i++) {
            var condition = lootConditions.get(i);
            if (condition instanceof CompositeLootItemCondition composite && findAndReplaceInComposite(composite, from, toolAction)) {
                found = true;
            } else if (condition instanceof MatchTool matchTool && checkMatchTool(matchTool, from)) {
                lootConditions.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
                found = true;
            }
        }

        return found;
    }

    private static boolean findAndReplaceInComposite(CompositeLootItemCondition alternative, Item from, ToolAction toolAction) {
        List<LootItemCondition> lootConditions = ObfuscationReflectionHelper.getPrivateValue(CompositeLootItemCondition.class, alternative, TERMS);
        boolean found = false;

        if (lootConditions == null)
            throw new IllegalStateException(CompositeLootItemCondition.class.getName() + " is missing field " + TERMS);
        else {
            lootConditions = new ArrayList<>(lootConditions);
            ObfuscationReflectionHelper.setPrivateValue(CompositeLootItemCondition.class, alternative, lootConditions, TERMS);
        }

        for (int i = 0; i < lootConditions.size(); i++) {
            if (lootConditions.get(i) instanceof MatchTool matchTool && checkMatchTool(matchTool, from)) {
                lootConditions.set(i, CanToolPerformAction.canToolPerformAction(toolAction).build());
                found = true;
            }
        }

        return found;
    }

    @SuppressWarnings("deprecation")
    private static boolean checkMatchTool(MatchTool lootCondition, Item expected) {
        return lootCondition.predicate().flatMap(ItemPredicate::items).filter(s -> s.contains(expected.builtInRegistryHolder())).isPresent();
    }
}
