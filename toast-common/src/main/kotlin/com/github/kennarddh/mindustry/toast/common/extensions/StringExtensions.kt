package com.github.kennarddh.mindustry.toast.common.extensions

val discordPingRegex = """@(everyone|here|[!&]?[0-9]{17,20})""".toRegex()

fun String.preventDiscordPings(): String = this.replace(discordPingRegex) { "@\u200B${it.groupValues[1]}" }