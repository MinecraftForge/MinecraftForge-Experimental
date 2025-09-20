/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.fml.event.IModBusEvent;

import java.util.function.Consumer;

/**
 * Fired on {@link PackRepository} creation to allow mods to add new pack finders.
 *
 * @param getPackType the {@link PackType} of the pack repository being constructed.
 */
public record AddPackFindersEvent(PackType getPackType, Consumer<RepositorySource> sourceAdder) implements IModBusEvent {
    public static EventBus<AddPackFindersEvent> getBus(BusGroup modBusGroup) {
        return IModBusEvent.getBus(modBusGroup, AddPackFindersEvent.class);
    }

    /**
     * Adds a new source to the list of pack finders.
     *
     * @param source the pack finder
     */
    public void addRepositorySource(RepositorySource source) {
        sourceAdder.accept(source);
    }
}
