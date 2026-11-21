package com.urchi.jarvis.v4.advanced

object BuildCapability {
    private val knownOutputs = setOf("apk","aab","jar","zip","exe","msi","deb","rpm","appimage","wasm")
    fun validate(request: BuildRequest): Result<Unit> =
        if (request.outputType.lowercase() in knownOutputs && request.source.isNotBlank())
            Result.success(Unit)
        else Result.failure(IllegalArgumentException("Неизвестный формат сборки или пустой исходный проект."))

    fun needsRemoteToolchain(request: BuildRequest): Boolean =
        request.outputType.lowercase() in setOf("exe","msi","deb","rpm","appimage","wasm")
}
