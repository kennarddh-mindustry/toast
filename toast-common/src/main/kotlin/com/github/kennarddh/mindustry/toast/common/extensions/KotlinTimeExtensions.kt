package com.github.kennarddh.mindustry.toast.common.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Duration.toDisplayString(): String = inWholeSeconds.seconds.toString()