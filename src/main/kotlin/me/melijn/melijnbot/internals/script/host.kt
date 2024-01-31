/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package me.melijn.melijnbot.internals.script

import me.melijn.melijnbot.internals.command.ICommandContext
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

/**
 * Good sources
 * https://github.com/Kotlin/kotlin-script-examples/blob/master/ReadMe.md
 * https://github.com/Kotlin/KEEP/blob/master/proposals/scripting-support.md#kotlin-main-kts
 * https://kotlinlang.org/docs/custom-script-deps-tutorial.html#run-scripts
 *
 * this file:
 * https://github.com/Kotlin/kotlin-script-examples/blob/99bca23d0de02b27ba0e9f2a9b2bb246174fce3e/jvm/basic/jvm-maven-deps/host/src/main/kotlin/org/jetbrains/kotlin/script/examples/jvm/resolve/maven/host/host.kt
 */
fun evalCode(script: String, implicitReceivers: ICommandContext, props: Map<String, Any?>): ResultWithDiagnostics<EvaluationResult> {

    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDeps> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        compilerOptions.append("-Xadd-modules=ALL-MODULE-PATH")
//        compilerOptions.append("-Xadd-modules=ALL-UNNAMED")
    }
    println("|\n$script\n|")
    return BasicJvmScriptingHost().eval(StringScriptSource(script), compilationConfiguration, ScriptEvaluationConfiguration {
        this.providedProperties("test" to implicitReceivers)
        println(implicitReceivers)
        this.implicitReceivers(implicitReceivers)
    })
}

fun evalCodeNoContext(script: String, props: Map<String, Any?>): ResultWithDiagnostics<EvaluationResult> {

    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptWithMavenDepsNoContext> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        compilerOptions.append("-Xadd-modules=ALL-MODULE-PATH")
    }
    println("|\n$script\n|")
    return BasicJvmScriptingHost().eval(StringScriptSource(script), compilationConfiguration, ScriptEvaluationConfiguration {
    })
}
