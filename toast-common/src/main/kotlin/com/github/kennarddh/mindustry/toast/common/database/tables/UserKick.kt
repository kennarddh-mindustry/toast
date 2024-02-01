package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.UserRole
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserKick : IntIdTable() {
    val reason = text("reason")
    val kickedAt = datetime("kickedAt").defaultExpression(CurrentDateTime)
    val kickEndAt = datetime("kickEndAt")
    val kickType = enumerationByName<UserRole>("kickType", 50)
    val mindustryUserID = reference("mindustryUserID", MindustryUser).nullable()
    val targetUserID = reference("targetUserID", Users).nullable()
    val targetMindustryUserID = reference("targetMindustryUserID", MindustryUser)
}