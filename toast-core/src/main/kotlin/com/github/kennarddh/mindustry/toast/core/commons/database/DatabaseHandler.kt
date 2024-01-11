package com.github.kennarddh.mindustry.toast.core.commons.database

import com.github.kennarddh.mindustry.toast.core.commons.database.tables.*
import kennarddh.genesis.core.handlers.Handler
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseHandler : Handler() {
    override fun onInit() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users)
            SchemaUtils.createMissingTablesAndColumns(IPAddresses)
            SchemaUtils.createMissingTablesAndColumns(MindustryUser)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserServerData)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserIPAddresses)
            SchemaUtils.createMissingTablesAndColumns(UserKick)
            SchemaUtils.createMissingTablesAndColumns(UserBan)
        }
    }
}