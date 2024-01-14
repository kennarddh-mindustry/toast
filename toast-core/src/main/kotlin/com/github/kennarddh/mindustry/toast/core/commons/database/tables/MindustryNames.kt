package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryNames : IntIdTable() {
    val name = integer("name").uniqueIndex()
    val rawName = integer("rawName")
}