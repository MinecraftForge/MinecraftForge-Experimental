/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.loading;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import com.mojang.renderpearl.api.device.GpuBackend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import static org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MAJOR_VERSION;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_CONTEXT_MINOR_VERSION;
import static org.lwjgl.sdl.SDLVideo.SDL_GL_GetAttribute;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class NoVizFallback {
    private static long WINDOW;
    public static LongSupplier windowHandoff(int width, int height, String title, Supplier<Object> backend) {
        return () -> {
            try {
                return WINDOW = Window.createWindowStatic((GpuBackend)backend.get(), width, height, title);
            } catch (Throwable e) {
                return sneak(e);
            }
        };
    }


    @SuppressWarnings("unchecked")
    private static <R, E extends Throwable> R sneak(Throwable t) throws E {
        throw (E)t;
    }

    public static Supplier<LoadingOverlay> loadingOverlay(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> ex, boolean fadein) {
        return () -> new LoadingOverlay(mc.get(), ri.get(), ex, fadein);
    }

    public static Boolean windowPositioning(Optional<Monitor> monitor, IntConsumer widthSetter, IntConsumer heightSetter, IntConsumer xSetter, IntConsumer ySetter) {
        return Boolean.FALSE;
    }

    public static String glVersion() {
        if (WINDOW != 0) {
            try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
                var majBuf = stack.mallocInt(1);
                var minBuf = stack.mallocInt(1);
                SDL_GL_GetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, majBuf);
                SDL_GL_GetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, minBuf);
                return majBuf.get(0) + "." + minBuf.get(0);
            }
        } else {
            return "3.2";
        }
    }
}
