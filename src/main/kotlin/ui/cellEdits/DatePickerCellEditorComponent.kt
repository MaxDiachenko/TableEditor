package ui.cellEdits
import app.expressions.values.DateTimeVal
import app.expressions.values.toLocalDateTime
import org.jdatepicker.impl.*
import java.awt.BorderLayout
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.*

class DatePickerCellEditorComponent(value: Any?) : JTextField() {

    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm")

    init {
        var cellData = (value as? DateTimeVal)

        var initialDate = Date.from(cellData!!.value.atZone(ZoneId.systemDefault()).toInstant())
        text = initialDate?.let { formatter.format(it) } ?: ""

        addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent?) {
                SwingUtilities.getAncestorOfClass(JTable::class.java, this@DatePickerCellEditorComponent)?.let { table ->
                    (table as JTable).cellEditor?.stopCellEditing()
                }
            }
        })

        val calendarModel = UtilDateModel()
        initialDate?.let { calendarModel.value = it }

        val datePanel = JDatePanelImpl(calendarModel, Properties())
        val popup = JPopupMenu().apply {
            layout = BorderLayout()
            add(datePanel, BorderLayout.CENTER)
            isFocusable = false
        }

        datePanel.addActionListener {
            val selected = calendarModel.value
            if (selected != null) {
                text = formatter.format(selected)
                popup.isVisible = false

                SwingUtilities.getAncestorOfClass(JTable::class.java, this)?.let { table ->
                    (table as JTable).cellEditor?.stopCellEditing()
                }
            }
        }

        addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                SwingUtilities.invokeLater {
                    popup.show(this@DatePickerCellEditorComponent, 0, height)
                }
            }

            override fun focusLost(e: FocusEvent?) {
                popup.isVisible = false
            }
        })
    }

    fun getValue(): Any {
        return text.trim()
    }
}
