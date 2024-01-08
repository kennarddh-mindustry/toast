package kennarddh.toast.core.database

import org.jetbrains.exposed.sql.Database

object DatabaseSettings {
    val db by lazy {
        Database.connect(
            "jdbc:mariadb://localhost:3307/toast", driver = "org.mariadb.jdbc.Driver",
            user = "root", password = "root"
        )
    }
}