package net.minecraftforge.forge.build

import groovy.json.JsonBuilder
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

@CompileStatic
abstract class WriteForgeVersionJson extends DefaultTask {
    abstract @Input Property<String> getForgeVersion()
    abstract @Input Property<String> getMinecraftVersion()
    abstract @Input Property<String> getMcpVersion()

    abstract @OutputFile RegularFileProperty getOutputFile()

    @Inject
    WriteForgeVersionJson() {}

    @TaskAction
    @CompileDynamic
    void exec() {
        var json = new JsonBuilder()
        json {
            forge this.forgeVersion.get()
            mc    this.minecraftVersion.get()
            mcp   this.mcpVersion.get()
        }
        this.outputFile.asFile.get().text = json.toPrettyString()
    }
}