package com.thesis.lumine.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onOtpSent: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var confirmPassword  by remember { mutableStateOf("") }
    var passwordVisible  by remember { mutableStateOf(false) }
    var confirmVisible   by remember { mutableStateOf(false) }
    var localError       by remember { mutableStateOf<String?>(null) }
    var termsAccepted        by remember { mutableStateOf(false) }
    var termsScrolledThrough by remember { mutableStateOf(false) }
    var showTerms        by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.OtpSent) onOtpSent()
    }

    if (showTerms) {
        TermsAndConditionsDialog(
            onAccept  = { termsScrolledThrough = true; termsAccepted = true; showTerms = false },
            onDismiss = { showTerms = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // ── branding header ──────────────────────────────────────
            Text(
                text = "LUMINE",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Create Your Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            // ── form card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    // email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = localError != null || authState is AuthState.Error,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null },
                        label = { Text("Password") },
                        placeholder = { Text("Min. 8 characters") },
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility
                                                  else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide" else "Show"
                                )
                            }
                        },
                        isError = localError != null || authState is AuthState.Error,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // confirm password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; localError = null },
                        label = { Text("Confirm Password") },
                        leadingIcon = {
                            Icon(Icons.Filled.LockOpen, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (confirmVisible) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    imageVector = if (confirmVisible) Icons.Filled.Visibility
                                                  else Icons.Filled.VisibilityOff,
                                    contentDescription = if (confirmVisible) "Hide" else "Show"
                                )
                            }
                        },
                        isError = localError != null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── terms and conditions section ─────────────────────────
            TermsSection(
                termsRead     = termsScrolledThrough,
                termsAccepted = termsAccepted,
                onReadClick   = { showTerms = true },
                onCheckedChange = { if (termsScrolledThrough) termsAccepted = it }
            )

            Spacer(Modifier.height(12.dp))

            // ── submit button ────────────────────────────────────────
            Button(
                onClick = {
                    localError = when {
                        email.isBlank()                -> "Email address is required."
                        password != confirmPassword    -> "Passwords do not match."
                        !termsAccepted                 -> "You must agree to the Terms and Conditions."
                        else                           -> null
                    }
                    if (localError == null) viewModel.initRegisterOtp(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = email.isNotBlank() && password.isNotBlank() &&
                          confirmPassword.isNotBlank() && authState !is AuthState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Filled.MarkEmailUnread, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send Verification Code", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // error message
            val errorMessage = localError ?: (authState as? AuthState.Error)?.message
            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── login redirect ───────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onNavigateToLogin) {
                    Text("Log In", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Terms section composable ──────────────────────────────────────────────────
@Composable
private fun TermsSection(
    termsRead: Boolean,
    termsAccepted: Boolean,
    onReadClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (termsRead) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // header label
        Text(
            "Terms & Conditions",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // prominent read button
        OutlinedButton(
            onClick = onReadClick,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.Description, contentDescription = null,
                modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (termsRead) "Review Terms & Conditions" else "Read Terms & Conditions",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            if (termsRead) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Read",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // agree checkbox — only enabled after reading
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = termsAccepted,
                onCheckedChange = onCheckedChange,
                enabled = termsRead,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    disabledUncheckedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "I have read and agree to the Terms and Conditions",
                style = MaterialTheme.typography.bodySmall,
                color = if (termsRead) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        if (!termsRead) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp))
                Text(
                    "Please read the full terms before agreeing.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Terms dialog ──────────────────────────────────────────────────────────────
@Composable
fun TermsAndConditionsDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()
    var hasScrolledToBottom by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.maxValue to scrollState.value }
            .distinctUntilChanged()
            .collect { (max, value) ->
                if (max != Int.MAX_VALUE) {
                    if (max == 0 || value >= max - 30) hasScrolledToBottom = true
                }
            }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // dialog header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp))
                    Text(
                        "Terms and Conditions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                HorizontalDivider()

                // scrollable terms content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                ) {
                    TermsText(
                        title = "LUMINE AUGMENTED REALITY APPLICATION",
                        body  = "Terms and Conditions of Use\nEffective Date: March 2026\n\nBy creating an account and using the Lumine AR application, you agree to be bound by the following Terms and Conditions. Please read them carefully before proceeding."
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
                        "Your privacy is important to us. Personal information collected during registration — including your email address — is used solely for account management and service delivery purposes. We do not sell, rent, or share your personal information with third parties without your explicit consent, except as required by applicable law.")
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

                // scroll hint + accept button
                if (!hasScrolledToBottom) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Scroll to the bottom to accept",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onAccept,
                    enabled = hasScrolledToBottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("I Have Read and Accept", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun TermsText(title: String, body: String) {
    Text(title, fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
    Text(body, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp)
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun TermsSection(title: String, body: String) {
    Text(title, fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp)
    Spacer(Modifier.height(18.dp))
}
