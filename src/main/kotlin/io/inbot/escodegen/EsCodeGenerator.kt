package io.inbot.escodegen

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import org.elasticsearch.client.RequestOptions
import java.io.File
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

private val genByComment = """
    |Generated by EsKotlinCodeGenPlugin.
    |
    |Do not modify. This code is regenerated regularly. 
""".trimMargin()

class EsCodeGenerator(
    private val sourceDir: String = "build/generated-kotlin-code",
    private val esRestClientReflectService: EsRestClientReflectService = EsRestClientReflectService(),
    private val sharedCodePackageName: String = "io.inbot.eskotlinwrapper"
) {
    fun generateCode() {
        val sharedCodePath = File(sourceDir + File.separatorChar + sharedCodePackageName.replace('.', File.separatorChar))
        // create the directory if it did not exist
        sharedCodePath.mkdirs()
        println(sharedCodePath.absolutePath)

        File(sharedCodePath.absolutePath,"SuspendingActionListener.kt").writeText(generateActionListener(sharedCodePackageName))

        generateActionListener(sharedCodePackageName)
        val directory = File(sourceDir).absoluteFile
        esRestClientReflectService.listCLientClasses().map {
            println(it.name)
            generateCodeForClientClass(it)
        }
            .forEach {
                it.writeTo(directory.toPath())
            }
    }

    fun generateActionListener(sharedCodePackageName: String) = """
/*
$genByComment
*/

package $sharedCodePackageName

import kotlinx.coroutines.suspendCancellableCoroutine
import org.elasticsearch.action.ActionListener
import java.lang.Exception
import kotlin.coroutines.Continuation

/**
 * Action listener that can be used with to adapt the async methods across the java client to co-routines.
 */
class SuspendingActionListener<T>(private val continuation: Continuation<T>) :
    ActionListener<T> {
    override fun onFailure(e: Exception) {
        continuation.resumeWith(Result.failure(e))
    }

    override fun onResponse(response: T) {
        continuation.resumeWith(Result.success(response))
    }

    companion object {
        /**
         * Use this to call java async methods in a co-routine.
         *
         * ```kotlin
         * suspending {
         *   client.searchAsync(searchRequest, requestOptions, it)
         * }
         * ```
         */
        suspend fun <T : Any> suspending(block: (SuspendingActionListener<T>) -> Unit): T {
            return suspendCancellableCoroutine {
                it.invokeOnCancellation {
                    // TODO blocked on https://github.com/elastic/elasticsearch/issues/44802
                    // given where the ticket is going we probably grab the cancellation token and pass it into suspending so we can call cancel here.
                }
                block.invoke(SuspendingActionListener(it))
            }
        }
    }
}
"""

    private fun suspendingAsyncFunSpec(
        extensionClass: Class<*>,
        name: String,
        requestBodyType: Class<*>?,
        returnType: Type
    ): FunSpec {
        val funSpec = FunSpec.builder(name)
            .receiver(extensionClass)
            .addModifiers(KModifier.SUSPEND)
            .returns(returnType)

        if (requestBodyType != null) {
            funSpec.addParameter("request", requestBodyType)
            funSpec.addCode(
                """
                    |// generated code block
                    |return suspending {
                    |    this.$name(request,requestOptions,it)
                    |}
                """.trimMargin()
            )
        } else {
            funSpec.addCode(
                """
                    |// generated code block
                    |return suspending {
                    |    this.$name(requestOptions,it)
                    |}
                """.trimMargin()
            )

        }
        funSpec.addParameter(
            ParameterSpec.builder("requestOptions", RequestOptions::class).defaultValue(
                "%L",
                "RequestOptions.DEFAULT"
            ).build()
        )

        return funSpec.build()
    }

    private fun generateCodeForClientClass(clazz: Class<*>): FileSpec {
        val fileSpecBuilder = FileSpec.builder(clazz.getPackage().name, "${clazz.simpleName}Ext")
        fileSpecBuilder.addImport(sharedCodePackageName, "SuspendingActionListener.Companion.suspending")

        fileSpecBuilder.addComment(genByComment.trimIndent())
        clazz.methods
            .filter { it.name.endsWith("Async") }
            // don't generate code for deprecated methods
            .filter { method -> method.annotations.firstOrNull { !(it.javaClass == Deprecated::class || it.javaClass == java.lang.Deprecated::class) } == null}
            // java.lang.Boolean vs. kotlin.Boolean seems to break things; affects only a few methods
            .filter {
                val returnType  = getTypeParameter(it.parameters[it.parameterCount-1])
                !(returnType == java.lang.Boolean::class.java || returnType == Boolean::class.java)
            }
            .forEach {
            println("generating async method for ${it.name}")
            try {

                when (it.parameterCount) {
                    2 -> {
                        // api call without a request body
                        val actionListenerTypeArg = getTypeParameter(it.parameters[1])
                        fileSpecBuilder.addFunction(suspendingAsyncFunSpec(clazz, it.name, null, actionListenerTypeArg))

//                        println("${clazz.name}.${it.name}: void -> $actionListenerTypeArg")
                    }
                    3 -> {
                        // api call with a request body

                        val requestParamType = it.parameters[0].type

                        val actionListenerTypeArg = getTypeParameter(it.parameters[2])
                        fileSpecBuilder.addFunction(
                            suspendingAsyncFunSpec(
                                clazz,
                                it.name,
                                requestParamType,
                                actionListenerTypeArg
                            )
                        )

//                        println("${clazz.name}.${it.name}: $requestParamType -> $actionListenerTypeArg")

                    }
                    else -> {
                        println("unhandled method ${it.name} in ${clazz.name} with param count ${it.parameterCount}")
                    }
                }
            } catch (e: Exception) {
                println("${clazz.name}.${it.name} is weird")
            }
        }
        return fileSpecBuilder.build()
    }

    private fun getTypeParameter(parameter: Parameter): Type {
        // https://stackoverflow.com/questions/1901164/get-type-of-a-generic-parameter-in-java-with-reflection
        val parameterizedType: ParameterizedType = parameter.parameterizedType as ParameterizedType
        val actionListenerTypeArg = parameterizedType.actualTypeArguments[0]
        return actionListenerTypeArg ?: throw IllegalStateException("no type found")
    }
}