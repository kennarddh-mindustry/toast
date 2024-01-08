package kennarddh.toast.core.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseSettings {
    val db by lazy {
        Database.connect(
            "jdbc:mariadb://localhost:3307/toast", driver = "org.mariadb.jdbc.Driver",
            user = "root", password = "root"
        )
    }

    init {
        transaction {
            addLogger(StdOutSqlLogger)
        }
    }
}