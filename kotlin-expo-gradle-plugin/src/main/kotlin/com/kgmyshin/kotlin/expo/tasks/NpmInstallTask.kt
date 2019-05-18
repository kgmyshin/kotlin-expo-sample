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

import com.kgmyshin.kotlin.expo.extensions.NpmDependency
import com.kgmyshin.kotlin.expo.utils.nodePath
import com.kgmyshin.kotlin.expo.utils.readLinesOrEmpty
import com.kgmyshin.kotlin.expo.utils.startWithRedirectOnFail
import com.kgmyshin.kotlin.expo.utils.workspace
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecSpec
import java.io.File
import java.net.URI
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

open class NpmInstallTask : DefaultTask() {

    override fun getGroup(): String? = "expo"

    override fun getDescription(): String? = "npm install for run expo"

    @OutputDirectory
    val nodeModulesDir: File = project.workspace().resolve("node_modules")

    @TaskAction
    fun install() {
        val npm = nodePath(project, "npm").first()

        val unpacked = (project.tasks.filterIsInstance<UnpackGradleDependenciesTask>().map { task ->
            task.resultNames?.map { NpmDependency(it.name, it.uri, NpmDependency.RuntimeScope) }
                ?: task.resultFile.readLinesOrEmpty()
                    .map { it.split("/", limit = 4).map(String::trim) }
                    .filter { it.size == 4 }
                    .map { NpmDependency(it[0], it[3], NpmDependency.RuntimeScope) }
        }).flatten()

        unpacked.forEach { dep ->
            val linkPath = nodeModulesDir.resolve(dep.name).toPath().toAbsolutePath()
            val target = Paths.get(URI(dep.versionOrUri)).toAbsolutePath()

            ensureSymbolicLink(linkPath, target)
        }

        ProcessBuilder(npm.absolutePath, "install")
            .directory(project.workspace())
            .apply { ensurePath(environment(), npm.parentFile.absolutePath) }
            .redirectErrorStream(true)
            .startWithRedirectOnFail(project, "npm install")
    }

    private fun ensureSymbolicLink(link: Path, target: Path) {
        try {
            if (Files.isSymbolicLink(link)) {
                if (Files.readSymbolicLink(link) != target) {
                    Files.delete(link)
                    Files.createSymbolicLink(link, target)
                }
                return
            }

            Files.delete(link)
        } catch (cause: DirectoryNotEmptyException) {
            link.toFile().deleteRecursively()
        } catch (ignore: NoSuchFileException) {
        } catch (ignore: java.nio.file.NoSuchFileException) {
        }

        createSymbolicLink(link, target)
    }

    private fun isWindows() = System.getProperty("os.name")?.contains("windows", ignoreCase = true) == true

    private fun createSymbolicLink(link: Path, target: Path) {
        if (isWindows()) {
            // always create junction on Windows as it does npm
            // Java doesn't provide any API to create junctions so we call native tool
            project.exec { spec: ExecSpec ->
                spec.apply {
                    workingDir(project.workspace())
                    commandLine("cmd", "/C", "mklink", "/J", link.toString(), target.toString())
                }
            }.assertNormalExitValue()
        } else {
            Files.createSymbolicLink(link, target)
        }
    }

    private fun ensurePath(env: MutableMap<String, String>, path: String) {
        val sep = File.pathSeparator
        env.keys.filter { it.equals("path", ignoreCase = true) }.forEach { envName ->
            val envValue = env[envName]
            if (envValue != null && !envValue.startsWith(path)) {
                env[envName] =
                    path + sep + if (envValue.endsWith(path)) envValue.removeSuffix(path) else envValue.replace(
                        "$sep$path$sep",
                        sep
                    )
            }
        }
    }
}