/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml.loading.moddiscovery;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.ModFileLoadingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jetbrains.annotations.ApiStatus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@ApiStatus.Internal
public final class MixinLocator extends AbstractModProvider implements IModLocator {
    private static final String ROIMFS = "roimfs";
    private static final String MIXIN = "org.spongepowered.mixin";
    private static final String MIXINS = "org/spongepowered/asm/mixin/Mixins.class";
    private static final Pattern DASH_VERSION = Pattern.compile("-(\\d+(\\.|$))");
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public List<IModLocator.ModFileOrException> scanMods() {
        var bootLayer = Launcher.INSTANCE.findLayerManager().orElseThrow().getLayer(Layer.BOOT).orElseThrow();
        var mixin = bootLayer.findModule(MIXIN).orElseThrow();
        var cl = Thread.currentThread().getContextClassLoader();
        var path = ClasspathLocator.getPathFromResource(cl, MIXINS);

        var bos = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bos)) {
            zip.putNextEntry(new ZipEntry(MODS_TOML));
            zip.write(buildModsToml(mixin, path));
            zip.putNextEntry(new ZipEntry("pack.mcmeta"));
            zip.write(buildPackMeta());
        } catch (IOException e) {
            return List.of(new IModLocator.ModFileOrException(null, new ModFileLoadingException("Failed to create mixin mod jar - " + e.getMessage())));
        }

        String filename = path.getFileName().toString();
        FileSystem roimfs = null;
        try {
            var uri = new URI(ROIMFS, "mixin/" + filename, null);
            roimfs = FileSystems.newFileSystem(uri, Collections.singletonMap("data", bos.toByteArray()));
        } catch (URISyntaxException | IOException e) {
            return List.of(new IModLocator.ModFileOrException(null, new ModFileLoadingException("Failed to create mixin mod jar file system - " + e.getMessage())));
        }

        var roimfsPath = roimfs.getPath(filename);
        return List.of(super.createMod(roimfsPath));
    }

    private static String getVersion(Module module, Path path) {
        var version = module.getDescriptor().rawVersion().orElse(null);
        if (version != null)
            return version;

        var name = path.getFileName().toString();
        var matcher = DASH_VERSION.matcher(name);
        if (matcher.find()) {
            // attempt to parse the tail as a version string
            try {
                var tail = name.substring(matcher.start() + 1, name.length() - 4);
                return Version.parse(tail).toString();
            } catch (IllegalArgumentException ignore) { }
        }

        return "unknown";
    }

    private static byte[] buildModsToml(Module module, Path path) throws IOException {
        var mods = new ArrayList<Config>();

        var conf = Config.inMemory();
        conf.set("modLoader", "lowcodefml");
        conf.set("loaderVersion", "1");
        conf.set("license", "MIT");
        conf.set("mods", mods);

        var version = getVersion(module, path);
        var mixin = Config.inMemory();
        mixin.set("modId", "mixin");
        mixin.set("version", version);
        mixin.set("displayName", "Mixin");
        mixin.set("description", "A used to modify bytecode during loading.");
        mods.add(mixin);

        // Potentially find a way to figure out what for it is and add them as a mod to be depended on.

        try (var bos = new ByteArrayOutputStream()) {
            TomlFormat.instance().createWriter().write(conf, bos, StandardCharsets.UTF_8);
            return bos.toByteArray();
        }
    }

    private static byte[] buildPackMeta() {
        var obj = new JsonObject();
        var pack = new JsonObject();
        obj.add("pack", pack);

        pack.add("description", new JsonPrimitive("Synthetic pack"));
        pack.add("min_format", new JsonPrimitive(88));
        pack.add("max_format", new JsonPrimitive(88));

        var json = GSON.toJson(obj);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String name() {
        return "mixin";
    }
}
