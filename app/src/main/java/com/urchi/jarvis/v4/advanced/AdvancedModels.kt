package com.urchi.jarvis.v4.advanced

enum class TaskKind { CHAT, WEB, APP, MEDIA, IMAGE, VIDEO, AUDIO, FILE, CODE, BUILD, MEMORY, DEVICE, TIKTOK, UNKNOWN }

data class TaskIntent(
    val kind: TaskKind,
    val text: String,
    val argument: String = "",
    val confidence: Float = 0f,
    val explicitExternalNavigation: Boolean = false
)

data class ToolSpec(
    val id: String,
    val description: String,
    val dangerous: Boolean = false,
    val requiresNetwork: Boolean = false
)

data class AgentPlan(
    val intent: TaskIntent,
    val tools: List<ToolSpec>,
    val requiresConfirmation: Boolean,
    val reason: String
)

data class BuildRequest(
    val projectType: String,
    val language: String,
    val outputType: String,
    val source: String,
    val target: String = "default"
)

data class MediaRequest(
    val kind: String,
    val prompt: String,
    val sourceUri: String? = null,
    val model: String? = null
)

data class TikTokApiConfig(
    val clientKey: String = "",
    val redirectUri: String = "",
    val scopes: Set<String> = emptySet()
)
