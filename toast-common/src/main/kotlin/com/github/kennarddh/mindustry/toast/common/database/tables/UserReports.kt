package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.Server
import org.jetbrains.exposed.dao.id.IntIdTable

object UserReports : IntIdTable() {
    val server = enumerationByName<Server>("server", 255)
    val reason = text("reason")
    val mindustryUserID = reference("mindustryUserID", MindustryUser).nullable()
    val userID = reference("userID", MindustryUser).nullable()
    val targetMindustryUserID = reference("targetMindustryUserID", MindustryUser).nullable()
    val targetUserID = reference("targetUserID", Users).nullable()
}