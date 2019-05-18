package com.kgmyshin.kotlin.expo

import com.kgmyshin.kotlin.expo.extensions.KotlinExpoExtension
import com.kgmyshin.kotlin.expo.extensions.NpmExtension
import com.kgmyshin.kotlin.expo.tasks.*
import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinExpoPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        createExtensions(project)
        createTasks(project)
    }

    private fun createExtensions(project: Project) {
        project.extensions.create("kotlinExpo", KotlinExpoExtension::class.java, project)
        project.extensions.create("npm", NpmExtension::class.java, project)
    }

    private fun createTasks(project: Project) {
        val unpack = project.tasks.create("unpack", UnpackGradleDependenciesTask::class.java)
        val generatePackageJson = project.tasks.create("generate-package-json", GeneratePackageJsonTask::class.java)
        val npmInstall = project.tasks.create("npm-install", NpmInstallTask::class.java)
        val build = project.tasks.create("expo-build", BuildTask::class.java)
        val run = project.tasks.create("expo-run", RunExpoTask::class.java)
        val runIOS = project.tasks.create("expo-run-ios", RunIOSExpoTask::class.java)
        val runAndroid = project.tasks.create("expo-run-android", RunAndroidExpoTask::class.java)

        unpack.dependsOn("compileKotlin2Js")
        generatePackageJson.dependsOn(unpack)
        npmInstall.dependsOn(generatePackageJson)
        build.dependsOn(npmInstall)
        run.dependsOn(build)
        runIOS.dependsOn(build)
        runAndroid.dependsOn(build)
    }
}