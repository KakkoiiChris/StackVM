/*   ______  ____   ____  ____    ____  _____
 * .' ____ \|_  _| |_  _||_   \  /   _||_   _|
 * | (___ \_| \ \   / /    |   \/   |    | |
 *  _.____`.   \ \ / /     | |\  /| |    | |   _
 * | \____) |   \ ' /     _| |_\/_| |_  _| |__/ |
 *  \______.'    \_/     |_____||_____||________|
 *
 *         Stack Virtual Machine Language
 *     Copyright (C) 2024 Christian Alexander
 */
package kakkoiichris.svml.cpu

import kakkoiichris.svml.util.bool
import kakkoiichris.svml.util.float
import kotlin.reflect.KProperty

sealed class Register<T>(private val address: kotlin.Int) {
    abstract fun toFloat(t: T): Double

    abstract fun toType(f: Double): T

    operator fun getValue(cpu: CPU, property: KProperty<*>) =
        toType(cpu.memory[address])

    operator fun setValue(cpu: CPU, property: KProperty<*>, value: T) {
        cpu.memory[address] = toFloat(value)
    }

    class Bool(address: kotlin.Int) : Register<Boolean>(address) {
        override fun toFloat(t: Boolean) = t.float

        override fun toType(f: Double) = f.bool
    }

    class Int(address: kotlin.Int) : Register<kotlin.Int>(address) {
        override fun toFloat(t: kotlin.Int) = t.toDouble()

        override fun toType(f: Double) = f.toInt()
    }

    class Float(address: kotlin.Int) : Register<Double>(address) {
        override fun toFloat(t: Double) = t

        override fun toType(f: Double) = f
    }
}