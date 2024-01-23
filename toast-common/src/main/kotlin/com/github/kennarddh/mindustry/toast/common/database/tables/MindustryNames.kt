package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryNames : IntIdTable() {
    val name = varchar("name", 50).uniqueIndex()
    val strippedName = varchar("strippedName", 50)
}