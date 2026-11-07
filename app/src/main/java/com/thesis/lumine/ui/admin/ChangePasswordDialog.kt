package com.thesis.lumine.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.thesis.lumine.ui.auth.AuthErrorBanner
import com.thesis.lumine.viewmodel.PasswordChangeState

private const val MIN_PASSWORD_LENGTH = 8

/** What's wrong with the entered passwords, or null when they can be submitted. */
internal fun passwordChangeProblem(current: String, new: String, confirm: String): String? = when {
    new.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters."
    new != confirm -> "Passwords don't match."
    new == current -> "Please choose a different password."
    else -> null
}

/** Lets a signed-in admin change their password. The email is shown but can't be edited. */
@Composable
fun ChangePasswordDialog(
    email: String,
    state: PasswordChangeState,
    onSubmit: (currentPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit
) {
    var current         by remember { mutableStateOf("") }
    var new             by remember { mutableStateOf("") }
    var confirm         by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localError      by remember { mutableStateOf<String?>(null) }

    val isSaving = state is PasswordChangeState.Saving
    val message = localError ?: (state as? PasswordChangeState.Error)?.message

    fun submit() {
        localError = passwordChangeProblem(current, new, confirm)
        if (localError == null) onSubmit(current, new)
    }

    @Composable
    fun PasswordField(value: String, label: String, imeAction: ImeAction, onValueChange: (String) -> Unit) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it); localError = null },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = { Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Change Password", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PasswordField(current, "Current Password", ImeAction.Next) { current = it }
                PasswordField(new, "New Password", ImeAction.Next) { new = it }
                PasswordField(confirm, "Confirm New Password", ImeAction.Done) { confirm = it }
                message?.let { AuthErrorBanner(it) }
            }
        },
        confirmButton = {
            Button(
                onClick = ::submit,
                enabled = !isSaving && current.isNotBlank() && new.isNotBlank() && confirm.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
