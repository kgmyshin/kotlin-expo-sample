/*
 * Copyright JetBrains
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kgmyshin.kotlin.expo.tasks

import com.kgmyshin.kotlin.expo.extensions.NpmExtension
import com.kgmyshin.kotlin.expo.utils.KotlinNewMpp
import com.kgmyshin.kotlin.expo.utils.toLocalURI
import com.kgmyshin.kotlin.expo.utils.toSemver
import com.kgmyshin.kotlin.expo.utils.workspace
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.utils.LibraryUtils
import java.io.File

open class UnpackGradleDependenciesTask : DefaultTask() {

    companion object {
        fun unpackFile(project: Project) = project.workspace().resolve(".unpack.txt")
    }

    data class NameVersionsUri(val name: String, val version: String, val semver: String, val uri: String)

    var customCompileConfiguration: Configuration? = null

    var customTestCompileConfiguration: Configuration? = null

    @get:Input
    val compileConfiguration: Configuration
        get() = customCompileConfiguration ?: project.configurations.getByName(JavaPlugin.COMPILE_CONFIGURATION_NAME)

    @get:Input
    val testCompileConfiguration: Configuration
        get() = customTestCompileConfiguration
            ?: project.configurations.getByName(JavaPlugin.TEST_COMPILE_CONFIGURATION_NAME)

    @OutputFile
    val resultFile = unpackFile(project)

    @Internal
    var resultNames: MutableList<NameVersionsUri>? = null

    @Internal
    private val npm: NpmExtension = project.extensions.findByType(NpmExtension::class.java)!!

    @get:Input
    val replacementsInput: String
        get() = npm.versionReplacements.joinToString()

    init {
        try {
            Class.forName("org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension")
            // This line executed only on Kotlin 1.2.70+
            KotlinNewMpp.configureNpmCompileConfigurations(this)
        } catch (e: ClassNotFoundException) {
        }
        onlyIf {
            npm.dependencies.isNotEmpty() || npm.developmentDependencies.isNotEmpty()
        }
    }

    override fun getGroup(): String? = "expo"

    override fun getDescription(): String? = "unpack"

    @TaskAction
    fun unpackLibraries() {
        resultNames = mutableListOf()
        val out = project.workspace().resolve("node_modules_imported")

        out.mkdirs()

//        val projectArtifacts = compileConfiguration.allDependencies
//            .filterIsInstance<ProjectDependency>()
//            .flatMap { it.dependencyProject.configurations }
//            .flatMap { it.allArtifacts }
//            .map { it.file.canonicalFile.absolutePath }
//            .toSet()

        (compileConfiguration.resolvedConfiguration.resolvedArtifacts +
                testCompileConfiguration.resolvedConfiguration.resolvedArtifacts
                )
//            .filter { it.file.canonicalFile.absolutePath !in projectArtifacts }
            .filter { it.file.exists() && LibraryUtils.isKotlinJavascriptLibrary(it.file) }
            .forEach { artifact ->
                @Suppress("UNCHECKED_CAST")
                val existingPackageJson = project.zipTree(artifact.file).firstOrNull { it.name == "package.json" }
                    ?.let { JsonSlurper().parse(it) as Map<String, Any> }

                if (existingPackageJson != null) {
                    val name = existingPackageJson["name"]?.toString()
                        ?: getJsModuleName(artifact.file)
                        ?: artifact.name
                        ?: artifact.id.displayName
                        ?: artifact.file.nameWithoutExtension

                    val outDir = out.resolve(name)
                    outDir.mkdirs()

                    logger.debug("Unpack to node_modules from ${artifact.file} to $outDir")
                    project.copy { copy ->
                        copy.from(project.zipTree(artifact.file))
                            .into(outDir)
                    }

                    val existingVersion = existingPackageJson["version"]?.toString() ?: toSemver(null)

                    resultNames?.add(
                        NameVersionsUri(
                            name,
                            artifact.moduleVersion.id.version,
                            existingVersion,
                            outDir.toLocalURI()
                        )
                    )
                } else {
                    val modules = getJsModuleNames(artifact.file)
                        .takeIf { it.isNotEmpty() } ?: listOf(
                        artifact.name
                            ?: artifact.id.displayName
                            ?: artifact.file.nameWithoutExtension
                    )

                    for (name in modules) {
                        val version =
                            npm.versionReplacements.singleOrNull { it.name == artifact.name || it.name == name }?.versionOrUri
                                ?: toSemver(artifact.moduleVersion.id.version)

                        val outDir = out.resolve(name)
                        outDir.mkdirs()

                        logger.debug("Unpack to node_modules from ${artifact.file} to $outDir")
                        project.copy { copy ->
                            copy.from(project.zipTree(artifact.file))
                                .into(outDir)
                        }

                        val packageJson = mapOf(
                            "name" to name,
                            "version" to version,
                            "main" to "$name.js",
                            "_source" to "gradle"
                        )

                        outDir.resolve("package.json").bufferedWriter().use { out ->
                            out.appendln(JsonBuilder(packageJson).toPrettyString())
                        }

                        resultNames?.add(
                            NameVersionsUri(
                                name,
                                artifact.moduleVersion.id.version,
                                version,
                                outDir.toLocalURI()
                            )
                        )
                    }
                }
            }

        resultFile.bufferedWriter().use { writer ->
            resultNames?.joinTo(
                writer,
                separator = "\n",
                postfix = "\n"
            ) { "${it.name}/${it.version}/${it.semver}/${it.uri}" }
        }
    }

    private val moduleNamePattern = """\s*//\s*Kotlin\.kotlin_module_metadata\(\s*\d+\s*,\s*("[^"]+")""".toRegex()
    private fun getJsModuleName(file: File): String? {
        return project.zipTree(file)
            .filter { it.name.endsWith(".meta.js") && it.canRead() }
            .mapNotNull { moduleNamePattern.find(it.readText())?.groupValues?.get(1) }
            .mapNotNull { JsonSlurper().parseText(it)?.toString() }
            .singleOrNull()
    }

    private fun getJsModuleNames(file: File): List<String> {
        return project.zipTree(file)
            .filter { it.name.endsWith(".meta.js") && it.canRead() }
            .mapNotNull { moduleNamePattern.find(it.readText())?.groupValues?.get(1) }
            .mapNotNull { JsonSlurper().parseText(it)?.toString() }
    }
}