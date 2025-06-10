package app.expressions.values

import app.data.CellType
import app.expressions.Expression
import app.expressions.Token
import java.time.*

fun Boolean.toInt(): Int = if (this) 1 else 0
fun LocalDateTime.toLong(): Long = this.toEpochSecond(ZoneOffset.UTC)
fun Long.toLocalDateTime(): LocalDateTime = Instant.ofEpochSecond(this).atOffset(ZoneOffset.UTC).toLocalDateTime()

interface CellValue : Token, Expression {
    fun toDouble(): Double
    fun toInt(): Int
    fun toBool(): Boolean
    fun toNumericValue(): NumericValue
    fun toBoolValue(): BoolVal
    fun cellType(): CellType
    override fun toString(): String
    fun any(): Any


    companion object {
        fun fromTypedString(type: CellType, value: String): CellValue = when (type) {
            CellType.STRING -> StringVal(value)
            CellType.INT -> IntVal(value.toInt())
            CellType.BOOL -> BoolVal(value.toBoolean())
            CellType.DOUBLE -> DoubleVal(value.toDouble())
            CellType.DATETIME -> DateTimeVal(LocalDateTime.parse(value))
            CellType.FUNC -> StringVal(value)
        }

        fun fromString(input: String): CellValue {
            val str = input.trim()

            val intVal = str.toIntOrNull()
            if (intVal != null) return IntVal(intVal)

            val doubleVal = str.toDoubleOrNull()
            if (doubleVal != null) return DoubleVal(doubleVal)

            val boolVal = str.toBooleanStrictOrNull()
            if (boolVal != null) return BoolVal(boolVal)

            val dateTimeVal = str.toLocalDateTime()
            if (dateTimeVal != null) return DateTimeVal(dateTimeVal)

            return StringVal(str)
        }
    }
}
