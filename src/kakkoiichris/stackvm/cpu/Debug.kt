package kakkoiichris.stackvm.cpu

object Debug {
    var enabled = true

    operator fun invoke(action: Debug.() -> Unit) =
        this.action()

    fun print(x: Any) {
        if (enabled) {
            kotlin.io.print(x)
        }
    }

    fun println(x: Any) {
        if (enabled) {
            kotlin.io.println(x)
        }
    }

    fun println() {
        if (enabled) {
            kotlin.io.println()
        }
    }
}