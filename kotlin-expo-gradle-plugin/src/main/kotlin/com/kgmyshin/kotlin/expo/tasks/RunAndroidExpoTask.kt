package com.kgmyshin.kotlin.expo.tasks

import com.kgmyshin.kotlin.expo.utils.expo
import com.kgmyshin.kotlin.expo.utils.startWithRedirectOnFail
import com.kgmyshin.kotlin.expo.utils.workspace
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class RunAndroidExpoTask : DefaultTask() {

    override fun getGroup(): String? = "expo"

    override fun getDescription(): String? = "run expo ios"

    @TaskAction
    fun run() {
        ProcessBuilder(project.expo().absolutePath, "start", "--android")
            .directory(project.workspace())
            .redirectErrorStream(true)
            .startWithRedirectOnFail(project, "expo")
    }
}