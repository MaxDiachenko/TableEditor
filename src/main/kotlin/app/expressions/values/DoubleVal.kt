package app.expressions.values

import app.data.CellType
import kotlin.math.pow

@JvmInline
value class DoubleVal(val value: Double) : NumericValue {
    override fun toDouble() = value
    override fun toInt() = value.toInt()
    override fun toBool() = value != 0.0
    override fun toNumericValue() = this
    override fun toBoolValue() = BoolVal(toBool())
    override fun cellType() = CellType.DOUBLE
    override fun toString() = value.toString()
    override fun any() = value

    override fun unaryMinus() = DoubleVal(-value)
    override fun unaryPlus() = this

    override fun plus(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value + other.value)
        is IntVal -> DoubleVal(this.value + other.value)
    }

    override fun minus(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value - other.value)
        is IntVal -> DoubleVal(this.value - other.value)
    }

    override fun times(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value * other.value)
        is IntVal -> DoubleVal(this.value * other.value)
    }

    override fun div(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.value / other.value)
        is IntVal -> DoubleVal(this.value / other.value)
    }

    override fun pow(other: NumericValue) = when (other) {
        is DoubleVal -> DoubleVal(this.toDouble().pow(other.value))
        is IntVal -> DoubleVal(this.toDouble().pow(other.value))
    }
}
