package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable

object MindustryUserMindustryNames : IntIdTable() {
    val mindustryNameID = reference("mindustryNameID", MindustryNames)
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
}