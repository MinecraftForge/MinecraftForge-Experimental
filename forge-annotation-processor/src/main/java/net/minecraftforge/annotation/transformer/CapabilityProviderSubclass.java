/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.annotation.transformer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

/**
 * Implements makeAttachCapabilitiesEvent() in CapabilityProvider subclasses based on annotation data.
 */
public class CapabilityProviderSubclass implements ILaunchPluginService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Marker MARKER = MarkerManager.getMarker("CAPSUB");

    private static final String[] EVENT_TYPES = new String[] {
		"net/minecraftforge/event/AttachCapabilitiesEvent$EntityEvent"
    };

    private Map<String, String> events = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "attach_capability_subclasses";
    }

    @Override
    public void addResources(List<SecureJar> resources) {
    	var gson = new GsonBuilder().create();
    	var token = new TypeToken<Map<String, Map<String, String>>>(){};

    	for (var jar : resources) {
    		var path = jar.getPath("generic-event-data.json");
    		if (!Files.exists(path))
    			continue;

    		try (var reader = new InputStreamReader(Files.newInputStream(path))) {
    			var data = gson.fromJson(reader, token);

    			for (var eventType : EVENT_TYPES) {
        			var entities = data.getOrDefault(eventType, Collections.emptyMap());
        			events.putAll(entities);
    			}

    			LOGGER.info(MARKER, data);
    		} catch (IOException e) {
    			LOGGER.error(MARKER, "Failed to read {}", path, e);
    		}
    	}
    }


    private static final EnumSet<Phase> YAY = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NAY = EnumSet.noneOf(Phase.class);

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (isEmpty)
            return NAY;

        return events.containsKey(classType.getInternalName()) ? YAY : NAY;
    }

    @Override
    public int processClassWithFlags(final Phase phase, final ClassNode classNode, final Type classType, final String reason) {
    	var event = events.get(classType.getInternalName());
    	if (event == null)
    		return ComputeFlags.NO_REWRITE;

        var mtd = classNode.visitMethod(Opcodes.ACC_PROTECTED, "makeAttachCapabilitiesEvent", "()Lnet/minecraftforge/event/AttachCapabilitiesEvent;", null, new String[0]);
        mtd.visitTypeInsn(Opcodes.NEW, event);
        mtd.visitInsn(Opcodes.DUP);
        mtd.visitVarInsn(Opcodes.ALOAD, 0);
        mtd.visitMethodInsn(Opcodes.INVOKESPECIAL, event, "<init>", "(L" + classType.getInternalName() + ";)V", false);
        mtd.visitInsn(Opcodes.ARETURN);
        mtd.visitEnd();
        return ComputeFlags.COMPUTE_MAXS;
    }
}
