package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUser : IntIdTable() {
    val userID = reference("userID", Users).nullable()
    val mindustryUUID = varchar("mindustryUUID", 255)
}