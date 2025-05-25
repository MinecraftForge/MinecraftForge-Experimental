/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.debug.dist;

import net.minecraft.world.entity.LivingEntityAttachEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.test.BaseTestMod;

@Mod(CapabilitySubEventTest.MOD_ID)
@GameTestNamespace("forge")
public class CapabilitySubEventTest extends BaseTestMod {
    static final String MOD_ID = "capability_sub_event_test";

    public CapabilitySubEventTest(FMLJavaModLoadingContext context) {
        super(context);
    }

    @SubscribeEvent
    public void onEntityAttach(LivingEntityAttachEvent event) {}
}
