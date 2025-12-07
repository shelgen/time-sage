package com.github.shelgen.timesage.ui.screens

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SerialIdGenerationTest {
    @Test
    fun `no collision of screen serial IDs`() {
        val map = mutableMapOf<String, KClass<*>>()
        Screen::class.sealedSubclasses.forEach { screenClass ->
            val serialId = SerialIdGeneration.forScreen(screenClass)
            println("$serialId ($screenClass)")
            assertFalse(map.containsKey(serialId)) {
                "Serial ID $serialId was generated for both ${map[serialId]} and $screenClass"
            }
            map[serialId] = screenClass
        }
    }

    @Test
    fun `no collision of component serial IDs within a screen`() {
        Screen::class.sealedSubclasses.forEach { screenClass ->
            println("--- Screen $screenClass ---")
            val map = mutableMapOf<String, KClass<*>>()
            CustomIdSerialization.getScreenComponents(screenClass)
                .forEach { componentClass ->
                    val serialId = SerialIdGeneration.forComponent(componentClass)
                    println("$serialId ($componentClass)")
                    assertFalse(map.containsKey(serialId)) {
                        "Serial ID $serialId was generated for both ${map[serialId]} and $componentClass"
                    }
                    map[serialId] = componentClass
                }
        }
    }
}
