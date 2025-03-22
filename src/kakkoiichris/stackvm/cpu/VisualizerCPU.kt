package kakkoiichris.stackvm.cpu

import kakkoiichris.stackvm.linker.Linker
import kakkoiichris.stackvm.util.bool
import kakkoiichris.stackvm.util.float
import kakkoiichris.stackvm.util.toAddress
import kakkoiichris.stackvm.util.truncate
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.*

object MoveBar : JPanel() {
    private fun readResolve(): Any = MoveBar

    init {
        layout = BorderLayout()

        val buttons = JPanel()

        buttons.layout = GridLayout(1, 2)

        buttons.add(object : JToggleButton("RUN") {
            init {
                addChangeListener {
                    setText(if (isSelected) "STOP" else "RUN")
                }
            }
        })

        buttons.add(JButton("STEP"))

        add(buttons, BorderLayout.CENTER)

        val speed = JSlider(JSlider.HORIZONTAL, 50, 2000, 250)
        speed.paintLabels = true

        add(speed, BorderLayout.SOUTH)
    }
}

abstract class VisualizerCPU(name: String) : CPU() {
    private val frame = JFrame("SVML Memory Visualizer - $name")

    init {
        val content = frame.contentPane

        val size = Dimension(800, 600)
        content.minimumSize = size
        content.preferredSize = size

        content.layout = BorderLayout()

        content.add(MoveBar, BorderLayout.NORTH)

        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.isVisible = true
    }

    override fun run(): Double {
        while (running) {
            decode()
        }

        return result
    }
}