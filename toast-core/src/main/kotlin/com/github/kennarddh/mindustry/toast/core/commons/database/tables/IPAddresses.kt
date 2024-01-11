package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object IPAddresses : IntIdTable() {
    val ipAddress = integer("ipAddress").uniqueIndex()
}