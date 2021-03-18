package com.gabriel.devlogServer.app.res

import impl.jfxtras.styles.jmetro.FluentButtonSkin
import tornadofx.FX
import tornadofx.Stylesheet

class Styles : Stylesheet() {

    init {
        button {
            skin = FluentButtonSkin::class
        }
    }
}

