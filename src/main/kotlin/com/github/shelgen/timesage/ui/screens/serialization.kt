package com.github.shelgen.timesage.ui.screens

import com.github.shelgen.timesage.domain.OperationContext
import java.time.Instant
import java.time.LocalDate
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.typeOf

/*
 Serializes and deserializes screen component to/from a Discord custom ID.
 The format is as follows: <abbrevated screen class name>{param1,...,paramN}|<abbrevated component class name>{param1,...,paramN}|<random value>
 The random value is added because the ID must be unique for the bot.
 */
object CustomIdSerialization {
    fun serialize(screenComponent: ScreenComponent<*>): String =
        buildString {
            append(SerialIdGeneration.forScreen(screenComponent.screen::class))
            append("{")
            append(serializeFields(screenComponent.screen).joinToString(separator = ","))
            append("}")
            append("|")
            append(SerialIdGeneration.forComponent(screenComponent::class))
            append("{")
            append(serializeFields(screenComponent).joinToString(separator = ","))
            append("}")
            append("|")
            append(Base64.getEncoder().encode(Random.Default.nextBytes(6)).decodeToString())
        }

    private fun serializeFields(instance: Any): List<String> =
        instance::class.primaryConstructor!!.parameters
            .filterNot { parameter -> parameter.type == typeOf<OperationContext>() }
            .filterNot { parameter -> parameter.type.isSubtypeOf(typeOf<Screen>()) }
            .mapNotNull { parameter ->
                instance::class.memberProperties.find { it.name == parameter.name }?.let {
                    it.isAccessible = true
                    val value = it.javaField!!.get(instance)
                    it.isAccessible = false
                    serializeField(value, it.returnType)
                }
            }

    fun <T : ScreenComponent<*>> deserialize(customId: String, context: OperationContext): T {
        val (screenPart, componentPart, _) = customId.split('|', limit = 3)

        val (screenSerialId, serializedScreenFieldsString) = screenPart.removeSuffix("}").split('{', limit = 2)
        val serializedScreenFields = serializedScreenFieldsString.split(',')
        val screenClass = Screen::class.sealedSubclasses
            .find { SerialIdGeneration.forScreen(it) == screenSerialId }
            ?: error("Could not find screen class for serialId $screenSerialId")

        var serializedScreenFieldIndex = 0
        val screenConstructorArgs = screenClass.primaryConstructor!!.parameters.map { parameter ->
            if (parameter.type == typeOf<OperationContext>()) {
                context
            } else {
                deserializeField(serializedScreenFields[serializedScreenFieldIndex++], parameter.type)
            }
        }.toTypedArray()

        val screen = screenClass.primaryConstructor!!.call(*screenConstructorArgs)

        val (componentSerialId, serializedComponentFieldsString) = componentPart.removeSuffix("}").split('{', limit = 2)
        val serializedComponentFields = serializedComponentFieldsString.split(',')
        val componentClass = getScreenComponents(screenClass)
            .find { SerialIdGeneration.forComponent(it) == componentSerialId }
            ?: error("Could not find component class for serialId $componentSerialId")

        var serializedComponentFieldIndex = 0
        val componentConstructorArgs = componentClass.primaryConstructor!!.parameters.map { parameter ->
            if (parameter.name == ScreenComponent<*>::screen.name) {
                screen
            } else {
                deserializeField(serializedComponentFields[serializedComponentFieldIndex++], parameter.type)
            }
        }.toTypedArray()

        @Suppress("UNCHECKED_CAST")
        val component = componentClass.primaryConstructor!!.call(*componentConstructorArgs) as T

        return component
    }

    private fun serializeField(fieldValue: Any, fieldType: KType): String =
        when (fieldType) {
            typeOf<String>(),
            typeOf<LocalDate>(),
            typeOf<Instant>(),
            typeOf<Int>(),
            typeOf<Long>()
                -> fieldValue.toString()

            else -> error("Serialization of field of type $fieldType not supported")
        }

    private fun deserializeField(serializedValue: String, fieldType: KType): Any =
        when (fieldType) {
            typeOf<String>() -> serializedValue
            typeOf<LocalDate>() -> LocalDate.parse(serializedValue)
            typeOf<Instant>() -> Instant.parse(serializedValue)
            typeOf<Int>() -> serializedValue.toInt()
            typeOf<Long>() -> serializedValue.toLong()
            else -> error("Deserialization of field of type $fieldType not supported")
        }

    private fun flatNestedClasses(klass: KClass<*>): List<KClass<*>> {
        return klass.nestedClasses.flatMap { flatNestedClasses(it) } + klass
    }

    fun getScreenComponents(screenClass: KClass<out Screen>): List<KClass<out ScreenComponent<*>>> =
        flatNestedClasses(screenClass)
            .filter { it.isSubclassOf(ScreenComponent::class) }
            .map {
                @Suppress("UNCHECKED_CAST")
                it as KClass<ScreenComponent<*>>
            }
}

object SerialIdGeneration {
    private const val MAX_SERIAL_ID_LENGTH = 10

    fun forScreen(klass: KClass<out Screen>): String =
        generate(klass.simpleName!!.removeSuffix("Screen"))

    fun forComponent(klass: KClass<out ScreenComponent<*>>): String =
        "${getTypeLetter(klass)}_${generate(klass.simpleName!!)}"

    private fun getTypeLetter(klass: KClass<out ScreenComponent<*>>): Char =
        klass.superclasses
            .first { it in ScreenComponent::class.sealedSubclasses }
            .simpleName!!
            .removePrefix("Screen")
            .first()

    private fun generate(simpleName: String): String {
        val parts = simpleName.replace(Regex("\\W"), "").split(Regex("(?=[A-Z])"))
        for (maxLettersPerPart in MAX_SERIAL_ID_LENGTH downTo 2) {
            parts.joinToString(separator = "") { it.take(maxLettersPerPart) }
                .let {
                    if (it.length <= MAX_SERIAL_ID_LENGTH) {
                        return it
                    }
                }
        }
        return parts.joinToString(separator = "") { it.take(1) }.take(MAX_SERIAL_ID_LENGTH)
    }
}
