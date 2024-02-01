package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserUSID : IntIdTable() {
    val mindustryUserServerDataID = reference("mindustryUserServerDataID", MindustryUserServerData)
    val mindustryUSID = varchar("mindustryUSID", 64)
}