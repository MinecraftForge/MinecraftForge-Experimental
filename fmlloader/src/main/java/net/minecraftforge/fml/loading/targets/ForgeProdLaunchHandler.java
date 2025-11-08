/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading.targets;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
sealed abstract class ForgeProdLaunchHandler extends CommonLaunchHandler {
    protected ForgeProdLaunchHandler(LaunchType type) {
        super(type, "forge_");
    }

    @Override public String getNaming() { return "mcp"; }
    @Override public boolean isProduction() { return true; }

    public static final class Client extends ForgeProdLaunchHandler {
        public Client() {
            super(CLIENT);

            // This is the earliest service found by ModLauncher, so process our log markers here.
            // This was in our ITransformationService, but that is called after a bunch of other services so we could miss some logging lines.
            // I put this in the Client class just so it runs once. Could go in any of our services. Yay magic code!
            var markers = System.getProperty("forge.logging.markers", "").split(",");
            for (var marker : markers)
                System.setProperty("forge.logging.marker." + marker.toLowerCase(Locale.ROOT), "ACCEPT");
        }

        @Override
        public List<Path> getMinecraftPaths() {
            return List.of(getPathFromResource("net/minecraft/client/Minecraft.class"));
        }
    }

    public static final class Server extends ForgeProdLaunchHandler {
        public Server() {
            super(SERVER);
        }

        @Override
        public List<Path> getMinecraftPaths() {
            return List.of(getPathFromResource("net/minecraft/server/MinecraftServer.class"));
        }
    }
}
