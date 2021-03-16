package com.gabriel.devlogServer.app

import com.gabriel.devlogServer.view.MainView
import tornadofx.App

class MyApp : App(MainView::class, Styles::class)

enum class LogLevel(val level: Int) {
    ALL(1),
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARNING(5),
    ERROR(6);
}

private data class LogMessage(
        val logLevel: Int,
        val tag: String,
        val timeMills: Long,
        val message: String
)