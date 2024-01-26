package com.github.kennarddh.mindustry.toast.core.commons.logging

import arc.util.ColorCodes
import arc.util.Log
import mindustry.net.Administration
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.AbstractLogger
import org.slf4j.helpers.MessageFormatter

class ArcLogger(val loggerName: String?, val plugin: String?) : AbstractLogger() {
    init {
        name = loggerName
    }

    constructor(name: String?) : this(name, null)

    companion object {
        private val WRITE_LOCK = Any()
        private val TRACE = Administration.Config("trace", "Enable trace logging when debug is enabled.", false)
    }
    
    override fun isTraceEnabled(): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.debug) && TRACE.bool()
    }

    override fun isTraceEnabled(marker: Marker): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.debug) && TRACE.bool()
    }

    override fun isDebugEnabled(): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.debug)
    }

    override fun isDebugEnabled(marker: Marker): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.debug)
    }

    override fun isInfoEnabled(): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.info)
    }

    override fun isInfoEnabled(marker: Marker): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.info)
    }

    override fun isWarnEnabled(): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.warn)
    }

    override fun isWarnEnabled(marker: Marker): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.warn)
    }

    override fun isErrorEnabled(): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.err)
    }

    override fun isErrorEnabled(marker: Marker): Boolean {
        return isArcLogLevelAtLeast(Log.LogLevel.err)
    }

    override fun getFullyQualifiedCallerName(): String? {
        return null
    }

    override fun handleNormalizedLoggingCall(
        level: Level,
        marker: Marker?,
        messagePattern: String,
        arguments: Array<Any>?,
        throwable: Throwable?
    ) {
        val newArguments = arguments?.toMutableList()
        var newThrowable = throwable

        val builder = StringBuilder()

        if (loggerName != ROOT_LOGGER_NAME) {
            if (plugin != null)
                builder
                    .append(getColorCode(level))
                    .append('[')
                    .append(plugin)
                    .append(']')
                    .append(ColorCodes.reset)
                    .append(' ')

            builder
                .append(getColorCode(level))
                .append('[')
                .append(name)
                .append(']')
                .append(ColorCodes.reset)
                .append(' ')
        }

        if (level == Level.ERROR)
            builder.append(this.getColorCode(level))

        if (newThrowable == null &&
            !newArguments.isNullOrEmpty()
        ) {
            val lastArgument = newArguments.last()

            if (lastArgument is Throwable) {
                newThrowable = lastArgument

                newArguments.removeLastOrNull()
            }
        }

        builder.append(
            MessageFormatter.basicArrayFormat(
                messagePattern.replace("{}", "&fb&lb{}&fr"),
                newArguments?.toTypedArray()
            )
        )

        val string = builder.toString()

        synchronized(WRITE_LOCK) {
            if (newThrowable != null && newArguments.isNullOrEmpty()) {
                Log.err(string)
                Log.err(newThrowable)
            } else {
                Log.log(getArcLogLevel(level), string)

                if (newThrowable != null)
                    Log.err(newThrowable)
            }
        }
    }

    private fun isArcLogLevelAtLeast(level: Log.LogLevel): Boolean {
        return level != Log.LogLevel.none && Log.level.ordinal <= level.ordinal
    }

    private fun getArcLogLevel(level: Level): Log.LogLevel {
        return when (level) {
            Level.TRACE, Level.DEBUG -> Log.LogLevel.debug
            Level.INFO -> Log.LogLevel.info
            Level.WARN -> Log.LogLevel.warn
            Level.ERROR -> Log.LogLevel.err
        }
    }

    private fun getColorCode(level: Level) = when (level) {
        Level.DEBUG, Level.TRACE -> ColorCodes.lightCyan + ColorCodes.bold
        Level.INFO -> ColorCodes.lightBlue + ColorCodes.bold
        Level.WARN -> ColorCodes.lightYellow + ColorCodes.bold
        Level.ERROR -> ColorCodes.lightRed + ColorCodes.bold
    }
}
