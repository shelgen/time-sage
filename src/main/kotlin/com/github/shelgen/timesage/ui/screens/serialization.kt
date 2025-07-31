package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.OperationContext
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/*
 Serializes and deserializes screen component to/from a Discord custom ID.
 The format is as follows: <abbrevated screen class name>{param1,...,paramN}|<abbrevated component class name>{param1,...,paramN}|<random value>
 The random value is added because the ID must be unique for the bot.
 */
object CustomIdSerialization {
    fun serialize(screenComponent: ScreenComponent<*, *>): String {
        val screenPart = serializePart(screenComponent.screen)
        val componentPart = serializePart(screenComponent)
        val randomPart = Base64.getEncoder().encode(Random.Default.nextBytes(6)).decodeToString()
        return "$screenPart|$componentPart|$randomPart"
    }

    fun <T : ScreenComponent<*, *>> deserialize(customId: String, expectedType: KClass<T>, context: OperationContext): T {
        val (screenPart, componentPart, randomPart) = customId.split('|', limit = 3)
        val (screenClassNameHashcode, screenParameters) = deserializePart(screenPart)
        val (omponentClassNameHashcode, componentParameters) = deserializePart(componentPart)

        val reconstructor =
            ScreenComponentReconstructor::class.sealedSubclasses.mapNotNull { it.objectInstance }
                .find {
                    it.screenClass.simpleName!!.hashCode() == screenClassNameHashcode
                            && it.componentClass.simpleName!!.hashCode() == omponentClassNameHashcode
                            && it.componentClass.isSubclassOf(expectedType)
                }
                ?: error("Could not find matching deserializer for component type $screenClassNameHashcode.$omponentClassNameHashcode")

        @Suppress("UNCHECKED_CAST")
        return reconstructor.reconstruct(
            screenParameters = screenParameters,
            componentParameters = componentParameters,
            context = context
        ) as T
    }

    private fun serializePart(serializable: SerializableWithParameters): String =
        serializePart(className = serializable.javaClass.simpleName, parameters = serializable.parameters())

    private fun serializePart(className: String, parameters: List<String>): String =
        "${className.hashCode()}{${parameters.joinToString(separator = ",")}}"

    private fun deserializePart(part: String): Pair<Int, List<String>> {
        val (classNameHashcodeString, parametersString) = part.removeSuffix("}").split('{', limit = 2)
        val parameters = parametersString.split(',')
        val classNameHashcode = classNameHashcodeString.toInt()
        return Pair(first = classNameHashcode, second = parameters)
    }
}

interface SerializableWithParameters {
    fun parameters(): List<String>
}

sealed class ScreenComponentReconstructor<SCREEN : Screen, COMPONENT : ScreenComponent<SCREEN, *>>(
    val screenClass: KClass<SCREEN>,
    val componentClass: KClass<COMPONENT>
) {
    abstract fun reconstruct(screenParameters: List<String>, componentParameters: List<String>, context: OperationContext): COMPONENT
}
