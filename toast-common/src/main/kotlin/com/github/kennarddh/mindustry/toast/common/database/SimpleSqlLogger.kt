package com.github.kennarddh.mindustry.toast.common.database

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.slf4j.Logger

object SimpleSqlLogger : SqlLogger {
    lateinit var logger: Logger

    override fun log(context: StatementContext, transaction: Transaction) {
        logger.debug("SQL: ${context.expandArgs(transaction)}")
    }
}