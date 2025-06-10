package app.expressions.values

import app.data.CellType

@JvmInline
value class StringVal(val value: String) : CellValue {
    override fun toDouble() = value.toDoubleOrNull() ?: Double.POSITIVE_INFINITY
    override fun toInt() = 0
    override fun toBool() = value.isNotEmpty()
    override fun toNumericValue() = IntVal(0)
    override fun toBoolValue() = BoolVal(toBool())
    override fun cellType() = if (value.startsWith('=')) CellType.FUNC else CellType.STRING
    override fun toString() = value
    override fun any() = value
}
