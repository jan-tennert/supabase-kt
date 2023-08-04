package io.github.jan.supabase.compose.auth.composable

import androidx.compose.runtime.Composable
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.gotrue.LogoutScope

/**
 * Composable for Google login with default behavior
 */
@Composable
actual fun ComposeAuth.rememberLoginWithGoogle(onResult: (NativeSignInResult) -> Unit, fallback: suspend () -> Unit): NativeSignInState = defaultLoginBehavior(fallback)

/**
 * Composable for SignOut with default behavior
 */
@Composable
actual fun ComposeAuth.rememberSignOut(logoutScope: LogoutScope): NativeSignInState = defaultSignOutBehavior(logoutScope)