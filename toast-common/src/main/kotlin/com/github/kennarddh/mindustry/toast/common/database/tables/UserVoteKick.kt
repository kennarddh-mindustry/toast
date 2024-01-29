package com.github.kennarddh.mindustry.toast.common.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object UserVoteKick : IntIdTable() {
    val reason = text("reason")
    val kickedAt = datetime("kickedAt").defaultExpression(CurrentDateTime)
    val kickEndAt = datetime("kickEndAt")
    val starterUserID = reference("starterUserID", Users)
    val userID = reference("userID", Users).nullable()
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val requiredVotes = short("requiredVotes")
}