package com.github.kennarddh.mindustry.toast.core.commons.database.tables

import com.github.kennarddh.mindustry.toast.core.commons.UserRole
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Users : IntIdTable() {
    val username: Column<String> = varchar("username", 255)
    val password: Column<String> = varchar("password", 255)
    val role =
        customEnumeration(
            "role",
            "ENUM(${UserRole.entries.joinToString(", ") { "'${it.id}'" }}",
            { value -> UserRole.entries.find { it.id == value } as UserRole },
            { it.id })
}