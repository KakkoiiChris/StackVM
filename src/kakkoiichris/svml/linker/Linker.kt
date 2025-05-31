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
package kakkoiichris.svml.linker

import kakkoiichris.svml.cpu.CPU
import kakkoiichris.svml.lang.lexer.Context
import kakkoiichris.svml.lang.parser.DataType
import kakkoiichris.svml.lang.parser.Node
import kakkoiichris.svml.lang.parser.Signature
import kakkoiichris.svml.linker.libraries.Console
import kakkoiichris.svml.linker.libraries.Lang
import kakkoiichris.svml.linker.libraries.Math
import kakkoiichris.svml.linker.libraries.Strings
import kakkoiichris.svml.linker.libraries.gfx.Graphics
import java.io.File

typealias Values = List<Double>

typealias Method = (cpu: CPU, data: LinkData) -> Values

object Linker {
    private val links = mutableListOf<Link>()

    private val functionTable = mutableMapOf<Int, Function>()

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

    fun hasFunction(id: Int) =
        id in functionTable

    operator fun get(id: Int) =
        functionTable[id]

    fun addFunction(name: String, format: String = "", vararg params: DataType, method: Method) {
        val node = Node.Name(Context.none(), name)

        val function = Function(format, method)

        val signature = Signature(node, params.toList())

        functionTable[signature.toString().hashCode()] = function
    }

    class Function(private val format: String, private val method: Method) {
        operator fun invoke(cpu: CPU, values: Values): Values {
            val data = LinkData.parse(format, values)

            return method.invoke(cpu, data)
        }
    }
}