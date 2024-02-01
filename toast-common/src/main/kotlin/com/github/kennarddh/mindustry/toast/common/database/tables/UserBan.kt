package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserBan : IntIdTable() {
    val reason = text("reason")
    val bannedAt = datetime("bannedAt").defaultExpression(CurrentDateTime)
    val userID = reference("userID", Users).nullable()
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
}