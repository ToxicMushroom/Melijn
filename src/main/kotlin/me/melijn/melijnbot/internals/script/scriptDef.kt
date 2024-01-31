/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package me.melijn.melijnbot.internals.script

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.internals.command.ICommandContext
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

/**
 * More good sources in host.kt
 * https://github.com/Kotlin/kotlin-script-examples/blob/99bca23d0de02b27ba0e9f2a9b2bb246174fce3e/jvm/basic/jvm-maven-deps/script/src/main/kotlin/org/jetbrains/kotlin/script/examples/jvm/resolve/maven/scriptDef.kt
 */

// The KotlinScript annotation marks a class that can serve as a reference to the script definition for
// `createJvmCompilationConfigurationFromTemplate` call as well as for the discovery mechanism
// The marked class also become the base class for defined script type (unless redefined in the configuration)
@KotlinScript(
    // file name extension by which this script type is recognized by mechanisms built into scripting compiler plugin
    // and IDE support, it is recommended to use double extension with the last one being "kts", so some non-specific
    // scripting support could be used, e.g. in IDE, if the specific support is not installed.
    fileExtension = "scriptwithdeps.kts",
    // the class or object that defines script compilation configuration for this type of scripts
    compilationConfiguration = ScriptWithMavenDepsConfiguration::class
)
// the class is used as the script base class, therefore it should be open or abstract
abstract class ScriptWithMavenDeps

object ScriptWithMavenDepsConfiguration : ScriptCompilationConfiguration(
    {
        // adds implicit import statements (in this case `import kotlin.script.experimental.dependencies.DependsOn`, etc.)
        // to each script on compilation
        defaultImports(DependsOn::class, Repository::class)
        compilerOptions("-Xskip-prerelease-check", "-Xadd-modules=ALL-MODULE-PATH")

        implicitReceivers(ICommandContext::class)
        providedProperties("test" to ICommandContext::class)

        jvm {
            // the dependenciesFromCurrentContext helper function extracts the classpath from current thread classloader
            // and take jars with mentioned names to the compilation classpath via `dependencies` key.
            // to add the whole classpath for the classloader without check for jar presence, use
            // `dependenciesFromCurrentContext(wholeClasspath = true)`
            dependenciesFromCurrentContext(

                wholeClasspath = true // take the whole classpath
            )
        }
        // section that callbacks during compilation
        refineConfiguration {
            // the callback called than any of the listed file-level annotations are encountered in the compiled script
            // the processing is defined by the `handler`, that may return refined configuration depending on the annotations
            onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
        }
    }
)

@KotlinScript(
    // file name extension by which this script type is recognized by mechanisms built into scripting compiler plugin
    // and IDE support, it is recommended to use double extension with the last one being "kts", so some non-specific
    // scripting support could be used, e.g. in IDE, if the specific support is not installed.
    fileExtension = "script.kts",
    // the class or object that defines script compilation configuration for this type of scripts
    compilationConfiguration = ScriptWithMavenDepsConfigurationNoContext::class
)
// the class is used as the script base class, therefore it should be open or abstract
abstract class ScriptWithMavenDepsNoContext

object ScriptWithMavenDepsConfigurationNoContext : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class)
        compilerOptions("-Xskip-prerelease-check", "-Xadd-modules=ALL-MODULE-PATH")

        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
        }
    }
)

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

// The handler that is called during script compilation in order to reconfigure compilation on the fly
fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess() // If no action is performed, the original configuration should be returned
    return runBlocking {
        // resolving maven artifacts using annotation arguments
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            // updating the original configurations with the newly resolved artifacts as compilation dependencies
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}
