package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.Server
import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserServerData : IntIdTable() {
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val server = enumerationByName<Server>("server", 255)
    val xp = integer("xp").default(0)
    val playTime = long("playTime").default(0)
    val activePlayTime = long("activePlayTime").default(0)
}