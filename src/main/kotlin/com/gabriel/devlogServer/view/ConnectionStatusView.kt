package com.gabriel.devlogServer.view

import javafx.beans.property.StringProperty
import tornadofx.View
import tornadofx.hboxConstraints
import tornadofx.label

class ConnectionStatusView(private val connectionStatus: StringProperty) : View() {
    override val root = label(connectionStatus) {
        maxHeight = (text.lines().count() * 2).toDouble()
        hboxConstraints {
            marginTop = 8.0
            marginBottom = 8.0
            marginLeft = 8.0
            marginRight = 8.0
        }
    }

}