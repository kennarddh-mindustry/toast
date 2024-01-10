package kennarddh.toast.core.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseSettings {
    lateinit var database: Database

    fun init() {
        val config = HikariConfig()

        config.minimumIdle = 2
        config.maximumPoolSize = 8
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.jdbcUrl = System.getenv("DB_HOST")
        config.username = System.getenv("DB_USERNAME")
        config.password = System.getenv("DB_PASSWORD")

        val dataSource = HikariDataSource(config)

        database = Database.connect(dataSource)

        transaction {
            addLogger(StdOutSqlLogger)
        }
    }
}