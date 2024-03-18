package com.github.kennarddh.mindustry.toast.core.handlers

import com.github.kennarddh.mindustry.genesis.core.handlers.Handler
import com.github.kennarddh.mindustry.genesis.core.timers.annotations.TimerTask
import com.github.kennarddh.mindustry.toast.core.commons.Logger
import mindustry.Vars
import mindustry.io.SaveIO

class AutoSaveHandler : Handler {
    @TimerTask(30f, 30f)
    fun autoSave() {
        Logger.info("Running auto save.")

        val file = Vars.saveDirectory.child("autoSave.msav")

        try {
            SaveIO.save(file)

            Logger.info("Successfully auto saved.")
        } catch (error: Exception) {
            Logger.error("Failed to save auto save msav file.", error)
        }
    }
}