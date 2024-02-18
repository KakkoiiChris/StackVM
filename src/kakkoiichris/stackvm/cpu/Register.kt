package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kotlin.reflect.KProperty

sealed class Register<T>(private val address: kotlin.Int) {
    abstract fun toFloat(t: T): kotlin.Double

    abstract fun toType(f: kotlin.Double): T

    operator fun getValue(cpu: CPU, property: KProperty<*>): T {
        return toType(cpu.memory[address])
    }

    operator fun setValue(cpu: CPU, property: KProperty<*>, value: T) {
        cpu.memory[address] = toFloat(value)
    }

    class Bool(address: kotlin.Int) : Register<Boolean>(address) {
        override fun toFloat(t: Boolean) = t.float

        override fun toType(f: kotlin.Double) = f.bool
    }

    class Int(address: kotlin.Int) : Register<kotlin.Int>(address) {
        override fun toFloat(t: kotlin.Int) = t.toDouble()

        override fun toType(f: kotlin.Double) = f.toInt()
    }

    class Float(address: kotlin.Int) : Register<Double>(address) {
        override fun toFloat(t: kotlin.Double) = t

        override fun toType(f: kotlin.Double) = f
    }
}