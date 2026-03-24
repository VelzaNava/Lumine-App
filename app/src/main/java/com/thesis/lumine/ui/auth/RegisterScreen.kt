package com.thesis.lumine.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thesis.lumine.viewmodel.AuthState
import com.thesis.lumine.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

// register screen — para mag-create ng bagong account, may OTP step bago matuloy
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onOtpSent: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }
    var localError      by remember { mutableStateOf<String?>(null) }
    var termsAccepted       by remember { mutableStateOf(false) }
    var termsScrolledThrough by remember { mutableStateOf(false) }
    var showTerms       by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    // pag naging OtpSent na yung state, pumunta sa OTP screen
    LaunchedEffect(authState) {
        if (authState is AuthState.OtpSent) onOtpSent()
    }

    // ipakita yung T&C dialog kung nag-click ang user
    if (showTerms) {
        TermsAndConditionsDialog(
            onAccept  = { termsScrolledThrough = true; termsAccepted = true; showTerms = false },
            onDismiss = { showTerms = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            Text("LUMINE", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Create your account to start trying on jewelry",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(36.dp))

            // email field — nag-cle-clear ng localError pag nag-type
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; localError = null },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = localError != null || authState is AuthState.Error
            )

            Spacer(Modifier.height(12.dp))

            // password field — minimum 8 characters, may show/hide toggle
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; localError = null },
                label = { Text("Password (min. 8 characters)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                isError = localError != null || authState is AuthState.Error
            )

            Spacer(Modifier.height(12.dp))

            // confirm password — i-check kung match yung dalawa bago mag-submit
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; localError = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { confirmVisible = !confirmVisible }) {
                        Text(if (confirmVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelSmall)
                    }
                },
                isError = localError != null
            )

            Spacer(Modifier.height(16.dp))

            // Terms and Conditions checkbox — hindi ma-check hanggat hindi pa nabasa lahat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = { if (termsScrolledThrough) termsAccepted = it },
                    enabled = termsScrolledThrough
                )
                Spacer(Modifier.width(4.dp))
                Text("I agree to the ", style = MaterialTheme.typography.bodySmall)
                TextButton(
                    onClick = { showTerms = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Terms and Conditions",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!termsScrolledThrough) {
                Text(
                    "Please read the Terms and Conditions to continue.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, bottom = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            // submit button — i-validate muna bago mag-send ng OTP
            Button(
                onClick = {
                    localError = when {
                        password != confirmPassword -> "Passwords do not match."
                        !termsAccepted -> "You must agree to the Terms and Conditions."
                        else -> null
                    }
                    if (localError == null) viewModel.initRegisterOtp(email, password)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = email.isNotBlank() && password.isNotBlank() &&
                          confirmPassword.isNotBlank() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Send Verification Code", fontSize = 16.sp)
                }
            }

            // ipakita yung local error o error galing sa ViewModel
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onNavigateToLogin) {
                    Text("Log In", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// Terms dialog — kailangan basahin lahat bago ma-enable ang Accept button
@Composable
fun TermsAndConditionsDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }

    // i-detect kung nakarating na sa bottom yung scroll — auto-enable pag maikli ang content
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.maxValue to scrollState.value }
            .distinctUntilChanged()
            .collect { (max, value) ->
                // max == Int.MAX_VALUE means layout not done yet
                if (max != Int.MAX_VALUE) {
                    if (max == 0 || value >= max - 30) {
                        hasScrolledToBottom = true
                    }
                }
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    "Terms and Conditions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    TermsText(
                        title = "LUMINE AUGMENTED REALITY APPLICATION",
                        body = "Terms and Conditions of Use\nEffective Date: March 2026\n\nBy creating an account and using the Lumine AR application, you agree to be bound by the following Terms and Conditions. Please read them carefully before proceeding."
                    )
                    TermsSection("1. ACCEPTANCE OF TERMS",
                        "By registering for and using the Lumine AR application (\"the App\"), you acknowledge that you have read, understood, and agree to comply with and be bound by these Terms and Conditions. If you do not agree to these terms, you must not register or use the App.")
                    TermsSection("2. USE OF THE APPLICATION",
                        "The App is designed to provide an augmented reality jewelry try-on experience for personal and educational use. You agree to use the App solely for its intended purpose and in full compliance with applicable laws, regulations, and these Terms.")
                    TermsSection("3. USER ACCOUNTS",
                        "You must provide accurate, complete, and current information during registration. You are solely responsible for maintaining the confidentiality of your account credentials. You must immediately notify us of any unauthorized use of your account. We reserve the right to suspend or terminate accounts that violate these Terms.")
                    TermsSection("4. CAMERA AND BIOMETRIC DATA",
                        "The App uses your device's camera to capture real-time imagery of your hands and face for the purpose of AR overlay and jewelry visualization. This data is processed locally on your device and is not stored, transmitted, or shared with any third parties. By using the App, you expressly consent to the use of your device camera for this purpose.")
                    TermsSection("5. VIRTUAL TRY-ON DISCLAIMER",
                        "The AR jewelry visualization provided by this App is an approximation for reference purposes only. Colors, sizes, proportions, and overall appearance may differ from actual physical jewelry items. Lumine makes no warranty or representation that virtual try-on results will be identical to physical products.")
                    TermsSection("6. INTELLECTUAL PROPERTY",
                        "All content, features, and functionality of the App — including but not limited to software, text, images, 3D models, graphics, logos, and jewelry designs — are the exclusive intellectual property of Lumine and its licensors, and are protected by applicable intellectual property laws. Unauthorized reproduction, distribution, or modification is strictly prohibited.")
                    TermsSection("7. PROHIBITED USES",
                        "You agree not to: (a) use the App for any unlawful or fraudulent purpose; (b) attempt to gain unauthorized access to any system or data; (c) reproduce, copy, sell, or exploit any part of the App without written permission; (d) transmit harmful, offensive, or disruptive content; (e) reverse-engineer, decompile, or disassemble the App or its components.")
                    TermsSection("8. PRIVACY POLICY",
                        "Your privacy is important to us. Personal information collected during registration — including your email address, name, and mobile number — is used solely for account management and service delivery purposes. We do not sell, rent, or share your personal information with third parties without your explicit consent, except as required by applicable law.")
                    TermsSection("9. LIMITATION OF LIABILITY",
                        "The App is provided on an \"as is\" and \"as available\" basis without warranties of any kind, either express or implied. Lumine shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising from or related to your use of or inability to use the App.")
                    TermsSection("10. MODIFICATIONS TO TERMS",
                        "Lumine reserves the right to modify these Terms and Conditions at any time. We will notify users of material changes through the App. Your continued use of the App following notification of changes constitutes your acceptance of the revised Terms.")
                    TermsSection("11. GOVERNING LAW",
                        "These Terms and Conditions shall be governed by and construed in accordance with applicable laws. Any disputes arising from these Terms or your use of the App shall be resolved through appropriate legal channels.")
                    TermsSection("12. CONTACT INFORMATION",
                        "If you have any questions, concerns, or feedback regarding these Terms and Conditions or the App, please contact us through the in-app support feature or reach out to the development team.")
                }
                HorizontalDivider()
                // ipakita yung scroll hint pag hindi pa nakakarating sa ibaba
                if (!hasScrolledToBottom) {
                    Text(
                        "Scroll to the bottom to continue",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                    )
                }
                // Accept button — disabled hanggang hindi pa nare-reach yung bottom
                Button(
                    onClick = onAccept,
                    enabled = hasScrolledToBottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { Text("I Understand") }
            }
        }
    }
}

// helper composable para sa terms header section
@Composable
private fun TermsText(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
}

// helper composable para sa bawat numbered section ng T&C
@Composable
private fun TermsSection(title: String, body: String) {
    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
}
