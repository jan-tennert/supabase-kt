// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.russhwolf.settings.PreferencesSettings
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.SettingsSessionManager
import io.github.jan.supabase.gotrue.gotrue
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.prefs.Preferences

@Serializable
data class User(val id: String, val username: String)

suspend fun main() {
    Napier.base(DebugAntilog())
    val client = createSupabaseClient(
        supabaseUrl = System.getenv("SUPABASE_URL"),
        supabaseKey = System.getenv("SUPABASE_KEY")
    ) {
        install(GoTrue) {
            sessionManager = SettingsSessionManager(PreferencesSettings(Preferences.userRoot().node("custom_name")))
        }
        install(Realtime)
    }
    val scope = CoroutineScope(Dispatchers.IO)
    application {
        Window(::exitApplication) {
            val status by client.gotrue.sessionStatus.collectAsState()
            //val status by client.realtime.status.collectAsState()
            if (status is SessionStatus.Authenticated) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Logged in as ${(status as SessionStatus.Authenticated).session.user?.email}")
                }
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    var email by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    Column {
                        TextField(email, { email = it }, placeholder = { Text("Email") })
                        TextField(
                            password,
                            { password = it },
                            placeholder = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(onClick = {
                            scope.launch {
                                client.gotrue.loginWith(Email) {
                                    this.email = email
                                    this.password = password
                                }
                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Login")
                        }
                        Button(onClick = {
                            scope.launch {
                                client.gotrue.loginWith(Google)
                            }
                        }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Login with Discord")
                        }
                        //
                    }
                }

            }
        }
    }

}
