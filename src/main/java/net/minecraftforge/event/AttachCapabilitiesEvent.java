/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.eventbus.api.bus.EventBus;
import net.minecraftforge.eventbus.api.event.InheritableEvent;
import net.minecraftforge.eventbus.api.event.MutableEvent;
import org.jspecify.annotations.NullMarked;

/**
 * Fired whenever an object with Capabilities support {currently TileEntity/Item/Entity)
 * is created. Allowing for the attachment of arbitrary capability providers.
 *
 * Please note that as this is fired for ALL object creations efficient code is recommended.
 * And if possible use one of the sub-classes to filter your intended objects.
 *
 * @see Entity
 * @see BlockEntity
 * @see ItemStack
 * @see Level
 * @see LevelChunk
 */
@NullMarked
public abstract class AttachCapabilitiesEvent<T> extends MutableEvent implements InheritableEvent {
    private final T obj;
    private final Map<ResourceLocation, ICapabilityProvider> caps = new LinkedHashMap<>();
    private final Map<ResourceLocation, ICapabilityProvider> view = Collections.unmodifiableMap(caps);
    private final List<Runnable> listeners = new ArrayList<>();
    private final List<Runnable> listenersView = Collections.unmodifiableList(listeners);

    public static class Entity extends AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> {
        public static final EventBus<Entity> BUS = EventBus.create(Entity.class);

        public Entity(net.minecraft.world.entity.Entity obj) {
            super(obj);
        }
    }

    public static class BlockEntity extends AttachCapabilitiesEvent<net.minecraft.world.level.block.entity.BlockEntity> {
        public static final EventBus<BlockEntity> BUS = EventBus.create(BlockEntity.class);

        public BlockEntity(net.minecraft.world.level.block.entity.BlockEntity obj) {
            super(obj);
        }
    }

    public static class ItemStack extends AttachCapabilitiesEvent<net.minecraft.world.item.ItemStack> {
        public static final EventBus<ItemStack> BUS = EventBus.create(ItemStack.class);

        public ItemStack(net.minecraft.world.item.ItemStack obj) {
            super(obj);
        }
    }

    public static class Level extends AttachCapabilitiesEvent<net.minecraft.world.level.Level> {
        public static final EventBus<Level> BUS = EventBus.create(Level.class);

        public Level(net.minecraft.world.level.Level obj) {
            super(obj);
        }
    }

    public static class LevelChunk extends AttachCapabilitiesEvent<net.minecraft.world.level.chunk.LevelChunk> {
        public static final EventBus<LevelChunk> BUS = EventBus.create(LevelChunk.class);

        public LevelChunk(net.minecraft.world.level.chunk.LevelChunk obj) {
            super(obj);
        }
    }

    protected AttachCapabilitiesEvent(T obj) {
        this.obj = obj;
    }

    /**
     * Retrieves the object that is being created, Not much state is set.
     */
    public T getObject() {
        return this.obj;
    }

    /**
     * Adds a capability to be attached to this object.
     * Keys MUST be unique, it is suggested that you set the domain to your mod ID.
     * If the capability is an instance of INBTSerializable, this key will be used when serializing this capability.
     *
     * @param key The name of owner of this capability provider.
     * @param cap The capability provider
     */
    public void addCapability(ResourceLocation key, ICapabilityProvider cap) {
        if (caps.containsKey(key))
            throw new IllegalStateException("Duplicate Capability Key: " + key  + " " + cap);
        this.caps.put(key, cap);
    }

    /**
     * A unmodifiable view of the capabilities that will be attached to this object.
     */
    public Map<ResourceLocation, ICapabilityProvider> getCapabilities() {
        return view;
    }

    /**
     * Adds a callback that is fired when the attached object is invalidated.
     * Such as a Entity/TileEntity being removed from world.
     * All attached providers should invalidate all of their held capability instances.
     */
    public void addListener(Runnable listener) {
        this.listeners.add(listener);
    }

    public List<Runnable> getListeners() {
        return this.listenersView;
    }
}
