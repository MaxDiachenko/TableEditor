import org.koin.core.context.startKoin
import ui.TableEditorApp
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

fun main() {
    startKoin {
        modules(appModule)
    }
    try {
        SwingUtilities.invokeLater {
            TableEditorApp().start()
        }
    } catch (e: Exception) {
        println("Exception in UI startup: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

