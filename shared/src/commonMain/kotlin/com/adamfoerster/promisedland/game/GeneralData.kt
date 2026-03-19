package com.adamfoerster.promisedland.game

data class GeneralData(
    val id: Long = 0,
    val name: String,
    val movements: Int = 2,
    val strength: Int = 1
)

data class GeneralPlacementInfo(
    val generalId: Long,
    val generalName: String,
    val playerId: Long,
    val playerColor: String,
    val hexCol: Int,
    val hexRow: Int
)
