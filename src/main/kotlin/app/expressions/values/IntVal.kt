package app.expressions.values

import app.data.CellType
import kotlin.math.pow

@JvmInline
value class IntVal(val value: Int) : NumericValue {
    override fun toDouble() = value.toDouble()
    override fun toInt() = value
    override fun toBool() = value != 0
    override fun toNumericValue() = this
    override fun toBoolValue() = BoolVal(toBool())
    override fun cellType() = CellType.INT
    override fun toString() = value.toString()
    override fun unaryMinus() = IntVal(-value)
    override fun unaryPlus() = this
    override fun any() = value

    override fun plus(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value + other.value)
        is IntVal -> IntVal(this.value + other.value)
    }

    override fun minus(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value - other.value)
        is IntVal -> IntVal(this.value - other.value)
    }

    override fun times(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value * other.value)
        is IntVal -> IntVal(this.value * other.value)
    }

    override fun div(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value / other.value)
        is IntVal -> DoubleVal(this.value.toDouble() / other.value)
    }

    override fun pow(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.toDouble().pow(other.value))
        is IntVal -> DoubleVal(this.toDouble().pow(other.value))
    }
}