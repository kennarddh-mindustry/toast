package com.github.kennarddh.mindustry.toast.common.database

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import org.slf4j.Logger

class SimpleSqlLogger(val logger: Logger) : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        logger.info("SQL: ${context.expandArgs(transaction)}")
    }
}