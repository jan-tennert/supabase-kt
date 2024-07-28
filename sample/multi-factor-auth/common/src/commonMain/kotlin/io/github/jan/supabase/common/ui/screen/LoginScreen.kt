package io.github.jan.supabase.common.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.common.AppViewModel
import io.github.jan.supabase.common.ui.components.AlertDialog
import io.github.jan.supabase.common.ui.components.GoogleButton
import io.github.jan.supabase.common.ui.components.PasswordField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    var signUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var password by remember { mutableStateOf("") }
        val passwordFocus = remember { FocusRequester() }
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            singleLine = true,
            label = { Text("E-Mail") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
            leadingIcon = { Icon(Icons.Filled.Mail, "Mail") },
        )
        PasswordField(
            password = password,
            onPasswordChanged = { password = it },
            modifier = Modifier.focusRequester(passwordFocus)
                .padding(top = 10.dp),
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(onDone = {
                authenticate(signUp, viewModel, email, password)
            }),
        )
        Button(
            onClick = { authenticate(signUp, viewModel, email, password) },
            modifier = Modifier.padding(top = 10.dp),
            enabled = email.isNotBlank() && password.isNotBlank()
        ) {
            Text(if (signUp) "Register" else "Login")
        }
        GoogleButton(
            text = if (signUp) "Sign Up with Google" else "Login with Google"
        ) { viewModel.loginWithGoogle() }

    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        TextButton(onClick = { signUp = !signUp }) {
            Text(if (signUp) "Already have an account? Login" else "Not registered? Register")
        }
    }
}

fun authenticate(signUp: Boolean, viewModel: AppViewModel, email: String, password: String) {
    if (signUp) {
        viewModel.signUp(email, password)
    } else {
        viewModel.login(email, password)
    }
}