package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserKick : IntIdTable() {
    val reason = text("reason")
    val kickedAt = datetime("kickedAt").defaultExpression(CurrentDateTime)
    val kickEndAt = datetime("kickEndAt")
    val ipAddressID = reference("ipAddressID", IPAddresses).nullable()
    val userID = reference("userID", Users).nullable()
    val mindustryUserID = reference("mindustryUserID", MindustryUser).nullable()
}