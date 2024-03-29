package com.gabriel.devlogServer.viewModel

import com.gabriel.devlogServer.db.LogMessage
import tornadofx.ItemViewModel

class LogViewModel : ItemViewModel<LogMessage>() {
    val userId = bind(LogMessage::userId)
    val processName = bind(LogMessage::processName)
    val logLevel = bind(LogMessage::logLevel)
    val tag = bind(LogMessage::tag)
    val timeInMillis = bind(LogMessage::timeInMillis)
    val message = bind(LogMessage::message)
}