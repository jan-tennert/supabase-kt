package io.github.jan.supabase.compose.auth.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.applicationContext
import io.github.jan.supabase.compose.auth.getActivity
import io.github.jan.supabase.compose.auth.getGoogleIDOptions
import io.github.jan.supabase.compose.auth.hash
import io.github.jan.supabase.compose.auth.signInWithGoogle
import io.github.jan.supabase.logging.d
import io.github.jan.supabase.logging.e

/**
 * Composable function that implements Native Google Auth.
 *
 * On unsupported platforms it will use the [fallback]
 *
 * @param onResult Callback for the result of the login
 * @param fallback Fallback function for unsupported platforms
 * @return [NativeSignInState]
 */
@Composable
actual fun ComposeAuth.rememberSignInWithGoogle(onResult: (NativeSignInResult) -> Unit, fallback: suspend () -> Unit): NativeSignInState {
    return signInWithCM(onResult, fallback)
}

@Composable
internal fun ComposeAuth.signInWithCM(onResult: (NativeSignInResult) -> Unit, fallback: suspend () -> Unit): NativeSignInState{
    val state = remember { NativeSignInState(serializer) }
    val context = LocalContext.current

    LaunchedEffect(key1 = state.status) {
        if (state.status is NativeSignInStatus.Started) {
            val activity = context.getActivity()
            val status = state.status as NativeSignInStatus.Started
            try {
                if (activity != null && config.googleLoginConfig != null) {
                    val hashedNonce = status.nonce?.hash()
                    ComposeAuth.logger.d { "Starting Google Sign In Flow${if(hashedNonce != null) " with hashed nonce: $hashedNonce" else ""}" }
                    val response = makeRequest(context, activity, config, hashedNonce)
                    if(response == null) {
                        onResult.invoke(NativeSignInResult.ClosedByUser)
                        ComposeAuth.logger.d { "Google Sign In Flow was closed by user" }
                        return@LaunchedEffect
                    }
                    parseCredential(
                        response.credential,
                        onResult
                    ) { signInWithGoogle(it, status.nonce, status.extraData) }
                } else {
                    fallback.invoke()
                }
            } catch (e: GetCredentialException) {
                when (e) {
                    is GetCredentialCancellationException -> onResult.invoke(NativeSignInResult.ClosedByUser)
                    else -> {
                        onResult.invoke(
                            NativeSignInResult.Error(
                                e.localizedMessage ?: "Credential exception",
                                e
                            )
                        )
                        ComposeAuth.logger.e(e) { "Credential exception" }
                    }
                }
            } catch (e: Exception) {
                onResult.invoke(NativeSignInResult.Error(e.localizedMessage ?: "error", e))
                ComposeAuth.logger.e(e) { "Error while logging into Supabase with Google ID Token Credential" }
            } finally {
                state.reset()
            }
        }
    }

    return state
}

private suspend fun parseCredential(
    credential: Credential,
    onResult: (NativeSignInResult) -> Unit,
    signInWithGoogle: suspend (idToken: String) -> Unit
) {
    when (credential) {
        is CustomCredential -> {
            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                ComposeAuth.logger.d { "Received Google ID Token Credential, logging into Supabase..." }
                try {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)
                    signInWithGoogle(googleIdTokenCredential.idToken)
                    ComposeAuth.logger.d { "Successfully logged into Supabase with Google ID Token Credential" }
                    onResult.invoke(NativeSignInResult.Success)
                } catch (e: GoogleIdTokenParsingException) {
                    ComposeAuth.logger.e(e) { "Google ID Token parsing exception" }
                    onResult.invoke(
                        NativeSignInResult.Error(
                            e.localizedMessage ?: "Google id parsing exception",
                            e
                        )
                    )
                } catch (e: Exception) {
                    ComposeAuth.logger.e(e) { "Error while logging into Supabase with Google ID Token Credential" }
                    onResult.invoke(
                        NativeSignInResult.Error(
                            e.localizedMessage ?: "error",
                            e
                        )
                    )
                }
            } else {
                onResult.invoke(NativeSignInResult.Error("Unexpected type of credential"))
            }
        }

        else -> {
            onResult.invoke(NativeSignInResult.Error("Unsupported credentials"))
        }
    }
}

internal actual suspend fun handleGoogleSignOut() {
    CredentialManager.create(applicationContext()).clearCredentialState(ClearCredentialStateRequest())
}

private suspend fun tryRequest(
    context: android.content.Context,
    activity: android.app.Activity,
    config: ComposeAuth.Config,
    nonce: String?,
    withFilter: Boolean
): GetCredentialResponse {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(getGoogleIDOptions(config.googleLoginConfig, withFilter, nonce))
        .build()
    return CredentialManager.create(context).getCredential(activity, request)
}

private suspend fun makeRequest(
    context: android.content.Context,
    activity: android.app.Activity,
    config: ComposeAuth.Config,
    nonce: String?
): GetCredentialResponse? {
    return try {
        ComposeAuth.logger.d { "Trying to get Google ID Token Credential with only authorized accounts" }
        tryRequest(context, activity, config, nonce, true)
    } catch(e: GetCredentialCancellationException) {
        return null
    } catch(e: GetCredentialException) {
        ComposeAuth.logger.d { "Error while trying to get Google ID Token Credential. Retrying without only authorized accounts" }
        tryRequest(context, activity, config, nonce, false)
    }
}