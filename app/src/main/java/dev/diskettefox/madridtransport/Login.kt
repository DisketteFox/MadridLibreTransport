package dev.diskettefox.madridtransport

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import dev.diskettefox.madridtransport.api.ApiCall
import dev.diskettefox.madridtransport.api.ApiInterface
import dev.diskettefox.madridtransport.models.TokenModel
import dev.diskettefox.madridtransport.ui.theme.MadridTransportTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class Login : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MadridTransportTheme {
                LoginScreen(
                    onBackClick = { finish() },
                    onLoginSuccess = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBackClick: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<Int?>(null) }
    var passwordError by remember { mutableStateOf<Int?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun validateAndLogin() {
        var valid = true
        if (username.isEmpty()) {
            usernameError = R.string.message_empty_username
            valid = false
        } else if (!username.contains("@") || !username.contains(".")) {
            usernameError = R.string.message_invalid_username
            valid = false
        } else {
            usernameError = null
        }

        if (password.isEmpty()) {
            passwordError = R.string.message_empty_password
            valid = false
        } else if (password.length < 8) {
            passwordError = R.string.message_short_password
            valid = false
        } else {
            passwordError = null
        }

        if (valid) {
            getToken(username, password, context, onLoginSuccess)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Surface(
                        onClick = onBackClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_left),
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_login),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_emt),
                contentDescription = stringResource(R.string.description_emt_logo),
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(24.dp)
            )

            Text(
                text = stringResource(R.string.mpass),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    if (usernameError != null) usernameError = null
                },
                label = { Text(stringResource(R.string.user)) },
                modifier = Modifier.fillMaxWidth(),
                isError = usernameError != null,
                supportingText = { usernameError?.let { Text(stringResource(it)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(stringResource(it)) } },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            painter = painterResource(id = R.drawable.eye),
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { validateAndLogin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = stringResource(R.string.login),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.signup),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, "https://datos.emtmadrid.es/user/register".toUri())
                        context.startActivity(browserIntent)
                    }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MadridTransportTheme {
        LoginScreen(
            onBackClick = {},
            onLoginSuccess = {}
        )
    }
}

private fun getToken(email: String, pass: String, context: android.content.Context, onSuccess: () -> Unit) {
    val apiInterface = ApiCall.callApi().create(ApiInterface::class.java)

    val call = apiInterface.getTokenByUser(email, pass)
    call.enqueue(object : Callback<TokenModel> {
        override fun onResponse(call: Call<TokenModel>, response: Response<TokenModel>) {
            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!
                when (token.code) {
                    1 -> {
                        val data = token.data[0]
                        Log.d("Token", data.accessToken)
                        ApiCall.setToken(data.accessToken)
                        Toast.makeText(context, R.string.message_successful_login, Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                    89 -> {
                        Toast.makeText(context, R.string.message_invalid_credentials, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.d("API Error", "Unexpected error")
                    }
                }
            } else {
                Log.e("API Error", "Unable to connect to database")
                Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFailure(call: Call<TokenModel>, t: Throwable) {
            Log.e("Call Error", "Error retrieving lines", t)
        }
    })
}
