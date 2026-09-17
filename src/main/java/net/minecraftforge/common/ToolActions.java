/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common;

import com.google.common.collect.Sets;
import net.minecraftforge.common.extensions.IForgeItem;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ToolActions {

    /**
     *  Exposed by shears to allow querying tool behaviours
     */
    public static final ToolAction SHEARS_DIG = ToolAction.get("shears_dig");

    /**
     *  Used during player attack to figure out if a sweep attack should be performed
     *
     *  @see IForgeItem#getSweepHitBox
     */
    public static final ToolAction SWORD_SWEEP = ToolAction.get("sword_sweep");

    /**
     *  This action is exposed by shears and corresponds to a harvest action that is triggered with a right click on a block that supports such behaviour.
     *  Example: Right click with shears on a beehive with honey level 5 to harvest it
     */
    public static final ToolAction SHEARS_HARVEST = ToolAction.get("shears_harvest");

    /**
     *  This action is exposed by shears and corresponds to a carve action that is triggered with a right click on a block that supports such behaviour.
     *  Example: Right click with shears o a pumpkin to carve it
     */
    public static final ToolAction SHEARS_CARVE = ToolAction.get("shears_carve");

    /**
     *  This action is exposed by shears and corresponds to a disarm action that is triggered by breaking a block that supports such behaviour.
     *  Example: Breaking a trip wire with shears to disarm it.
     */
    public static final ToolAction SHEARS_DISARM = ToolAction.get("shears_disarm");

    /**
     * This action corresponds to right-clicking the fishing rod.
     */
    public static final ToolAction FISHING_ROD_CAST = ToolAction.get("fishing_rod_cast");

    // Default actions supported by each tool type
    public static final Set<ToolAction> DEFAULT_SHEARS_ACTIONS = of(SHEARS_DIG, SHEARS_HARVEST, SHEARS_CARVE, SHEARS_DISARM);
    public static final Set<ToolAction> DEFAULT_FISHING_ROD_ACTIONS = of(FISHING_ROD_CAST);

    private static Set<ToolAction> of(ToolAction... actions) {
        return Stream.of(actions).collect(Collectors.toCollection(Sets::newIdentityHashSet));
    }
}
