package io.github.jan.supabase.gotrue

actual fun GoTrue.generateRedirectUrl(fallbackUrl: String?): String? {
    if(fallbackUrl != null) return fallbackUrl
    this as GoTrueImpl
    return "${config.scheme}://${config.host}"
}