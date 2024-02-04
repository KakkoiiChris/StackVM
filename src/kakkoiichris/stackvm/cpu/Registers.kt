package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.util.toBool
import kakkoiichris.stackvm.util.toFloat
import kotlin.reflect.KProperty

class BoolRegister(private val address: Int) {
    operator fun getValue(cpu: CPU, property: KProperty<*>): Boolean {
        return cpu.memory[address].toBool()
    }

    operator fun setValue(cpu: CPU, property: KProperty<*>, value: Boolean) {
        cpu.memory[address] = value.toFloat()
    }
}

class IntRegister(private val address: Int) {
    operator fun getValue(cpu: CPU, property: KProperty<*>): Int {
        return cpu.memory[address].toInt()
    }

    operator fun setValue(cpu: CPU, property: KProperty<*>, value: Int) {
        cpu.memory[address] = value.toFloat()
    }
}

class FloatRegister(private val address: Int) {
    operator fun getValue(cpu: CPU, property: KProperty<*>): Float {
        return cpu.memory[address]
    }

    operator fun setValue(cpu: CPU, property: KProperty<*>, value: Float) {
        cpu.memory[address] = value
    }
}