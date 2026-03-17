package com.adamfoerster.promisedland.game

data class HexagonData(
    val col: Int, 
    val row: Int,
    val name: String = "",
    val isActive: Boolean = true,
    val type: String? = null
) {
    val id: String = name.ifBlank { "${('A'.code + col).toChar()}${row + 1}" }
}
