package com.github.kennarddh.mindustry.toast.common.database

import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext

object DatabaseSettings {
    lateinit var database: Database

    suspend fun init(coroutineContext: CoroutineContext) {
        val config = HikariConfig()

        config.minimumIdle = 2
        config.maximumPoolSize = 8
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.jdbcUrl = System.getenv("DB_HOST")
        config.username = System.getenv("DB_USERNAME")
        config.password = System.getenv("DB_PASSWORD")
        config.connectionTimeout = 20000

        val dataSource = HikariDataSource(config)

        database = Database.connect(dataSource)

        newSuspendedTransaction(coroutineContext) {
            addLogger(StdOutSqlLogger)

            SchemaUtils.createMissingTablesAndColumns(Users)
            SchemaUtils.createMissingTablesAndColumns(MindustryUser)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserServerData)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserIPAddresses)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserMindustryNames)
            SchemaUtils.createMissingTablesAndColumns(UserPunishments)
            SchemaUtils.createMissingTablesAndColumns(UserVoteKickVotes)
        }
    }
}