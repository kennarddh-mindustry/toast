package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserMindustryNames : IntIdTable() {
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val name = varchar("name", 50)
    val strippedName = varchar("strippedName", 50)
}