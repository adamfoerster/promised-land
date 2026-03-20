package com.adamfoerster.promisedland.game

data class HexagonData(
    val col: Int, 
    val row: Int,
    val name: String = "",
    val isActive: Boolean = true,
    val type: String? = null,
    val terrain: String? = null
) {
    val id: String = if(name.isNotBlank())
        "$name ${('A'.code + col).toChar()}${row + 1}"
    else
        "${('A'.code + col).toChar()}${row + 1}"
}
