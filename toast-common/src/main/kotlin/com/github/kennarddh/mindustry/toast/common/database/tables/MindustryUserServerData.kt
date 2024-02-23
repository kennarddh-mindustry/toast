package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.Server
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import kotlin.time.Duration.Companion.seconds

object MindustryUserServerData : IntIdTable() {
    val mindustryUserID = reference("mindustryUserID", MindustryUser)
    val server = enumerationByName<Server>("server", 255)
    val xp = integer("xp").default(0)
    val playTime = duration("playTime").default(0.seconds)
    val activePlayTime = duration("activePlayTime").default(0.seconds)
    val mindustryUSID = varchar("mindustryUSID", 225)
    val userID = reference("userID", Users).nullable()
}