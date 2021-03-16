package com.gabriel.devlogServer.view

import javafx.beans.property.StringProperty
import tornadofx.View
import tornadofx.label

class ConnectionStatusView(private val connectionStatus: StringProperty) : View() {
    override val root = label(connectionStatus) {
        maxHeight = (text.lines().count() * 2).toDouble()
    }

}