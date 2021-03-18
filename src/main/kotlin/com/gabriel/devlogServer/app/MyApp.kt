package com.gabriel.devlogServer.app

import com.gabriel.devlogServer.view.MainView
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.Style
import tornadofx.App
import tornadofx.FX

class MyApp : App(MainView::class, Styles::class) {
    val jMetro = JMetro(Style.LIGHT)
    var isDarkMode = false
        private set

    override fun start(stage: Stage) {
        super.start(stage)
        jMetro.scene = stage.scene
    }

    fun toggleDarkMode() {
        jMetro.style = if (isDarkMode) {
            Style.LIGHT
        } else {
            Style.DARK
        }
        isDarkMode = !isDarkMode
    }
}

enum class LogLevel(val level: Int) {
    ALL(1),
    VERBOSE(2),
    DEBUG(3),
    INFO(4),
    WARNING(5),
    ERROR(6);
}

