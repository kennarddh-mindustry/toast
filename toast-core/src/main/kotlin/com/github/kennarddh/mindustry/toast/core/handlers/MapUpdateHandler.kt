package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.commons.CoroutineScopes
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.toast.common.database.Database
import com.github.kennarddh.mindustry.toast.common.database.tables.Map
import com.github.kennarddh.mindustry.toast.common.messaging.Messenger
import com.github.kennarddh.mindustry.toast.common.messaging.messages.MapUpdateServerControl
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.launch
import mindustry.Vars
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.io.File

class MapUpdateHandler : Handler {
    override suspend fun onInit() {
        Messenger.listenServerControl(
            "${ToastVars.server.name}ServerMapUpdate",
            "map-update.${ToastVars.server.gameMode.name}"
        ) { message ->
            val data = message.data

            if (data !is MapUpdateServerControl) return@listenServerControl Logger.warn("Invalid map update server control message")

            CoroutineScopes.Main.launch {
                updateMaps()
            }
        }

        Logger.info("Map directory: ${Vars.customMapDirectory.absolutePath()}")

        updateMaps()
    }

    suspend fun updateMaps() {
        Database.newTransaction {
            val maps =
                Map.selectAll().where { (Map.gameMode eq ToastVars.server.gameMode) and (Map.active eq true) }

            val mapsID = mutableSetOf<Int>()

            val mapsDir = File(Vars.customMapDirectory.absolutePath())

            maps.forEach {
                mapsID.add(it[Map.id].value)

                val file = File(mapsDir, "${it[Map.id]}.msav")

                if (file.exists()) return@forEach

                file.outputStream().use { fileOut ->
                    it[Map.file].inputStream.copyTo(fileOut)
                }

                Logger.info("Map added: ${it[Map.id]}")
            }

            mapsDir.listFiles()?.forEach {
                for (validID in mapsID) {
                    if (it.name == "${validID}.msav") return@forEach
                }

                Logger.info("Map deleted: ${it.name.dropLast(5)}")

                it.delete()
            }
        }

        runOnMindustryThread {
            Vars.maps.reload()
        }
    }
}