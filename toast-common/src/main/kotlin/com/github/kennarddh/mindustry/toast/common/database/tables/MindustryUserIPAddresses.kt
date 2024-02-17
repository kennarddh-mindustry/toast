package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserIPAddresses : IntIdTable() {
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val ipAddress = integer("ipAddress").uniqueIndex()
}