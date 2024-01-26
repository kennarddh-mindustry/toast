package com.github.kennarddh.mindustry.toast.core.commons.logging

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.helpers.NOPMDCAdapter
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider

class ArcServiceProvider : SLF4JServiceProvider {
    private lateinit var loggerFactory: ILoggerFactory
    private lateinit var markerFactory: IMarkerFactory
    private lateinit var mdcAdapter: MDCAdapter

    override fun getLoggerFactory(): ILoggerFactory = loggerFactory

    override fun getMarkerFactory(): IMarkerFactory = markerFactory

    override fun getMDCAdapter(): MDCAdapter = mdcAdapter

    override fun getRequestedApiVersion(): String {
        return "2.0.99"
    }

    override fun initialize() {
        loggerFactory = ArcLoggerFactory()
        markerFactory = BasicMarkerFactory()
        mdcAdapter = NOPMDCAdapter()
    }
}