package com.github.kennarddh.mindustry.toast.common.database

import com.github.kennarddh.mindustry.toast.common.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.Logger
import kotlin.coroutines.CoroutineContext

object Database {
    lateinit var database: Database
    lateinit var coroutineContext: CoroutineContext

    suspend fun init(coroutineContext: CoroutineContext, logger: Logger) {
        this.coroutineContext = coroutineContext
        SimpleSqlLogger.logger = logger

        val config = HikariConfig()

        config.minimumIdle = 2
        config.maximumPoolSize = 8
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.jdbcUrl = "${System.getenv(" DB_HOST ")}?characterEncoding=utf8&useUnicode=true"
        config.username = System.getenv("DB_USERNAME")
        config.password = System.getenv("DB_PASSWORD")
        config.connectionTimeout = 20000

        val dataSource = HikariDataSource(config)

        database = Database.connect(dataSource)

        newTransaction {
            SchemaUtils.createMissingTablesAndColumns(Users)
            SchemaUtils.createMissingTablesAndColumns(MindustryUser)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserServerData)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserIPAddresses)
            SchemaUtils.createMissingTablesAndColumns(MindustryUserMindustryNames)
            SchemaUtils.createMissingTablesAndColumns(UserPunishments)
            SchemaUtils.createMissingTablesAndColumns(UserVoteKickVotes)
        }
    }

    suspend fun <T> newTransaction(log: Boolean = true, statement: suspend Transaction.() -> T): T =
        newSuspendedTransaction(coroutineContext, database) {
            if (log)
                addLogger(SimpleSqlLogger)

            statement(this)
        }
}