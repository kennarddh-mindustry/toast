package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import com.github.kennarddh.mindustry.toast.core.commons.Server
import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserServerData : IntIdTable() {
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val server = enumerationByName<Server>("server", 255)
    val xp = integer("xp").default(0)
    val playTime = integer("playTime").default(0)
}