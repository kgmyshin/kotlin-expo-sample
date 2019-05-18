package com.kgmyshin.kotlin.expo.tasks

import com.kgmyshin.kotlin.expo.extensions.KotlinExpoExtension
import com.kgmyshin.kotlin.expo.utils.workspace
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

open class BuildTask : DefaultTask() {

    override fun getGroup(): String? = "expo"

    override fun getDescription(): String? = "build"

    @Internal
    private val expo = project.extensions.getByType(KotlinExpoExtension::class.java)

    @TaskAction
    fun build() {
        copyBundledJs()
    }

    private fun copyBundledJs() {
        (project.tasks.getByPath("compileKotlin2Js") as Kotlin2JsCompile).outputFile.let {
            it.copyTo(
                project.workspace().resolve("App.js"),
                overwrite = true
            )
        }
    }
}