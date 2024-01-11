package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import com.github.kennarddh.mindustry.toast.core.commons.UserRole
import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val username = varchar("username", 50)
    val password = varchar("password", 255)
    val role = enumerationByName<UserRole>("role", 100)
    val discordID = varchar("discordID", 255).nullable()
}