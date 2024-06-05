package com.github.kennarddh.mindustry.toast.common.database.tables

import com.github.kennarddh.mindustry.toast.common.MapReviewStatus
import com.github.kennarddh.mindustry.toast.common.extensions.mediumblob
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Map : IntIdTable() {
    val name = varchar("name", 100)
    val description = text("description")
    val author = varchar("name", 100)
    val width = integer("width")
    val height = integer("height")
    val file = mediumblob("file")
    val submittedByUserID = reference("submittedByUserID", MindustryUser)
    val active = bool("active")
    val submittedAt = datetime("submittedAt")
    val reviewedByUserID = reference("reviewedByUserID", MindustryUser).nullable()
    val reviewStatus = enumerationByName<MapReviewStatus>("reviewStatus", 50)
    val reviewedAt = datetime("reviewedAt").nullable()
    val obsoletedBy = reference("obsoletedBy", Map).nullable()
}