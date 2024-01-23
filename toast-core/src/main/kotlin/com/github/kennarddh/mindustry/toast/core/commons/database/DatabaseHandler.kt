package com.github.kennarddh.mindustry.toast.core.commons.database

import com.github.kennarddh.mindustry.toast.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.toast.core.commons.database.tables.*
import kennarddh.genesis.core.handlers.Handler
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class DatabaseHandler : Handler() {
    override fun onInit() {
        CoroutineScopes.Main.launch {
            newSuspendedTransaction(CoroutineScopes.IO.coroutineContext) {
                SchemaUtils.createMissingTablesAndColumns(Users)
                SchemaUtils.createMissingTablesAndColumns(IPAddresses)
                SchemaUtils.createMissingTablesAndColumns(MindustryNames)
                SchemaUtils.createMissingTablesAndColumns(MindustryUser)
                SchemaUtils.createMissingTablesAndColumns(MindustryUserServerData)
                SchemaUtils.createMissingTablesAndColumns(MindustryUserIPAddresses)
                SchemaUtils.createMissingTablesAndColumns(MindustryUserMindustryNames)
                SchemaUtils.createMissingTablesAndColumns(UserKick)
                SchemaUtils.createMissingTablesAndColumns(UserBan)
            }
        }
    }
}