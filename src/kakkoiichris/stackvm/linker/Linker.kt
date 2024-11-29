package kakkoiichris.stackvm.linker

import kakkoiichris.stackvm.cpu.CPU
import kakkoiichris.stackvm.lang.lexer.Location
import kakkoiichris.stackvm.lang.lexer.TokenType
import kakkoiichris.stackvm.lang.parser.DataType
import kakkoiichris.stackvm.lang.parser.Node
import kakkoiichris.stackvm.lang.parser.Signature
import kakkoiichris.stackvm.linker.libraries.Console
import kakkoiichris.stackvm.linker.libraries.Lang
import kakkoiichris.stackvm.linker.libraries.Math
import kakkoiichris.stackvm.linker.libraries.Strings
import kakkoiichris.stackvm.linker.libraries.gfx.Graphics
import java.io.File

typealias Values = List<Double>

typealias Method = (cpu: CPU, data: LinkData) -> Values

object Linker {
    private val links = mutableListOf<Link>()

    private val functionTable = mutableMapOf<String, Int>()

    private val functions = mutableListOf<Function>()

    private val sources = mutableMapOf<String, File>()

    val void = listOf(0.0)

    init {
        val folder = File(javaClass.getResource("/")!!.toURI())

        val files = folder
            .listFiles()!!
            .filter { it.isFile && it.extension == "svml" }

        for (file in files) {
            sources[file.nameWithoutExtension] = file
        }

        links += listOf(Lang, Console, Math, Strings, Graphics)
    }

    fun addLink(link: Link) {
        links.add(link)
    }

    fun link() {
        for (link in links) {
            link.open(this)
        }
    }

    fun hasFile(name: String) =
        name in sources

    fun getFile(name: String) =
        sources[name]!!

    fun hasFunction(signature: Signature): Boolean =
        signature.toString() in functionTable

    operator fun get(signature: Signature) =
        functionTable[signature.toString()]!!

    operator fun get(id: Int) =
        functions[id]

    fun addFunction(name: String, format: String="", vararg params: DataType, method: Method) {
        val node = Node.Name(Location.none(), TokenType.Name(name))

        val function = Function(format, method)

        functions += function

        val signature = Signature(node, params.toList())

        functionTable[signature.toString()] = functions.size - 1
    }

    class Function(private val format: String, private val method: Method) {
        operator fun invoke(cpu: CPU, values: Values): Values {
            val data = LinkData.parse(format, values)

            return method.invoke(cpu, data)
        }
    }
}