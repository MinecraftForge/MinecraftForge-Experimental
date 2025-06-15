/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.unsafe.UnsafeHacks;

import java.lang.invoke.MethodHandles.Lookup;
import java.util.Collection;

/**
 * A small helper class to aid Forge modders in migrating from EventBus 6 to the new EventBus 7.
 * <br>
 * Refer to the migration guide mentioned in the Forge 1.21.6 announcement or ask in the Forge Discord for help with
 * migrating your mod to the new system.
 */
public final class EventBusMigrationHelper {
    private EventBusMigrationHelper() {}

    static final EventBusMigrationHelper INSTANCE = new EventBusMigrationHelper();

    /**
     * A version of {@link BusGroup#register(Lookup, Class)} that doesn't require a lookup. It is strongly recommended
     * to use the BusGroup method directly with {@code MethodHandles.lookup()} as the first argument, as this migration
     * helper method relies on JDK internals that may break without notice.
     * @param clazz the class containing static event listener methods that you want to register.
     * @return a collection of {@link EventListener} instances that were registered, can be passed to
     *         {@link BusGroup#unregister(Collection)} to unregister them later.
     * @see BusGroup#register(Lookup, Class)
     */
    public Collection<EventListener> register(Class<?> clazz) {
        return registerListeners(clazz);
    }

    /**
     * A version of {@link BusGroup#register(Lookup, Object)} that doesn't require a lookup. It is strongly recommended
     * to use the BusGroup method directly with {@code MethodHandles.lookup()} as the first argument, as this migration
     * helper method relies on JDK internals that may break without notice.
     * @param instance the instance of a class containing instance and static event listener methods that you want to register.
     * @return a collection of {@link EventListener} instances that were registered, can be passed to
     *         {@link BusGroup#unregister(Collection)} to unregister them later.
     * @see BusGroup#register(Lookup, Object)
     */
    public Collection<EventListener> register(Object instance) {
        return registerListeners(instance);
    }

    private static Collection<EventListener> registerListeners(Object instance) {
        final class DodgyLookup {
            private DodgyLookup() {}
            private static final Lookup INSTANCE;
            static {
                try {
                    var lookupField = Lookup.class.getDeclaredField("IMPL_LOOKUP");
                    UnsafeHacks.setAccessible(lookupField);
                    INSTANCE = (Lookup) lookupField.get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (instance.getClass() == Class.class) {
            return BusGroup.DEFAULT.register(DodgyLookup.INSTANCE, (Class<?>) instance);
        } else {
            return BusGroup.DEFAULT.register(DodgyLookup.INSTANCE, instance);
        }
    }
}
