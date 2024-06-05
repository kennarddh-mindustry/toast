package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.GameMode
import org.jetbrains.exposed.dao.id.IntIdTable

object MapGameMode : IntIdTable() {
    val mapID = reference("mapID", Map)
    val gameMode = enumerationByName<GameMode>("gameMode", 100)
}