package com.example.ui.screens.auth

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.ENABLE_DEMO_LOGIN
import com.example.data.repository.ENABLE_GOOGLE_SIGNIN
import com.example.ui.components.ErrorBanner
import com.example.ui.theme.TourGoldDark
import com.example.ui.theme.TourNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    authRepo: AuthRepository,
    onAuthSuccess: () -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authError by authRepo.authError.collectAsState()
    val isPhoneMissing by authRepo.isPhoneMissing.collectAsState()
    val currentUser by authRepo.currentUser.collectAsState()

    // If user just logged in with Google and is missing phone, show "Complete your profile"
    var showPhonePrompt by remember { mutableStateOf(false) }
    var completePhoneInput by remember { mutableStateOf("") }

    // Demo Login state
    var showDemoLoginDialog by remember { mutableStateOf(false) }
    var demoEmailInput by remember { mutableStateOf("traveler@tourmanage.demo") }
    var demoNameInput by remember { mutableStateOf("Demo Traveler") }

    LaunchedEffect(currentUser, isPhoneMissing) {
        if (currentUser != null && isPhoneMissing) {
            showPhonePrompt = true
        } else if (currentUser != null && !isPhoneMissing) {
            onAuthSuccess()
        }
    }

    // 0: Log In, 1: Sign Up, 2: Forgot Password
    var isSignUpMode by remember { mutableStateOf(false) }
    var isForgotPasswordMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusNotice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onContinueAsGuest) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo & Tagline
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Tour Manage",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tour Manage",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "India's marketplace for verified local tour agencies",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner with exact message
            ErrorBanner(
                errorMessage = authError,
                onDismiss = { authRepo.clearError() }
            )

            if (statusNotice != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Text(text = statusNotice!!, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (ENABLE_GOOGLE_SIGNIN) {
                // PRIMARY "CONTINUE WITH GOOGLE" BUTTON
                Button(
                    onClick = {
                        statusNotice = null
                        scope.launch {
                            isLoading = true
                            val res = authRepo.signInWithGoogle(context)
                            isLoading = false
                            if (res.isSuccess) {
                                val user = res.getOrNull()
                                val emailText = user?.email ?: "User"
                                Toast.makeText(context, "Logged in as $emailText", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_signin_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // "OR" Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  or sign in with email  ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Sign Up specific fields
            if (isSignUpMode && !isForgotPasswordMode) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("auth_name_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        val clean = it.filter { ch -> ch.isDigit() }
                        if (clean.length <= 10) phone = clean
                    },
                    label = { Text("Mobile Number (+91)") },
                    prefix = { Text("+91 ") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("auth_phone_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    supportingText = { Text("Format check only (exactly 10 digits, no OTP required)") }
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Gmail or any email)") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().testTag("auth_email_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Field
            if (!isForgotPasswordMode) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Forgot Password Link
            if (!isSignUpMode && !isForgotPasswordMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isForgotPasswordMode = true }) {
                        Text("Forgot password?", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    statusNotice = null
                    if (email.isBlank()) {
                        Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isForgotPasswordMode) {
                        isLoading = true
                        scope.launch {
                            val res = authRepo.sendPasswordReset(email)
                            isLoading = false
                            if (res.isSuccess) {
                                statusNotice = "Password reset email sent to $email. Please check your inbox."
                            }
                        }
                    } else if (isSignUpMode) {
                        if (fullName.isBlank()) {
                            Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (phone.length != 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit phone number (+91)", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        scope.launch {
                            val res = authRepo.signUp(fullName, email, phone, password)
                            isLoading = false
                            if (res.isSuccess) {
                                val user = res.getOrNull()
                                val emailText = user?.email ?: email.trim()
                                Toast.makeText(context, "Logged in as $emailText", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        }
                    } else {
                        // Log In
                        if (password.isBlank()) {
                            Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val res = authRepo.logIn(email, password)
                            isLoading = false
                            if (res.isSuccess) {
                                val user = res.getOrNull()
                                val emailText = user?.email ?: email.trim()
                                Toast.makeText(context, "Logged in as $emailText", Toast.LENGTH_SHORT).show()
                                onAuthSuccess()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        text = when {
                            isForgotPasswordMode -> "Send Reset Instructions"
                            isSignUpMode -> "Sign Up & Continue"
                            else -> "Log In"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch between Login / Sign Up / Forgot Password
            if (isForgotPasswordMode) {
                TextButton(onClick = { isForgotPasswordMode = false }) {
                    Text("Back to Log In", fontSize = 13.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isSignUpMode) "Already have an account?" else "Don't have an account?",
                        fontSize = 13.sp
                    )
                    TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                        Text(
                            text = if (isSignUpMode) "Log In" else "Sign Up",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CONTINUE AS GUEST BUTTON
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.testTag("continue_as_guest_button")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Continue as guest (browsing only)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            if (ENABLE_DEMO_LOGIN) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showDemoLoginDialog = true },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .testTag("demo_login_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TourGoldDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Demo login (instant test session)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Small Terms Text
            Text(
                text = "By signing in, you agree to Tour Manage Terms of Service and Privacy Policy. All payments and bookings are processed under verified operator escrow.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // "COMPLETE YOUR PROFILE" DIALOG FOR GOOGLE USERS MISSING PHONE
    if (showPhonePrompt) {
        AlertDialog(
            onDismissRequest = { /* Require phone or guest */ },
            title = { Text("Complete Your Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "To enable tour booking, support chat, and voucher notifications, please provide your 10-digit mobile number.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = completePhoneInput,
                        onValueChange = {
                            val clean = it.filter { ch -> ch.isDigit() }
                            if (clean.length <= 10) completePhoneInput = clean
                        },
                        label = { Text("Mobile Number (+91)") },
                        prefix = { Text("+91 ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("complete_phone_field"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (completePhoneInput.length == 10) {
                            authRepo.completeProfilePhone(completePhoneInput)
                            showPhonePrompt = false
                            onAuthSuccess()
                        } else {
                            Toast.makeText(context, "Please enter exactly 10 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save & Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhonePrompt = false
                    onAuthSuccess()
                }) {
                    Text("Skip for Now")
                }
            }
        )
    }

    if (showDemoLoginDialog) {
        AlertDialog(
            onDismissRequest = { showDemoLoginDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = TourGoldDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Demo Mode Login", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Creates an on-device demo session with sample data and simulated booking/chat. No password required, and real customer records remain untouched.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = demoEmailInput,
                        onValueChange = { demoEmailInput = it },
                        label = { Text("Demo Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("demo_email_input"),
                        singleLine = true,
                        placeholder = { Text("traveler@demo.com") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = demoNameInput,
                        onValueChange = { demoNameInput = it },
                        label = { Text("Traveler Name (Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("demo_name_input"),
                        singleLine = true,
                        placeholder = { Text("e.g. Subhojit Mondal") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = authRepo.loginDemo(demoEmailInput, demoNameInput.ifBlank { null })
                        if (res.isSuccess) {
                            showDemoLoginDialog = false
                            Toast.makeText(context, "Logged in as ${demoEmailInput.trim()}", Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                        } else {
                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Invalid email", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("submit_demo_login_button")
                ) {
                    Text("Start Demo Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
