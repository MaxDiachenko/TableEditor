package app.expressions.values

import app.data.CellType
import java.time.*
import java.time.format.DateTimeFormatter

fun String.toLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        } catch (e2: Exception) {
            try {
                val date = LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                date.atStartOfDay()
            } catch (e2: Exception) {
                null
            }
        }
    }
}

class DateTimeVal(val value: LocalDateTime) : CellValue {
    override fun toDouble() = value.toLong().toDouble()
    override fun toInt() = value.toLong().toInt()
    override fun toBool() = true
    override fun toNumericValue() = IntVal(toInt())
    override fun toBoolValue() = BoolVal(true)
    override fun cellType() = CellType.DATETIME
    override fun toString() = value.toString()
    override fun any() = value

    fun plus(other: NumericValue) = DateTimeVal(value.plusDays(other.toInt().toLong()))
    fun minus(other: NumericValue) = DateTimeVal(value.minusDays(other.toInt().toLong()))
    fun minus(other: DateTimeVal) = IntVal(Period.between(value.toLocalDate(), other.value.toLocalDate()).days)
}
