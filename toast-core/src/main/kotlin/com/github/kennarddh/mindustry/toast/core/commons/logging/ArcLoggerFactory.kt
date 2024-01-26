/*
 * Distributor, a feature-rich framework for Mindustry plugins.
 *
 * Copyright (C) 2023 Xpdustry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.kennarddh.mindustry.toast.core.commons.logging

import org.slf4j.ILoggerFactory
import org.slf4j.Logger


class ArcLoggerFactory : ILoggerFactory {
    private var loggers: MutableMap<String, ArcLogger> = mutableMapOf()

    /**
     * Return an appropriate [ArcLogger] instance by name.
     *
     * This method will call [.createLogger] if the logger
     * has not been created yet.
     */
    override fun getLogger(name: String): Logger = loggers.computeIfAbsent(name, ::createLogger)

    /**
     * Actually creates the logger for the given name.
     */
    private fun createLogger(name: String?): ArcLogger {
        return ArcLogger(name)
    }
}