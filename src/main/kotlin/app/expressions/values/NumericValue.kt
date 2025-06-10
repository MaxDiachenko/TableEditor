package app.expressions.values

sealed interface NumericValue : CellValue {
    fun unaryMinus(): NumericValue
    fun unaryPlus(): NumericValue
    fun plus(other: NumericValue): NumericValue
    fun minus(other: NumericValue): NumericValue
    fun times(other: NumericValue): NumericValue
    fun div(other: NumericValue): NumericValue
    fun pow(other: NumericValue): NumericValue
}
