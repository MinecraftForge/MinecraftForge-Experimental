/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fml;

import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Master list of all mods - game-side version. This is classloaded in the game scope and
 * can dispatch game level events as a result.
 */
public class ModList {
    private static ModList INSTANCE;
    private final List<IModFileInfo> modFiles;
    private final List<IModInfo> sortedList;
    private final Map<String, IModFileInfo> fileById;
    private List<ModContainer> mods;
    private Map<String, ModContainer> indexedMods;
    private List<ModFileScanData> modFileScanData;
    private List<ModContainer> sortedContainers;

    private ModList(final List<ModFile> modFiles, final List<ModInfo> sortedList) {
        var modFileInfos = this.modFiles = modFiles.stream().map(ModFile::getModFileInfo).toList();
        this.sortedList = sortedList.stream().map(IModInfo.class::cast).toList();
        var byId = new HashMap<String, IModFileInfo>();
        for (var file : modFileInfos) {
            for (var mod : file.getMods())
                byId.put(mod.getModId(), mod.getOwningFile());
        }
        this.fileById = Map.copyOf(byId);
        CrashReportCallables.registerCrashCallable("Mod List", this::crashReport);
    }

    private String getModContainerState(String modId) {
        return getModContainerById(modId).map(ModContainer::getCurrentState).map(Object::toString).orElse("NONE");
    }

    private String fileToLine(IModFile mf) {
        var mainMod = mf.getModInfos().getFirst();
        return String.format(Locale.ENGLISH, "%-50.50s|%-30.30s|%-30.30s|%-20.20s|%-10.10s|Manifest: %s", mf.getFileName(),
                mainMod.getDisplayName(),
                mainMod.getModId(),
                mainMod.getVersion(),
                getModContainerState(mainMod.getModId()),
                ((ModFileInfo)mf.getModFileInfo()).getCodeSigningFingerprint().orElse("NOSIGNATURE"));
    }

    private String crashReport() {
        return "\n" + applyForEachModFile(this::fileToLine).collect(Collectors.joining("\n\t\t", "\t\t", ""));
    }

    public static ModList of(List<ModFile> modFiles, List<ModInfo> sortedList) {
        INSTANCE = new ModList(modFiles, sortedList);
        return INSTANCE;
    }

    public static ModList get() {
        return INSTANCE;
    }

    public List<IModFileInfo> getModFiles() {
        return modFiles;
    }

    public IModFileInfo getModFileById(String modid) {
        return this.fileById.get(modid);
    }

    void setLoadedMods(final List<ModContainer> modContainers) {
        this.mods = modContainers;
        this.sortedContainers = modContainers.stream().sorted(Comparator.comparingInt(c -> sortedList.indexOf(c.getModInfo()))).toList();
        this.indexedMods = modContainers.stream().collect(Collectors.toMap(ModContainer::getModId, Function.identity()));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getModObjectById(String modId) {
        return getModContainerById(modId).map(ModContainer::getMod).map(o -> (T) o);
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        return Optional.ofNullable(this.indexedMods.get(modId));
    }

    public Optional<? extends ModContainer> getModContainerByObject(Object obj) {
        return mods.stream().filter(mc -> mc.getMod() == obj).findFirst();
    }

    public List<IModInfo> getMods() {
        return this.sortedList;
    }

    public boolean isLoaded(String modTarget) {
        return this.indexedMods.containsKey(modTarget);
    }

    public int size() {
        return mods.size();
    }

    public List<ModFileScanData> getAllScanData() {
        if (modFileScanData == null) {
            modFileScanData = this.sortedList.stream().
                    map(IModInfo::getOwningFile).
                    filter(Objects::nonNull).
                    map(IModFileInfo::getFile).
                    distinct().
                    map(IModFile::getScanResult).
                    toList();
        }
        return modFileScanData;
    }

    public void forEachModFile(Consumer<IModFile> fileConsumer) {
        modFiles.stream().map(IModFileInfo::getFile).forEach(fileConsumer);
    }

    public <T> Stream<T> applyForEachModFile(Function<IModFile, T> function) {
        return modFiles.stream().map(IModFileInfo::getFile).map(function);
    }

    public void forEachModContainer(BiConsumer<String, ModContainer> modContainerConsumer) {
        indexedMods.forEach(modContainerConsumer);
    }

    public void forEachModInOrder(Consumer<ModContainer> containerConsumer) {
        this.sortedContainers.forEach(containerConsumer);
    }

    public List<ModContainer> getLoadedMods() {
        return this.sortedContainers;
    }

    public <T> Stream<T> applyForEachModContainer(Function<ModContainer, T> function) {
        return indexedMods.values().stream().map(function);
    }
}
