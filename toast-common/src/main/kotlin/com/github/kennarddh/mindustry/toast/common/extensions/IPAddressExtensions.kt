package com.github.kennarddh.mindustry.toast.common.extensions

class InvalidIPStringException(override val message: String) : RuntimeException(message)

fun String.packIP(): Int {
    try {
        val intArray = this.split(".").map { it.toInt() }

        return intArray[0] shl 24 or (intArray[1] shl 16) or (intArray[2] shl 8) or intArray[3]
    } catch (error: NumberFormatException) {
        throw InvalidIPStringException("IP String is not a valid ip")
    }
}

fun Int.unpackIP(): String {
    return intArrayOf(
        this shr 24 and 0xff,
        this shr 16 and 0xff,
        this shr 8 and 0xff,
        this and 0xff
    ).joinToString(".")
}