package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.PunishmentType
import com.github.kennarddh.mindustry.toast.common.Server
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object UserPunishments : IntIdTable() {
    val server = enumerationByName<Server>("server", 255)
    val reason = text("reason")
    val punishedAt = datetime("punishedAt").defaultExpression(CurrentDateTime)
    val endAt = datetime("endAt").nullable()
    val pardonedAt = datetime("pardonedAt").nullable()
    val type = enumerationByName<PunishmentType>("type", 50)
    val mindustryUserID = reference("mindustryUserID", MindustryUser).nullable()
    val userID = reference("userID", MindustryUser).nullable()
    val targetMindustryUserID = reference("targetMindustryUserID", MindustryUser).nullable()
    val targetUserID = reference("targetUserID", Users).nullable()
}