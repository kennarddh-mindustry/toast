package com.github.kennarddh.mindustry.toast.core.commons

import mindustry.gen.Call
import mindustry.gen.Player

fun Player.infoMessage(message: String) {
    Call.infoMessage(this.con, message)
}