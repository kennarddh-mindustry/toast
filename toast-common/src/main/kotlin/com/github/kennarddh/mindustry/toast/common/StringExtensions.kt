package com.github.kennarddh.mindustry.toast.common

import arc.util.Strings

val discordPingRegex = """@(everyone|here|[!&]?[0-9]{17,20})""".toRegex()

fun String.stripColors(): String = Strings.stripColors(this)

fun String.stripGlyphs(): String = Strings.stripGlyphs(this)

fun String.preventDiscordPings(): String = this.replace(discordPingRegex) { "@\u200B${it.groupValues[1]}" }