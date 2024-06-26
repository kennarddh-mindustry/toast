package com.github.kennarddh.mindustry.toast.core.handlers

import arc.files.Fi
import com.github.kennarddh.mindustry.genesis.core.commons.runOnMindustryThread
import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import com.github.kennarddh.mindustry.toast.core.commons.ToastState
import com.github.kennarddh.mindustry.toast.core.commons.ToastVars
import kotlinx.coroutines.sync.withLock
import mindustry.Vars
import mindustry.io.SaveIO

class AutoSaveHandler : Handler {
    companion object {
        val file: Fi = Vars.saveDirectory.child("autoSave.msav")
    }

    @TimerTask(30f, 30f)
    suspend fun autoSave() {
        ToastVars.stateLock.withLock {
            if (ToastVars.state != ToastState.Hosting) return
        }

        Logger.debug("Running auto save.")

        runOnMindustryThread {
            save()
        }
    }

    fun save() {
        try {
            SaveIO.save(file)

            Logger.debug("Successfully auto saved.")
        } catch (error: Exception) {
            Logger.error("Failed to save auto save msav file.", error)
        }
    }
}