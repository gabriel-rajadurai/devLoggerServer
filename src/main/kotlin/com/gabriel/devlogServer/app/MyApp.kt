package com.gabriel.devlogServer.app

import com.gabriel.devlogServer.app.res.Styles
import com.gabriel.devlogServer.view.MainView
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.Style
import tornadofx.App

class MyApp : App(MainView::class, Styles::class) {
    val jMetro = JMetro(Style.LIGHT)
    var isDarkMode = SimpleBooleanProperty(false)

    override fun start(stage: Stage) {
        super.start(stage)
        jMetro.scene = stage.scene
    }

    fun toggleDarkMode() {
        jMetro.style = if (isDarkMode.value) {
            Style.LIGHT
        } else {
            Style.DARK
        }
        isDarkMode.value = !isDarkMode.value
    }
}

