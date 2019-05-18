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

package com.kgmyshin.kotlin.expo.utils

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

fun Project.workspace(): File {
    if (!project.buildDir.exists()) {
        project.buildDir.mkdir()
    }
    val workspace = project.buildDir.resolve("expo")
    if (!workspace.exists()) {
        workspace.mkdir()
    }
    return workspace
}

fun Project.packageJson(): File = workspace().resolve("package.json")

fun Project.expo(): File = workspace().resolve("node_modules").resolve(".bin").resolve("expo")

fun Project.npmrc(): File = packageJson().resolveSibling(".npmrc")

fun nodePath(project: Project, command: String = "node"): List<File> {
    val paths = whereIs(command)
    if (paths.isEmpty()) {
        project.logger.debug("No executable $command found in ${splitEnvironmentPath()}")
        throw GradleException("No executable $command found")
    }
    return paths
}

fun splitEnvironmentPath() = System.getenv("PATH").split(File.pathSeparator).filter(String::isNotBlank)

private fun whereIs(command: String): List<File> = splitEnvironmentPath().flatMap {
    val bin = it + File.separator + command

    Suffixes.map { File(bin + it) }
}.filter { it.exists() && it.canExecute() }.distinct()

private val Suffixes = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    listOf(".exe", ".bat", ".cmd")
} else {
    listOf("", ".sh", ".bin", ".app")
}