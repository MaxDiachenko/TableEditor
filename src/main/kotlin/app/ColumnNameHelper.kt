package app

object ColumnNameHelper {
    fun toName(index: Int): String {
        require(index >= 0) { "Index must be non-negative" }
        var i = index
        val sb = StringBuilder()

        while (i >= 0) {
            val remainder = i % 26
            sb.append(('A' + remainder))
            i = (i / 26) - 1
        }

        return sb.reverse().toString()
    }

    fun toIndex(name: String): Int {
        require(name.isNotBlank()) { "Column name must not be blank" }

        var result = 0
        val upperName = name.uppercase()

        for (char in upperName) {
            require(char in 'A'..'Z') { "Invalid column character: $char" }
            result = result * 26 + (char - 'A' + 1)
        }

        return result - 1
    }
}