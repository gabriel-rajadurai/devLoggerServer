package com.example.demo.viewModel

import com.example.demo.db.LogMessage
import tornadofx.ItemViewModel

class LogViewModel : ItemViewModel<LogMessage>() {
    val userId = bind(LogMessage::userId)
    val logLevel = bind(LogMessage::logLevel)
    val tag = bind(LogMessage::tag)
    val timeInMillis = bind(LogMessage::timeInMillis)
    val message = bind(LogMessage::message)
}