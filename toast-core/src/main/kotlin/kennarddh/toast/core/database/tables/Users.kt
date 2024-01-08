package kennarddh.toast.core.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column

object Users : IntIdTable() {
    val username: Column<String> = varchar("username", 255)
    val password: Column<String> = varchar("password", 255)
}