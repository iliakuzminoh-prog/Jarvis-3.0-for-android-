package com.urchi.jarvis.v4.advanced

import java.io.File

data class FileInfo(
    val path: String,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val exists: Boolean,
    val isDirectory: Boolean
)

object FileCapability {
    fun inspect(file: File): FileInfo {
        val name = file.name
        val ext = name.substringAfterLast('.', "").lowercase()
        return FileInfo(file.absolutePath,name,ext,file.length(),file.exists(),file.isDirectory)
    }

    fun classify(extension: String): String = when (extension.lowercase()) {
        "apk" -> "Android package"
        "aab" -> "Android App Bundle"
        "jar" -> "Java archive"
        "zip" -> "ZIP archive"
        "exe" -> "Windows executable"
        "msi" -> "Windows installer"
        "kt" -> "Kotlin source"
        "java" -> "Java source"
        "py" -> "Python source"
        "js", "ts" -> "JavaScript/TypeScript source"
        "cpp", "c", "h", "hpp" -> "C/C++ source"
        "rs" -> "Rust source"
        "go" -> "Go source"
        "cs" -> "C# source"
        "json" -> "JSON data"
        "xml" -> "XML document"
        "pdf" -> "PDF document"
        "mp3", "wav", "m4a", "ogg" -> "Audio"
        "mp4", "mkv", "webm", "mov" -> "Video"
        "png", "jpg", "jpeg", "webp", "gif" -> "Image"
        else -> "Unknown or generic file"
    }
}
