/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.common.data;

import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class ForgeSpriteSourceProvider extends SpriteSourceProvider
{
    public ForgeSpriteSourceProvider(PackOutput output, ExistingFileHelper fileHelper)
    {
        super(output, fileHelper, "forge");
    }

    @Override
    protected void addSources()
    {
        atlas(SpriteSourceProvider.BLOCKS_ATLAS).addSource(new SingleFile(Identifier.parse("forge:white"), Optional.empty()));
    }
}
