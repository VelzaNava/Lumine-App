package com.thesis.lumine.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.viewmodel.AuthState
import com.thesis.lumine.viewmodel.AuthViewModel

// OTP verification screen — dito i-itype yung 6-digit code na natanggap sa email
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    onVerified: (isAdmin: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var otpCode    by remember { mutableStateOf("") }
    val authState  by viewModel.authState.collectAsState()
    // kunin yung pending email para ipakita sa screen kung saan napunta yung code
    val email      = viewModel.getPendingEmail()

    // pag verified na, i-redirect depende kung admin o regular user
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val isAdmin = (authState as AuthState.Success).authResponse.isAdmin
            onVerified(isAdmin)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Email") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Check Your Email", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(12.dp))

            Text(
                "We sent a 6-digit verification code to:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // OTP input field — digits only, max 6 chars, centered at malaking font
            OutlinedTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) otpCode = it },
                label = { Text("6-Digit Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    letterSpacing = 8.sp
                ),
                isError = authState is AuthState.Error
            )

            Spacer(Modifier.height(8.dp))
            Text(
                "Enter the code exactly as received. It expires in 10 minutes.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // verify button — enabled lang pag kumpleto na ang 6 digits
            Button(
                onClick = { viewModel.verifyAndRegister(otpCode) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = otpCode.length == 6 && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Verify & Create Account", fontSize = 16.sp)
                }
            }

            // ipakita yung error message pag mali o expired na ang OTP
            if (authState is AuthState.Error) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // resend button — mag-babalik sa register para mag-request ng bagong code
            TextButton(onClick = onBack) {
                Text("Resend Code", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
