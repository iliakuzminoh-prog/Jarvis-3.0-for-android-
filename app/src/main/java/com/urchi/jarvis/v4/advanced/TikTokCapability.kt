package com.urchi.jarvis.v4.advanced

import android.net.Uri

object TikTokCapability {
    fun authorizationUrl(config: TikTokApiConfig): String {
        require(config.clientKey.isNotBlank()) { "TikTok client key is not configured" }
        require(config.redirectUri.isNotBlank()) { "TikTok redirect URI is not configured" }
        val scopes = config.scopes.joinToString(",")
        return Uri.parse("https://www.tiktok.com/v2/auth/authorize/")
            .buildUpon()
            .appendQueryParameter("client_key", config.clientKey)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes)
            .appendQueryParameter("redirect_uri", config.redirectUri)
            .appendQueryParameter("state", "jarvis")
            .build().toString()
    }
}
