package app.expressions.values

import app.data.CellType

@JvmInline
value class BoolVal(val value: Boolean) : CellValue {
    override fun toDouble() = value.toInt().toDouble()
    override fun toInt() = value.toInt()
    override fun toBool() = value
    override fun toNumericValue() = IntVal(toInt())
    override fun toBoolValue() = this
    override fun cellType() = CellType.BOOL
    override fun toString() = value.toString()
    override fun any() = value

    fun not() = BoolVal(!value)
}
