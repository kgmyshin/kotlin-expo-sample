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

import net.rubygrapefruit.platform.Native
import net.rubygrapefruit.platform.ProcessLauncher
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.utils.closeQuietly
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

fun ProcessBuilder.startWithRedirectOnFail(
    project: Project,
    name: String,
    exec: Executor = DummyExecutor
): java.lang.Process {
    require(command().isNotEmpty()) { "No command specified" }

    val cmd = command().toList()
    val process = Native.get(ProcessLauncher::class.java).let { l ->
        addCommandPathToSystemPath()

        if (Os.isFamily(Os.FAMILY_WINDOWS) && !cmd[0].endsWith(".exe")) {
            command(listOf("cmd.exe", "/c") + cmd)
        }

        l.start(this)!!
    }

    val out = if (project.logger.isInfoEnabled) System.out else NullOutputStream
    val buffered = OutputStreamWithBuffer(out, 8192)

    val rc = try {
        ProcessHandler(process, buffered, buffered, exec).startAndWaitFor()
    } catch (t: Throwable) {
        project.logger.error("Process ${command().first()} failed", t)
        process.destroyForcibly()
        -1
    }

    if (rc != 0) {
        project.logger.error(buffered.lines().toString(Charsets.UTF_8))

        project.logger.debug("Command failed (exit code = $rc): ${command().joinToString(" ")}")
        throw GradleException("$name failed (exit code = $rc)")
    }

    return process
}

fun ProcessBuilder.addCommandPathToSystemPath() = apply {
    if (command().isNotEmpty()) {
        val commandFile = File(command()[0])
        if (commandFile.isAbsolute) {
            val env = splitEnvironmentPath()
            val commandDir = commandFile.parent
            if (commandDir !in env) {
                environment()["PATH"] = (env + commandDir).joinToString(File.pathSeparator)
            }
        }
    }
}

private object DummyExecutor : Executor {
    override fun execute(command: Runnable) {
        Thread(command).start()
    }
}

private object NullOutputStream : OutputStream() {
    override fun write(b: ByteArray?) {
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
    }

    override fun write(b: Int) {
    }
}

internal class OutputStreamWithBuffer(out: OutputStream, sizeLimit: Int) : FilterOutputStream(out) {
    private val buffer = ByteBuffer.allocate(sizeLimit)

    @Synchronized
    override fun write(b: Int) {
        if (ensure(1) >= 1) {
            buffer.put(b.toByte())
        }
        out.write(b)
    }


    override fun write(b: ByteArray) {
        write(b, 0, b.size)
    }

    @Synchronized
    override fun write(b: ByteArray, off: Int, len: Int) {
        putOrRoll(b, off, len)
        out.write(b, off, len)
    }

    @Synchronized
    fun lines(): ByteArray = buffer.duplicate().let { it.flip(); ByteArray(it.remaining()).apply { it.get(this) } }

    private fun putOrRoll(b: ByteArray, off: Int, len: Int) {
        var pos = off
        var rem = len

        while (rem > 0) {
            val count = ensure(rem)
            buffer.put(b, pos, count)
            pos += count
            rem -= count
        }
    }

    private fun ensure(count: Int): Int {
        if (buffer.remaining() < count) {
            val space = buffer.remaining()

            buffer.flip()
            while (buffer.hasRemaining() && buffer.position() + space < count) {
                dropLine()
            }
            buffer.compact()
        }

        return Math.min(count, buffer.remaining())
    }

    private fun dropLine() {
        while (buffer.hasRemaining()) {
            if (buffer.get().toInt() == 0x0d) {
                break
            }
        }
    }
}

private class ProcessHandler(
    val process: java.lang.Process,
    private val out: OutputStream,
    private val err: OutputStream,
    private val exec: Executor
) {
    private val latch = CountDownLatch(1)
    private var exitCode: Int = 0
    private var exception: Throwable? = null

    fun start() {
        StreamForwarder(process.inputStream, out, exec).start()
        StreamForwarder(process.errorStream, err, exec).start()

        exec.execute {
            try {
                exitCode = process.waitFor()
            } catch (t: Throwable) {
                exception = t
            } finally {
                closeQuietly(process.inputStream)
                closeQuietly(process.errorStream)
                closeQuietly(process.outputStream)

                latch.countDown()
            }
        }
    }

    fun waitFor(): Int {
        latch.await()

        exception?.let { throw it }

        return exitCode
    }

    fun startAndWaitFor(): Int {
        start()
        return waitFor()
    }
}

private class StreamForwarder(val source: InputStream, val destination: OutputStream, val exec: Executor) {
    fun start() {
        exec.execute {
            try {
                val buffer = ByteArray(4096)
                do {
                    val rc = source.read(buffer)
                    if (rc == -1) {
                        break
                    }

                    destination.write(buffer, 0, rc)
                    if (source.available() == 0) {
                        destination.flush()
                    }
                } while (true)
            } catch (ignore: IOException) {
            }
            destination.flush()
        }
    }
}
