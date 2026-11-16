package com.urchi.jarvis.v4.core

enum class CommandKind {
    CONVERSATION, OPEN_APP, OPEN_URL, SEARCH_WEB, YOUTUBE,
    MEDIA_PLAY, MEDIA_PAUSE, MEDIA_NEXT, MEDIA_PREVIOUS,
    ALARM, TIMER, NOTIFICATION,
    MEMORY_REMEMBER, MEMORY_FORGET, MEMORY_SEARCH,
    CAMERA, IMAGE_ANALYSIS, IMAGE_GENERATION,
    SCHOOL, CALCULATE, SETTINGS, WIFI_SETTINGS, BLUETOOTH_SETTINGS,
    VPN, DEVICE_INFO, HELP, UNKNOWN
}

data class ParsedCommand(
    val kind: CommandKind,
    val original: String,
    val normalized: String,
    val argument: String = "",
    val confidence: Float = 0f,
    val requiresConfirmation: Boolean = false
)

data class AssistantReply(
    val text: String,
    val speech: String = text,
    val command: ParsedCommand? = null,
    val performedAction: Boolean = false
)
