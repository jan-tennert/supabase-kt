package io.github.jan.supabase.common.di

import io.github.jan.supabase.gotrue.AuthConfig

actual fun AuthConfig.platformGoTrueConfig() {
    htmlTitle = "Chat App"
}