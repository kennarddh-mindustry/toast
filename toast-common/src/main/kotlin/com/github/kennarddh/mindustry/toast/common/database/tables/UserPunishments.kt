package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.PunishmentType
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserPunishments : IntIdTable() {
    val reason = text("reason")
    val punishedAt = datetime("punishedAt").defaultExpression(CurrentDateTime)
    val endAt = datetime("endAt")
    val type = enumerationByName<PunishmentType>("type", 50)
    val mindustryUserID = reference("mindustryUserID", MindustryUser).nullable()
    val userID = reference("userID", MindustryUser).nullable()
    val targetMindustryUserID = reference("targetMindustryUserID", MindustryUser).nullable()
    val targetUserID = reference("targetUserID", Users).nullable()
}